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

    private var mode = 1 // 0日 1月 2年，默认按月
    private var day = DateUtil.today()
    private var monthKey = DateUtil.monthNow()
    private var year = DateUtil.yearOf(DateUtil.monthNow())

    private val tvNav = UiKit.text(host, "", 16f, Theme.mainText(host), bold = true, gravity = Gravity.CENTER)
    private lateinit var tvCurrent: TextView
    private val tvSummaryLabel = UiKit.text(host, "", 13f, Theme.subText(host))
    private val tvExpense = UiKit.text(host, "", 34f, C.EXPENSE, bold = true)
    private val tvIncome = UiKit.text(host, "", 16f, C.INCOME, bold = true)
    private val tvBalance = UiKit.text(host, "", 16f, Theme.mainText(host), bold = true)
    private val listContainer = UiKit.vertical(host)
    private val segButtons = ArrayList<TextView>()

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.pageBg(host))
        buildUi()
        refresh()
    }

    private fun buildUi() {
        // 口径切换 + 回到当前
        val segWrap = LinearLayout(host).apply {
            background = UiKit.rounded(host, Theme.surface(host), 22)
        }
        val seg = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER
            setPadding(Theme.dp(host, 3), Theme.dp(host, 3), Theme.dp(host, 3), Theme.dp(host, 3))
        }
        seg.addView(segButton("按日", 0), segParams())
        seg.addView(segButton("按月", 1), segParams())
        seg.addView(segButton("按年", 2), segParams())
        segWrap.addView(seg, LinearLayout.LayoutParams(0, Theme.dp(host, 38), 1f))

        tvCurrent = UiKit.text(host, "本月", 13f, C.PRIMARY, bold = true, gravity = Gravity.CENTER)
        tvCurrent.background = UiKit.rounded(host, C.PRIMARY_BG, 19)
        tvCurrent.layoutParams = LinearLayout.LayoutParams(Theme.dp(host, 56), Theme.dp(host, 38))
        tvCurrent.setOnClickListener { backToCurrent() }

        val segRow = UiKit.horizontal(host).apply { gravity = Gravity.CENTER_VERTICAL }
        segRow.addView(segWrap, LinearLayout.LayoutParams(0, Theme.dp(host, 44), 1f))
        segRow.addView(tvCurrent, LinearLayout.LayoutParams(Theme.dp(host, 56), Theme.dp(host, 38)).apply {
            setMargins(Theme.dp(host, 10), 0, 0, 0)
        })
        addView(segRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 52)).apply {
            setMargins(Theme.dp(host, 24), Theme.dp(host, 8), Theme.dp(host, 24), 0)
        })

        // 日期导航
        val navRow = UiKit.horizontal(host).apply { gravity = Gravity.CENTER }
        navRow.addView(navArrow(-1))
        navRow.addView(tvNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        navRow.addView(navArrow(1))
        tvNav.setOnClickListener {
            when (mode) {
                0 -> pickDay()
                1 -> pickMonth()
                else -> pickYear()
            }
        }
        addView(navRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 52)))

        // 统计卡片：surface 配色，对比清晰但不突兀
        val card = UiKit.vertical(host).apply {
            gravity = Gravity.CENTER
            background = UiKit.rounded(host, Theme.surface(host), 18)
            setPadding(Theme.dp(host, 20), Theme.dp(host, 14), Theme.dp(host, 20), Theme.dp(host, 14))
        }
        val cardLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        cardLp.setMargins(Theme.dp(host, 24), Theme.dp(host, 4), Theme.dp(host, 24), Theme.dp(host, 4))
        card.addView(tvSummaryLabel)
        card.addView(tvExpense, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topMargin = Theme.dp(host, 2)
        })
        val divider = View(host).apply {
            background = UiKit.rounded(host, Theme.divider(host), 1)
        }
        val dividerLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 1))
        dividerLp.setMargins(Theme.dp(host, 16), Theme.dp(host, 10), Theme.dp(host, 16), Theme.dp(host, 10))
        card.addView(divider, dividerLp)
        val summary = UiKit.horizontal(host)
        summary.gravity = Gravity.CENTER
        summary.addView(heroStat("收入", tvIncome), sumParams())
        summary.addView(heroStat("结余", tvBalance), sumParams())
        card.addView(summary, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(card, cardLp)

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
        val lp = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)
        lp.setMargins(Theme.dp(host, 3), 0, Theme.dp(host, 3), 0)
        return lp
    }

    private fun updateSeg() {
        for (tv in segButtons) {
            val active = tv.tag as Int == mode
            tv.background = if (active) UiKit.rounded(host, C.PRIMARY, 18) else null
            tv.setTextColor(if (active) 0xFFFFFFFF.toInt() else Theme.subText(host))
            tv.setTypeface(tv.typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
        tvCurrent.text = when (mode) {
            0 -> "今天"
            1 -> "本月"
            else -> "今年"
        }
        updateCurrentVisibility()
    }

    private fun updateCurrentVisibility() {
        val isCurrent = when (mode) {
            0 -> day == DateUtil.today()
            1 -> monthKey == DateUtil.monthNow()
            else -> year == DateUtil.yearOf(DateUtil.monthNow())
        }
        tvCurrent.visibility = if (isCurrent) View.GONE else View.VISIBLE
    }

    private fun backToCurrent() {
        when (mode) {
            0 -> day = DateUtil.today()
            1 -> monthKey = DateUtil.monthNow()
            else -> year = DateUtil.yearOf(DateUtil.monthNow())
        }
        updateNav()
        refresh()
    }

    private fun navArrow(dir: Int): View {
        val btn = TextView(host).apply {
            text = if (dir < 0) "‹" else "›"
            textSize = 28f
            setTextColor(Theme.subText(host))
            gravity = Gravity.CENTER
            background = UiKit.rounded(host, Theme.surface(host), 22)
        }
        btn.layoutParams = LinearLayout.LayoutParams(Theme.dp(host, 48), Theme.dp(host, 48))
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

    /** 按日：点击标题弹出日期列表（近一年） */
    private fun pickDay() {
        val items = ArrayList<Pair<Int, String>>()
        val today = DateUtil.today()
        for (i in 364 downTo 0) {
            val d = DateUtil.addDays(today, -i)
            items.add(d to DateUtil.fullDate(d))
        }
        val names = items.map { it.second }.toTypedArray()
        val selected = items.indexOfFirst { it.first == day }.coerceAtLeast(0)
        pickList("选择日期", names, selected) { idx ->
            day = items[idx].first
            updateNav()
            refresh()
        }
    }

    /** 按月：点击标题弹出月份列表（近 5 年） */
    private fun pickMonth() {
        val nowMonth = DateUtil.monthNow()
        val items = ArrayList<Pair<Int, String>>()
        for (i in 59 downTo 0) {
            val mk = DateUtil.addMonths(nowMonth, -i)
            items.add(mk to DateUtil.monthTitle(mk))
        }
        val names = items.map { it.second }.toTypedArray()
        val selected = items.indexOfFirst { it.first == monthKey }.coerceAtLeast(0)
        pickList("选择月份", names, selected) { idx ->
            monthKey = items[idx].first
            updateNav()
            refresh()
        }
    }

    /** 按年：点击标题弹出年份列表 */
    private fun pickYear() {
        val nowYear = DateUtil.yearOf(DateUtil.monthNow())
        val years = (nowYear - 5..nowYear + 1).toList()
        val names = years.map { "${it}年" }.toTypedArray()
        val selected = years.indexOf(year).coerceAtLeast(0)
        pickList("选择年份", names, selected) { idx ->
            year = years[idx]
            updateNav()
            refresh()
        }
    }

    private fun pickList(title: String, names: Array<String>, selected: Int, onPick: (Int) -> Unit) {
        android.app.AlertDialog.Builder(host)
            .setTitle(title)
            .setItems(names) { _, which -> onPick(which) }
            .show()
            .let { dialog ->
                val listView = dialog.listView
                listView.setSelection(selected)
            }
    }

    private fun heroStat(label: String, tv: TextView): LinearLayout {
        val c = UiKit.vertical(host)
        c.gravity = Gravity.CENTER
        c.addView(UiKit.text(host, label, 12f, Theme.subText(host)))
        c.addView(tv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(0, Theme.dp(host, 2), 0, 0)
        })
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
        tvSummaryLabel.text = when (mode) {
            0 -> "今日支出"
            1 -> "本月支出"
            else -> "本年支出"
        }
        updateCurrentVisibility()
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

