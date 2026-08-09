package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.localbill.R
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.model.Bill
import com.localbill.model.Kinds
import com.localbill.util.C
import com.localbill.util.DateUtil
import com.localbill.util.Money
import com.localbill.util.Theme
import com.localbill.util.UiKit

class BillsPage(private val host: MainActivity) : LinearLayout(host) {

    private var mode = 0 // 0日 1月 2年
    private var day = DateUtil.today()
    private var monthKey = DateUtil.monthNow()
    private var year = DateUtil.yearOf(DateUtil.monthNow())

    private val tvNav = UiKit.text(host, "", 16f, Theme.mainText(host), bold = true, gravity = Gravity.CENTER)
    private val tvExpense = UiKit.text(host, "", 14f, C.EXPENSE, bold = true)
    private val tvIncome = UiKit.text(host, "", 14f, C.INCOME, bold = true)
    private val tvBalance = UiKit.text(host, "", 14f, Theme.subText(host), bold = true)
    private val listContainer = UiKit.vertical(host)
    private val segButtons = ArrayList<TextView>()

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.pageBg(host))
        buildUi()
        refresh()
    }

    private fun buildUi() {
        // 口径切换
        val segRow = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER
            setPadding(0, Theme.dp(host, 10), 0, Theme.dp(host, 2))
        }
        segRow.addView(segButton("按日", 0), segParams())
        segRow.addView(segButton("按月", 1), segParams())
        segRow.addView(segButton("按年", 2), segParams())
        addView(segRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 40)))

        // 日期导航
        val navRow = UiKit.horizontal(host).apply { gravity = Gravity.CENTER }
        navRow.addView(navArrow(-1))
        navRow.addView(tvNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        navRow.addView(navArrow(1))
        addView(navRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 42)))

        // 汇总
        val summary = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER
            setPadding(Theme.dp(host, 8), Theme.dp(host, 4), Theme.dp(host, 8), Theme.dp(host, 4))
        }
        summary.addView(smallStat("支出", tvExpense), sumParams())
        summary.addView(smallStat("收入", tvIncome), sumParams())
        summary.addView(smallStat("结余", tvBalance), sumParams())
        addView(summary, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(host)
        scroll.isFillViewport = true
        scroll.addView(listContainer, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        updateSeg()
        updateNav()
    }

    private fun segButton(label: String, idx: Int): TextView {
        val tv = TextView(host).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
        }
        tv.tag = idx
        tv.setOnClickListener {
            mode = idx
            updateSeg()
            updateNav()
            refresh()
        }
        segButtons.add(tv)
        return tv
    }

    private fun segParams(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, Theme.dp(host, 34), 1f)
        lp.setMargins(Theme.dp(host, 18), 0, Theme.dp(host, 18), 0)
        return lp
    }

    private fun updateSeg() {
        for (tv in segButtons) {
            val active = tv.tag as Int == mode
            tv.setTextColor(if (active) C.PRIMARY else Theme.subText(host))
            tv.setTypeface(tv.typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun navArrow(dir: Int): View {
        val btn = TextView(host).apply {
            text = if (dir < 0) "‹" else "›"
            textSize = 24f
            setTextColor(Theme.subText(host))
            gravity = Gravity.CENTER
        }
        btn.layoutParams = LinearLayout.LayoutParams(Theme.dp(host, 40), Theme.dp(host, 40))
        btn.setOnClickListener {
            when (mode) {
                0 -> day = DateUtil.addDays(day, dir)
                1 -> monthKey = DateUtil.addMonths(monthKey, dir)
                2 -> year += dir
            }
            updateNav()
            refresh()
        }
        return btn
    }

    private fun smallStat(label: String, tv: TextView): LinearLayout {
        val c = UiKit.vertical(host)
        c.gravity = Gravity.CENTER
        c.addView(UiKit.text(host, label, 12f, Theme.subText(host)))
        c.addView(tv)
        return c
    }

    private fun sumParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun updateNav() {
        tvNav.text = when (mode) {
            0 -> DateUtil.fullDate(day)
            1 -> DateUtil.monthTitle(monthKey)
            else -> "${year}年"
        }
    }

    fun refresh() {
        val ledger = App.db.activeLedger()
        var from: Int
        var to: Int
        when (mode) {
            0 -> { from = day; to = day }
            1 -> { from = DateUtil.firstDayOfMonth(monthKey); to = DateUtil.lastDayOfMonth(monthKey) }
            else -> { from = year * 10000 + 101; to = year * 10000 + 1231 }
        }
        val bills = App.db.bills(ledger.id, from, to)
        var exp = 0L
        var inc = 0L
        bills.forEach {
            if (it.kind == Kinds.EXPENSE) exp += it.amount else inc += it.amount
        }
        tvExpense.text = Money.format(exp)
        tvIncome.text = Money.format(inc)
        tvBalance.text = Money.format(inc - exp)

        listContainer.removeAllViews()
        if (bills.isEmpty()) {
            val empty = UiKit.text(host, host.getString(R.string.no_bills), 14f, Theme.lightText(host), gravity = Gravity.CENTER)
            empty.setPadding(0, Theme.dp(host, 40), 0, Theme.dp(host, 40))
            listContainer.addView(empty, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            return
        }

        // 按组渲染
        var lastGroup = -1
        var dayExp = 0L
        var dayInc = 0L
        var groupBills = ArrayList<Bill>()
        for (bill in bills) {
            val group = if (mode == 2) DateUtil.monthOf(bill.day) else bill.day
            if (lastGroup != -1 && group != lastGroup) {
                flushGroup(lastGroup, dayExp, dayInc, groupBills)
                dayExp = 0; dayInc = 0; groupBills = ArrayList()
            }
            lastGroup = group
            groupBills.add(bill)
            if (bill.kind == Kinds.EXPENSE) dayExp += bill.amount else dayInc += bill.amount
        }
        flushGroup(lastGroup, dayExp, dayInc, groupBills)
    }

    private fun flushGroup(group: Int, exp: Long, inc: Long, bills: ArrayList<Bill>) {
        val header = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Theme.dp(host, 14), Theme.dp(host, 10), Theme.dp(host, 14), Theme.dp(host, 2))
        }
        val title = if (mode == 2) {
            val mk = group
            "${DateUtil.yearOf(mk)}年${DateUtil.monthIdx(mk)}月"
        } else {
            DateUtil.dayTitle(group)
        }
        header.addView(UiKit.text(host, title, 13f, Theme.subText(host)), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val side = if (exp > 0 && inc > 0) "支 ${Money.format(exp)}  收 ${Money.format(inc)}"
        else if (exp > 0) "支出 ${Money.format(exp)}"
        else if (inc > 0) "收入 ${Money.format(inc)}"
        else ""
        if (side.isNotEmpty()) header.addView(UiKit.text(host, side, 12f, Theme.lightText(host)))
        listContainer.addView(header)

        bills.forEach { listContainer.addView(billRow(it)) }
    }

    private fun billRow(bill: Bill): View {
        val cat = App.db.categoryById(bill.categoryId)
        val account = App.db.accountById(bill.accountId)
        val row = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Theme.surface(host))
            isClickable = true
            isFocusable = true
        }
        row.setOnClickListener { host.openRecord(bill.id) }
        row.setOnLongClickListener {
            confirmDelete(bill)
            true
        }
        val lp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 58))
        lp.setMargins(Theme.dp(host, 24), Theme.dp(host, 3), Theme.dp(host, 24), Theme.dp(host, 3))

        val dot = UiKit.catIcon(host, cat, 36)
        val dotWrap = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER
            setPadding(Theme.dp(host, 6), 0, Theme.dp(host, 8), 0)
        }
        dotWrap.addView(dot)

        val center = UiKit.vertical(host)
        center.gravity = Gravity.CENTER_VERTICAL
        val catName = cat?.name ?: "未知"
        val timeStr = DateUtil.timeText(bill.time)
        val sub = when {
            bill.remark.isNotEmpty() -> "$timeStr · ${bill.remark}"
            account != null -> "$timeStr · ${account.name}"
            else -> timeStr
        }
        center.addView(UiKit.text(host, catName, 15f, Theme.mainText(host)))
        if (sub.isNotEmpty()) center.addView(UiKit.text(host, sub, 12f, Theme.lightText(host)))

        val amount = UiKit.text(host,
            (if (bill.kind == Kinds.EXPENSE) "-" else "+") + Money.format(bill.amount),
            16f, if (bill.kind == Kinds.EXPENSE) C.EXPENSE else C.INCOME, bold = true)
        val amountWrap = UiKit.horizontal(host).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(Theme.dp(host, 8), 0, Theme.dp(host, 14), 0)
        }
        amountWrap.addView(amount)

        row.addView(dotWrap)
        row.addView(center, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(amountWrap)
        return row.apply { layoutParams = lp }
    }

    private fun confirmDelete(bill: Bill) {
        android.app.AlertDialog.Builder(host)
            .setTitle("删除这笔记录？")
            .setMessage("将移入回收站，可随时恢复。")
            .setPositiveButton("删除") { _, _ ->
                App.db.softDelete(bill.id)
                refresh()
            }
            .setNegativeButton("取消", null)
            .show()
            .let { }
    }
}

