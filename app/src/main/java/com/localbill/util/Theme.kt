package com.localbill.util

import android.content.Context
import com.localbill.R

object Theme {
    val isDark: Boolean
        get() = Prefs.darkMode

    fun pageBg(ctx: Context): Int = attrColor(ctx, R.attr.pageBg, C.BG)
    fun surface(ctx: Context): Int = attrColor(ctx, R.attr.surfaceColor, C.CARD)
    fun border(ctx: Context): Int = attrColor(ctx, R.attr.borderColor, C.BORDER)
    fun divider(ctx: Context): Int = attrColor(ctx, R.attr.dividerColor, C.DIVIDER)
    fun mainText(ctx: Context): Int = attrColor(ctx, R.attr.mainText, C.TEXT_MAIN)
    fun subText(ctx: Context): Int = attrColor(ctx, R.attr.subText, C.TEXT_SUB)
    fun lightText(ctx: Context): Int = attrColor(ctx, R.attr.lightText, C.TEXT_LIGHT)

    fun attrColor(ctx: Context, attr: Int, fallback: Int): Int {
        val a = ctx.theme.obtainStyledAttributes(intArrayOf(attr))
        return try {
            a.getColor(0, fallback)
        } finally {
            a.recycle()
        }
    }

    fun dp(ctx: Context, value: Int): Int {
        return (value * ctx.resources.displayMetrics.density + 0.5f).toInt()
    }
}
