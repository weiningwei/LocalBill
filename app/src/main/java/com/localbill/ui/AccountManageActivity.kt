package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.model.Account
import com.localbill.util.C
import com.localbill.util.Money
import com.localbill.util.Theme
import com.localbill.util.UiKit

class AccountManageActivity : Activity() {
    private val ctx: Context get() = this@AccountManageActivity

    private var items: List<Account> = emptyList()
    private lateinit var listView: ListView

    private val types = arrayOf("现金", "银行卡", "电子钱包", "信用卡", "其他")

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (com.localbill.util.Prefs.darkMode) R.style.Theme_LocalBill_Dark else R.style.Theme_LocalBill)
        super.onCreate(savedInstanceState)
        buildUi()
        reload()
    }

    private fun buildUi() {
        val root = UiKit.vertical(ctx).apply { setBackgroundColor(Theme.pageBg(ctx)) }

        val topBar = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = ImageView(ctx)
        back.setImageResource(R.drawable.ic_back)
        back.setColorFilter(Theme.subText(ctx))
        back.setPadding(Theme.dp(ctx, 16), Theme.dp(ctx, 14), Theme.dp(ctx, 14), Theme.dp(ctx, 14))
        back.setOnClickListener { finish() }
        topBar.addView(back, LinearLayout.LayoutParams(Theme.dp(ctx, 48), Theme.dp(ctx, 48)))
        topBar.addView(UiKit.text(ctx, "账户管理", 17f, Theme.mainText(ctx), bold = true, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        topBar.addView(View(ctx), LinearLayout.LayoutParams(Theme.dp(ctx, 48), 0))
        root.addView(topBar, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))

        listView = ListView(ctx)
        listView.divider = null
        listView.setSelector(android.R.color.transparent)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> editDialog(items[pos]) }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            deleteConfirm(items[pos]); true
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        val addBtn = UiKit.text(ctx, "+ 添加账户", 16f, C.PRIMARY, bold = true, gravity = Gravity.CENTER)
        addBtn.background = UiKit.rounded(ctx, C.PRIMARY_BG, 24)
        addBtn.setPadding(0, Theme.dp(ctx, 13), 0, Theme.dp(ctx, 13))
        val addLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 50))
        addLp.setMargins(Theme.dp(ctx, 16), Theme.dp(ctx, 6), Theme.dp(ctx, 16), Theme.dp(ctx, 16))
        addBtn.setOnClickListener { addDialog() }
        root.addView(addBtn, addLp)

        setContentView(root)
    }

    private fun reload() {
        items = App.db.accounts(App.db.activeLedger().id)
        listView.adapter = AccountAdapter()
    }

    private fun addDialog() {
        val name = EditText(ctx).apply {
            setTextSize(14f)
            hint = "账户名称"
            setTextColor(Theme.mainText(ctx))
            setHintTextColor(Theme.lightText(ctx))
        }
        val balance = EditText(ctx).apply {
            setTextSize(14f)
            hint = "初始余额 (元)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Theme.mainText(ctx))
            setHintTextColor(Theme.lightText(ctx))
        }
        val box = UiKit.vertical(ctx)
        box.addView(name)
        box.addView(balance, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = Theme.dp(ctx, 8)
        })
        val dialog = android.app.AlertDialog.Builder(ctx)
            .setTitle("添加账户")
            .setView(box)
            .setPositiveButton("确定") { _, _ ->
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    Toast.makeText(ctx, "请输入名称", Toast.LENGTH_SHORT).show()
                } else {
                    val cents = Money.parseToCents(balance.text.toString()) ?: 0L
                    App.db.addAccount(App.db.activeLedger().id, n, 0, cents, C.CATEGORY_COLORS[items.size % C.CATEGORY_COLORS.size])
                    reload()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun editDialog(account: Account) {
        val name = EditText(ctx).apply {
            setTextSize(14f)
            setText(account.name)
            setTextColor(Theme.mainText(ctx))
        }
        val balance = EditText(ctx).apply {
            setTextSize(14f)
            setText(Money.format(account.balance).replace(",", ""))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Theme.mainText(ctx))
        }
        val typeDialog = arrayOfNulls<Int>(1)
        val picker = ColorPickerView(ctx, account.color)
        val box = UiKit.vertical(ctx)
        box.addView(name)
        box.addView(balance, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = Theme.dp(ctx, 8) })
        box.addView(UiKit.text(ctx, "颜色", 13f, Theme.subText(ctx)).apply {
            setPadding(0, Theme.dp(ctx, 8), 0, Theme.dp(ctx, 2))
        })
        box.addView(picker, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 40)))
        val dialog = android.app.AlertDialog.Builder(ctx)
            .setTitle("编辑账户")
            .setView(box)
            .setPositiveButton("确定") { _, _ ->
                val n = name.text.toString().trim()
                val cents = Money.parseToCents(balance.text.toString()) ?: account.balance
                if (n.isEmpty()) {
                    Toast.makeText(ctx, "请输入名称", Toast.LENGTH_SHORT).show()
                } else {
                    App.db.updateAccount(account.id, n, account.type, cents, picker.selected)
                    reload()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun deleteConfirm(account: Account) {
        android.app.AlertDialog.Builder(ctx)
            .setTitle("删除账户「${account.name}」？")
            .setMessage("该账户的历史账单将保留。")
            .setPositiveButton("删除") { _, _ ->
                App.db.deleteAccount(account.id)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class AccountAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(pos: Int): Any = items[pos]
        override fun getItemId(pos: Int): Long = items[pos].id

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val account = items[pos]
            val row = UiKit.horizontal(this@AccountManageActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Theme.surface(this@AccountManageActivity))
            }
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(this@AccountManageActivity, 60))
            lp.setMargins(Theme.dp(this@AccountManageActivity, 12), Theme.dp(this@AccountManageActivity, 2),
                Theme.dp(this@AccountManageActivity, 12), Theme.dp(this@AccountManageActivity, 2))

            val circle = UiKit.horizontal(this@AccountManageActivity).apply {
                gravity = Gravity.CENTER
                background = UiKit.rounded(this@AccountManageActivity, account.color, 20)
            }
            circle.layoutParams = LinearLayout.LayoutParams(Theme.dp(this@AccountManageActivity, 40), Theme.dp(this@AccountManageActivity, 40))
            val ch = UiKit.text(this@AccountManageActivity, account.name.ifEmpty { "?" }.substring(0, 1),
                16f, 0xFFFFFFFF.toInt(), bold = true, gravity = Gravity.CENTER)
            circle.addView(ch, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            val dotWrap = UiKit.horizontal(this@AccountManageActivity).apply {
                gravity = Gravity.CENTER
                setPadding(Theme.dp(this@AccountManageActivity, 6), 0, Theme.dp(this@AccountManageActivity, 8), 0)
            }
            dotWrap.addView(circle)
            row.addView(dotWrap)

            val center = UiKit.vertical(this@AccountManageActivity)
            center.gravity = Gravity.CENTER_VERTICAL
            center.addView(UiKit.text(this@AccountManageActivity, account.name, 15f, Theme.mainText(this@AccountManageActivity)))
            center.addView(UiKit.text(this@AccountManageActivity, types.getOrElse(account.type) { "其他" }, 12f, Theme.lightText(this@AccountManageActivity)))
            row.addView(center, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

            row.addView(UiKit.text(this@AccountManageActivity, Money.format(account.balance), 15f,
                Theme.mainText(this@AccountManageActivity), bold = true).apply {
                setPadding(Theme.dp(this@AccountManageActivity, 10), 0, Theme.dp(this@AccountManageActivity, 14), 0)
            })
            return row.apply { layoutParams = lp }
        }
    }
}

