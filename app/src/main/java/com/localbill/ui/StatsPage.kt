package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.localbill.App
import com.localbill.model.Kinds
import com.localbill.util.C
import com.localbill.util.DateUtil
import com.localbill.util.Money
import com.localbill.util.Theme
import com.localbill.util.UiKit
import kotlin.math.roundToInt

class StatsPage(private val host: MainActivity) : LinearLayout(host) {

    private var monthKey = DateUtil.monthNow()
    private val tvMonth = UiKit.text(host, "", 16f, Theme.mainText(host), bold = true, gravity = Gravity.CENTER)
    private val tvExpense = UiKit.text(host, "", 26f, C.EXPENSE, bold = true)
    private val tvIncome = UiKit.text(host, "", 15f, C.INCOME, bold = true)
    private val tvBalance = UiKit.text(host, "", 15f, Theme.subText(host), bold = true)
    private val pieChart = PieChartView(host)
    private val barChart = BarChartView(host)
    private val legendBox = UiKit.vertical(host)
    private val rankBox = UiKit.vertical(host)

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.pageBg(host))
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val navRow = UiKit.horizontal(host).apply { gravity = Gravity.CENTER }
        navRow.addView(navArrow(-1))
        navRow.addView(tvMonth, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        navRow.addView(navArrow(1))
        addView(navRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 44)))

        val scroll = ScrollView(host)
        scroll.isFillViewport = true
        val body = UiKit.vertical(host)

        // 汇总卡片
        val card = UiKit.vertical(host).apply { background = UiKit.rounded(host, Theme.surface(host), 14) }
        val cardLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        cardLp.setMargins(Theme.dp(host, 24), Theme.dp(host, 4), Theme.dp(host, 24), Theme.dp(host, 4))
        val row = UiKit.horizontal(host)
        row.gravity = Gravity.CENTER
        row.addView(col("支出", tvExpense, C.EXPENSE), weight(1))
        row.addView(col("收入", tvIncome, C.INCOME), weight(1))
        row.addView(col("结余", tvBalance, Theme.subText(host)), weight(1))
        card.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 90)))
        body.addView(card, cardLp)

        // 分类占比
        body.addView(sectionTitle("支出分类占比"))
        val pieWrap = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER
        }
        val pieLp = LinearLayout.LayoutParams(Theme.dp(host, 170), Theme.dp(host, 170))
        pieLp.topMargin = Theme.dp(host, 4)
        pieLp.bottomMargin = Theme.dp(host, 8)
        pieWrap.addView(pieChart, pieLp)
        body.addView(pieWrap)
        body.addView(legendBox)

        // 趋势
        body.addView(sectionTitle("近12月支出趋势"))
        barChart.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(host, 170))
        barChart.setPadding(Theme.dp(host, 8), Theme.dp(host, 4), Theme.dp(host, 8), 0)
        body.addView(barChart)

        // 排行
        body.addView(sectionTitle("分类支出排行"))
        body.addView(rankBox)

        scroll.addView(body, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
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
            monthKey = DateUtil.addMonths(monthKey, dir)
            refresh()
        }
        return btn
    }

    private fun sectionTitle(text: String): TextView {
        val tv = UiKit.text(host, text, 14f, Theme.mainText(host), bold = true)
        tv.setPadding(Theme.dp(host, 14), Theme.dp(host, 12), Theme.dp(host, 14), Theme.dp(host, 2))
        return tv
    }

    private fun col(label: String, amount: TextView, color: Int): LinearLayout {
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
        val ledger = App.db.activeLedger()
        val from = DateUtil.firstDayOfMonth(monthKey)
        val to = DateUtil.lastDayOfMonth(monthKey)
        val exp = App.db.sumKind(ledger.id, Kinds.EXPENSE, from, to)
        val inc = App.db.sumKind(ledger.id, Kinds.INCOME, from, to)
        tvExpense.text = Money.format(exp)
        tvIncome.text = Money.format(inc)
        tvBalance.text = Money.format(inc - exp)

        // 分类占比
        val amounts = App.db.categoryAmounts(ledger.id, Kinds.EXPENSE, from, to)
        val total = amounts.sumOf { it.second }
        pieChart.slices = amounts.map { PieChartView.Slice(it.first.color, it.second.toFloat()) }
        legendBox.removeAllViews()
        if (total > 0) {
            amounts.forEach { (cat, amt) ->
                val pct = amt * 100f / total
                legendBox.addView(legendRow(cat.color, cat.name, Money.format(amt), pct))
            }
        } else {
            legendBox.addView(UiKit.text(host, "本月暂无支出", 13f, Theme.lightText(host),
                gravity = Gravity.CENTER).apply {
                setPadding(0, Theme.dp(host, 10), 0, Theme.dp(host, 10))
            })
        }

        // 趋势
        val series = App.db.monthlySeries(ledger.id, Kinds.EXPENSE, 12)
        barChart.bars = series.map { (mk, amt) ->
            BarChartView.Bar("${DateUtil.monthIdx(mk)}月", amt.toFloat())
        }

        // 排行
        rankBox.removeAllViews()
        if (total > 0) {
            amounts.forEach { (cat, amt) ->
                rankBox.addView(rankRow(cat.color, cat.name, Money.format(amt), amt * 100f / total))
            }
        }
    }

    private fun legendRow(color: Int, name: String, amount: String, pct: Float): LinearLayout {
        val row = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Theme.dp(host, 14), Theme.dp(host, 3), Theme.dp(host, 14), Theme.dp(host, 3))
        }
        row.addView(UiKit.circle(host, color, 10))
        row.addView(UiKit.text(host, name, 14f, Theme.mainText(host)).apply {
            setPadding(Theme.dp(host, 8), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(UiKit.text(host, "${String.format("%.1f", pct)}%", 13f, Theme.lightText(host)))
        row.addView(UiKit.text(host, amount, 14f, Theme.mainText(host), bold = true).apply {
            setPadding(Theme.dp(host, 12), 0, 0, 0)
        })
        return row
    }

    private fun rankRow(color: Int, name: String, amount: String, pct: Float): LinearLayout {
        val row = UiKit.vertical(host).apply {
            setPadding(Theme.dp(host, 14), Theme.dp(host, 5), Theme.dp(host, 14), Theme.dp(host, 5))
        }
        val top = UiKit.horizontal(host).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(UiKit.circle(host, color, 10))
        top.addView(UiKit.text(host, name, 14f, Theme.mainText(host)).apply {
            setPadding(Theme.dp(host, 8), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        top.addView(UiKit.text(host, amount, 14f, Theme.mainText(host), bold = true))
        row.addView(top)

        val bar = View(host)
        val widthPx = (host.resources.displayMetrics.widthPixels * (pct / 100f)).toInt()
        bar.background = UiKit.rounded(host, color, 2)
        val barLp = LinearLayout.LayoutParams(widthPx, Theme.dp(host, 4))
        barLp.setMargins(Theme.dp(host, 18), Theme.dp(host, 5), 0, 0)
        row.addView(bar, barLp)
        return row
    }
}

