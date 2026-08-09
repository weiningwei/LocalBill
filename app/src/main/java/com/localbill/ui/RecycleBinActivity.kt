package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.model.Bill
import com.localbill.model.Kinds
import com.localbill.util.C
import com.localbill.util.DateUtil
import com.localbill.util.Money
import com.localbill.util.Theme
import com.localbill.util.UiKit

class RecycleBinActivity : Activity() {
    private val ctx: Context get() = this@RecycleBinActivity

    private var items: List<Bill> = emptyList()
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView

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
        topBar.addView(UiKit.text(ctx, "回收站", 17f, Theme.mainText(ctx), bold = true, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val emptyAll = UiKit.text(ctx, "清空", 15f, Theme.subText(ctx))
        emptyAll.setPadding(Theme.dp(ctx, 12), Theme.dp(ctx, 12), Theme.dp(ctx, 16), Theme.dp(ctx, 12))
        emptyAll.setOnClickListener { emptyAll() }
        topBar.addView(emptyAll)
        root.addView(topBar, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))

        listView = ListView(ctx)
        listView.divider = null
        listView.setSelector(android.R.color.transparent)
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        emptyView = UiKit.text(ctx, "回收站是空的", 14f, Theme.lightText(ctx), gravity = Gravity.CENTER)
        emptyView.setPadding(0, Theme.dp(ctx, 60), 0, Theme.dp(ctx, 60))
        root.addView(emptyView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        emptyView.visibility = View.GONE

        UiKit.fitSystemBars(root)
        setContentView(root)
    }

    private fun reload() {
        items = App.db.recycledBills()
        listView.adapter = RecycledAdapter()
        listView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun emptyAll() {
        android.app.AlertDialog.Builder(ctx)
            .setTitle("清空回收站？")
            .setMessage("全部记录将被永久删除，不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                App.db.emptyRecycle()
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class RecycledAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(pos: Int): Any = items[pos]
        override fun getItemId(pos: Int): Long = items[pos].id

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val bill = items[pos]
            val cat = App.db.categoryById(bill.categoryId)
            val row = UiKit.vertical(this@RecycleBinActivity).apply {
                background = UiKit.rounded(this@RecycleBinActivity, Theme.surface(this@RecycleBinActivity), 12)
            }
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            lp.setMargins(Theme.dp(this@RecycleBinActivity, 24), Theme.dp(this@RecycleBinActivity, 3),
                Theme.dp(this@RecycleBinActivity, 24), Theme.dp(this@RecycleBinActivity, 3))

            val top = UiKit.horizontal(this@RecycleBinActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(Theme.dp(this@RecycleBinActivity, 12), Theme.dp(this@RecycleBinActivity, 8),
                    Theme.dp(this@RecycleBinActivity, 12), 0)
            }
            top.addView(UiKit.text(this@RecycleBinActivity, cat?.name ?: "未知", 15f, Theme.mainText(this@RecycleBinActivity)),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            top.addView(UiKit.text(this@RecycleBinActivity, Money.format(bill.amount), 15f,
                if (bill.kind == Kinds.EXPENSE) C.EXPENSE else C.INCOME, bold = true))
            row.addView(top)

            val mid = UiKit.horizontal(this@RecycleBinActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(Theme.dp(this@RecycleBinActivity, 12), Theme.dp(this@RecycleBinActivity, 2),
                    Theme.dp(this@RecycleBinActivity, 12), 0)
            }
            mid.addView(UiKit.text(this@RecycleBinActivity,
                "${DateUtil.fullDate(bill.day)} ${DateUtil.timeText(bill.time)}  ${if (bill.remark.isNotEmpty()) bill.remark else ""}",
                12f, Theme.lightText(this@RecycleBinActivity)),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            row.addView(mid)

            val ops = UiKit.horizontal(this@RecycleBinActivity).apply {
                gravity = Gravity.RIGHT
                setPadding(Theme.dp(this@RecycleBinActivity, 8), Theme.dp(this@RecycleBinActivity, 4),
                    Theme.dp(this@RecycleBinActivity, 8), Theme.dp(this@RecycleBinActivity, 6))
            }
            val restore = UiKit.text(this@RecycleBinActivity, "恢复", 14f, C.PRIMARY, bold = true)
            restore.setPadding(Theme.dp(this@RecycleBinActivity, 14), Theme.dp(this@RecycleBinActivity, 6),
                Theme.dp(this@RecycleBinActivity, 14), Theme.dp(this@RecycleBinActivity, 6))
            restore.setOnClickListener {
                App.db.restore(bill.id)
                Toast.makeText(this@RecycleBinActivity, R.string.toast_restored, Toast.LENGTH_SHORT).show()
                reload()
            }
            val del = UiKit.text(this@RecycleBinActivity, "永久删除", 14f, C.EXPENSE)
            del.setPadding(Theme.dp(this@RecycleBinActivity, 14), Theme.dp(this@RecycleBinActivity, 6),
                Theme.dp(this@RecycleBinActivity, 14), Theme.dp(this@RecycleBinActivity, 6))
            del.setOnClickListener {
                android.app.AlertDialog.Builder(this@RecycleBinActivity)
                    .setMessage("永久删除这条记录？")
                    .setPositiveButton("删除") { _, _ ->
                        App.db.hardDelete(bill.id)
                        reload()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            ops.addView(restore)
            ops.addView(del)
            row.addView(ops)
            return row.apply { layoutParams = lp }
        }
    }
}

