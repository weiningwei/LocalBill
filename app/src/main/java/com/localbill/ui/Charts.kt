package com.localbill.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.min

/** 环形饼图 */
class PieChartView(context: Context) : View(context) {

    data class Slice(val color: Int, val value: Float)

    var slices: List<Slice> = emptyList()
        set(v) { field = v; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f || slices.isEmpty()) {
            paint.color = 0xFFF0F0F0.toInt()
            canvas.drawCircle(width / 2f, height / 2f, min(width, height) / 2f - 4, paint)
            return
        }
        val w = min(width, height).toFloat()
        val left = (width - w) / 2f
        val top = (height - w) / 2f
        val inset = 0f
        val rect = RectF(left + inset, top + inset, left + w - inset, top + w - inset)

        var start = -90f
        for (s in slices) {
            val sweep = s.value / total * 360f
            paint.color = s.color
            canvas.drawArc(rect, start, sweep, true, paint)
            start += sweep
        }
        // 中心挖空
        paint.color = 0xFFFFFFFF.toInt()
        val hole = w * 0.5f
        canvas.drawCircle(width / 2f, height / 2f, hole / 2f, paint)

        // 中心文字
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * 0.13f
        paint.color = 0xFF314659.toInt()
        canvas.drawText(String.format("%.0f%%", 100f), width / 2f, height / 2f + paint.textSize / 3f, paint)
    }
}

/** 柱状图 */
class BarChartView(context: Context) : View(context) {

    data class Bar(val label: String, val value: Float)

    var bars: List<Bar> = emptyList()
        set(v) { field = v; invalidate() }
    var barColor: Int = 0xFF1890FF.toInt()
        set(v) { field = v; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return
        val maxVal = bars.maxOfOrNull { it.value } ?: 1f
        val topVal = if (maxVal <= 0f) 1f else maxVal
        val pad = 12f
        val labelH = 22f
        val topPad = 8f
        val chartH = height - labelH - topPad
        val n = bars.size
        val slot = width.toFloat() / n
        val barW = slot * 0.55f

        // 网格线
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = 0xFFE8E8E8.toInt()
        canvas.drawLine(pad, topPad, width - pad, topPad + chartH, paint)
        canvas.drawLine(pad, topPad + chartH, width - pad, topPad + chartH, paint)

        paint.style = Paint.Style.FILL
        for ((i, bar) in bars.withIndex()) {
            val h = if (bar.value <= 0f) 0f else max(3f, bar.value / topVal * (chartH - 6f))
            val cx = slot * i + slot / 2f
            val left = cx - barW / 2f
            val top = topPad + (chartH - h)
            paint.color = barColor
            val alpha = (0.35f + 0.65f * (bar.value / topVal)).coerceIn(0f, 1f)
            paint.color = withAlpha(barColor, alpha)
            canvas.drawRoundRect(left, top, left + barW, top + chartH, 4f, 4f, paint)

            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11f
            paint.color = 0xFF697B8C.toInt()
            canvas.drawText(bar.label, cx, height - 6f, paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (255 * alpha).toInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0xFFFFFF)
    }
}
