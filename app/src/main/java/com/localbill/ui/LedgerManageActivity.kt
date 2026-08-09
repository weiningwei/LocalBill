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
import com.localbill.model.Ledger
import com.localbill.util.C
import com.localbill.util.Prefs
import com.localbill.util.Theme
import com.localbill.util.UiKit

class LedgerManageActivity : Activity() {
    private val ctx: Context get() = this@LedgerManageActivity

    private var items: List<Ledger> = emptyList()
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (Prefs.darkMode) R.style.Theme_LocalBill_Dark else R.style.Theme_LocalBill)
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
        topBar.addView(UiKit.text(ctx, "账本管理", 17f, Theme.mainText(ctx), bold = true, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        topBar.addView(View(ctx), LinearLayout.LayoutParams(Theme.dp(ctx, 48), 0))
        root.addView(topBar, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))

        listView = ListView(ctx)
        listView.divider = null
        listView.setSelector(android.R.color.transparent)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> switchTo(items[pos]) }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            deleteConfirm(items[pos]); true
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        val addBtn = UiKit.text(ctx, "+ 新建账本", 16f, C.PRIMARY, bold = true, gravity = Gravity.CENTER)
        addBtn.background = UiKit.rounded(ctx, C.PRIMARY_BG, 24)
        addBtn.setPadding(0, Theme.dp(ctx, 13), 0, Theme.dp(ctx, 13))
        val addLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 50))
        addLp.setMargins(Theme.dp(ctx, 16), Theme.dp(ctx, 6), Theme.dp(ctx, 16), Theme.dp(ctx, 16))
        addBtn.setOnClickListener { addDialog() }
        root.addView(addBtn, addLp)

        UiKit.fitSystemBars(root)
        setContentView(root)
    }

    private fun reload() {
        items = App.db.ledgers()
        listView.adapter = LedgerAdapter()
    }

    private fun addDialog() {
        val name = EditText(ctx).apply {
            setTextSize(14f)
            hint = "账本名称"
            setTextColor(Theme.mainText(ctx))
            setHintTextColor(Theme.lightText(ctx))
        }
        val dialog = android.app.AlertDialog.Builder(ctx)
            .setTitle("新建账本")
            .setView(name)
            .setPositiveButton("确定") { _, _ ->
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    Toast.makeText(ctx, "请输入名称", Toast.LENGTH_SHORT).show()
                } else {
                    App.db.addLedger(n)
                    reload()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun switchTo(ledger: Ledger) {
        Prefs.activeLedgerId = ledger.id
        Toast.makeText(ctx, "已切换到「${ledger.name}」", Toast.LENGTH_SHORT).show()
        reload()
    }

    private fun deleteConfirm(ledger: Ledger) {
        if (ledger.isDefault) {
            Toast.makeText(ctx, "默认账本不可删除", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(ctx)
            .setTitle("删除账本「${ledger.name}」？")
            .setMessage("其下的历史账单将保留在数据库中。")
            .setPositiveButton("删除") { _, _ ->
                App.db.deleteLedger(ledger.id)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class LedgerAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(pos: Int): Any = items[pos]
        override fun getItemId(pos: Int): Long = items[pos].id

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val ledger = items[pos]
            val active = ledger.id == Prefs.activeLedgerId
            val row = UiKit.horizontal(this@LedgerManageActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Theme.surface(this@LedgerManageActivity))
            }
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(this@LedgerManageActivity, 56))
            lp.setMargins(Theme.dp(this@LedgerManageActivity, 24), Theme.dp(this@LedgerManageActivity, 2),
                Theme.dp(this@LedgerManageActivity, 24), Theme.dp(this@LedgerManageActivity, 2))

            val dot = UiKit.circle(this@LedgerManageActivity, if (active) C.PRIMARY else Theme.lightText(this@LedgerManageActivity), 14)
            val dotWrap = UiKit.horizontal(this@LedgerManageActivity).apply {
                gravity = Gravity.CENTER
                setPadding(Theme.dp(this@LedgerManageActivity, 8), 0, Theme.dp(this@LedgerManageActivity, 12), 0)
            }
            dotWrap.addView(dot)
            row.addView(dotWrap)

            val center = UiKit.vertical(this@LedgerManageActivity)
            center.gravity = Gravity.CENTER_VERTICAL
            center.addView(UiKit.text(this@LedgerManageActivity, ledger.name, 15f, Theme.mainText(this@LedgerManageActivity)))
            center.addView(UiKit.text(this@LedgerManageActivity,
                if (active) "当前账本" else if (ledger.isDefault) "默认账本" else "点击切换", 12f,
                if (active) C.PRIMARY else Theme.lightText(this@LedgerManageActivity)))
            row.addView(center, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            return row.apply { layoutParams = lp }
        }
    }
}

