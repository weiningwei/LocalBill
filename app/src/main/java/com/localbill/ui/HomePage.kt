package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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

class HomePage(private val host: MainActivity) : LinearLayout(host) {

    private var monthKey = DateUtil.monthNow()
    private val tvMonth = UiKit.text(host, "", 18f, Theme.mainText(host), bold = true, gravity = Gravity.CENTER)
    private val tvCurrent = UiKit.text(host, "本月", 12f, Theme.primary(host), bold = true, gravity = Gravity.CENTER).apply {
        background = UiKit.rounded(host, Theme.primaryBg(host), 16)
        setOnClickListener { backToCurrent() }
    }
    private val tvExpense = UiKit.text(host, "0.00", 36f, C.EXPENSE, bold = true)
    private val tvIncome = UiKit.text(host, "0.00", 16f, C.INCOME, bold = true)
    private val tvBalance = UiKit.text(host, "0.00", 16f, Theme.mainText(host), bold = true)
    private val listContainer = UiKit.vertical(host)

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.pageBg(host))
        buildUi()
        refresh()
    }

    private fun buildUi() {
        // 月份切换
        val monthRow = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER
            setPadding(0, Theme.dp(host, 10), 0, Theme.dp(host, 2))
        }
        monthRow.addView(arrow(-1))
        monthRow.addView(tvMonth, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        monthRow.addView(arrow(1))
        monthRow.addView(tvCurrent, LinearLayout.LayoutParams(Theme.dp(host, 48), Theme.dp(host, 32)).apply {
            setMargins(Theme.dp(host, 8), 0, 0, 0)
        })
        addView(monthRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 44)))

        // 本月统计大卡片
        val card = UiKit.vertical(host).apply {
            gravity = Gravity.CENTER
            background = UiKit.rounded(host, Theme.surface(host), 18)
            setPadding(Theme.dp(host, 20), Theme.dp(host, 14), Theme.dp(host, 20), Theme.dp(host, 14))
        }
        val cardLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        cardLp.setMargins(Theme.dp(host, 24), Theme.dp(host, 6), Theme.dp(host, 24), Theme.dp(host, 2))
        card.addView(UiKit.text(host, "本月支出", 13f, Theme.subText(host)))
        card.addView(tvExpense, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topMargin = Theme.dp(host, 2)
        })
        val divider = View(host).apply {
            background = UiKit.rounded(host, Theme.divider(host), 1)
        }
        val dividerLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 1))
        dividerLp.setMargins(Theme.dp(host, 16), Theme.dp(host, 10), Theme.dp(host, 16), Theme.dp(host, 10))
        card.addView(divider, dividerLp)
        val row = UiKit.horizontal(host)
        row.gravity = Gravity.CENTER
        row.addView(heroCol(host.getString(R.string.month_income), tvIncome), weight(1))
        row.addView(heroCol(host.getString(R.string.month_balance), tvBalance), weight(1))
        card.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(card, cardLp)

        // 记一笔按钮
        val recordBtn = UiKit.text(host, "＋ 记一笔", 14f, 0xFFFFFFFF.toInt(), bold = true, gravity = Gravity.CENTER)
        recordBtn.background = UiKit.rounded(host, Theme.primary(host), 20)
        val recordLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 40))
        recordLp.setMargins(Theme.dp(host, 24), Theme.dp(host, 2), Theme.dp(host, 24), Theme.dp(host, 2))
        recordBtn.setOnClickListener { host.openRecord(null) }
        addView(recordBtn, recordLp)

        // 今日明细
        val label = UiKit.horizontal(host).apply {
            setPadding(Theme.dp(host, 14), Theme.dp(host, 8), Theme.dp(host, 14), Theme.dp(host, 4))
        }
        label.addView(UiKit.text(host, "今日明细", 15f, Theme.mainText(host), bold = true))
        addView(label)

        val scroll = ScrollView(host)
        scroll.isFillViewport = true
        scroll.addView(listContainer, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
    }

    private fun arrow(dir: Int): View {
        val btn = TextView(host).apply {
            text = if (dir < 0) "‹" else "›"
            textSize = 26f
            setTextColor(Theme.subText(host))
            gravity = Gravity.CENTER
        }
        btn.layoutParams = LinearLayout.LayoutParams(Theme.dp(host, 40), Theme.dp(host, 40))
        btn.setOnClickListener {
            monthKey = DateUtil.addMonths(monthKey, dir)
            refresh()
        }
        return btn
    }

    private fun backToCurrent() {
        monthKey = DateUtil.monthNow()
        refresh()
    }

    private fun updateCurrentVisibility() {
        tvCurrent.visibility = if (monthKey == DateUtil.monthNow()) View.GONE else View.VISIBLE
    }

    private fun heroCol(label: String, amount: TextView): LinearLayout {
        val c = UiKit.vertical(host)
        c.gravity = Gravity.CENTER
        c.addView(UiKit.text(host, label, 12f, Theme.subText(host)))
        c.addView(amount, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(0, Theme.dp(host, 2), 0, 0)
        })
        return c
    }

    private fun weight(w: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w.toFloat())

    fun refresh() {
        tvMonth.text = DateUtil.monthTitle(monthKey)
        updateCurrentVisibility()
        val ledger = App.db.activeLedger()
        val from = DateUtil.firstDayOfMonth(monthKey)
        val to = DateUtil.lastDayOfMonth(monthKey)
        val exp = App.db.sumKind(ledger.id, Kinds.EXPENSE, from, to)
        val inc = App.db.sumKind(ledger.id, Kinds.INCOME, from, to)
        tvExpense.text = Money.format(exp)
        tvIncome.text = Money.format(inc)
        tvBalance.text = Money.format(inc - exp)

        listContainer.removeAllViews()
        val today = DateUtil.today()
        val bills = App.db.bills(ledger.id, today, today)
        if (bills.isEmpty()) {
            val empty = UiKit.text(host, host.getString(R.string.no_bills), 14f, Theme.lightText(host), gravity = Gravity.CENTER)
            empty.setPadding(0, Theme.dp(host, 40), 0, Theme.dp(host, 40))
            listContainer.addView(empty, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        } else {
            bills.forEach { listContainer.addView(billRow(it), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
        }
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
        val name = cat?.name ?: "未知"
        center.addView(UiKit.text(host, name, 15f, Theme.mainText(host)))
        val timeStr = DateUtil.timeText(bill.time)
        val sub = when {
            bill.remark.isNotEmpty() -> "$timeStr · ${bill.remark}"
            account != null -> "$timeStr · ${account.name}"
            else -> timeStr
        }
        if (sub.isNotEmpty()) {
            center.addView(UiKit.text(host, sub, 12f, Theme.lightText(host)))
        }

        val amount = UiKit.text(host, Money.format(bill.amount), 16f,
            if (bill.kind == Kinds.EXPENSE) C.EXPENSE else C.INCOME, bold = true)
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
}

