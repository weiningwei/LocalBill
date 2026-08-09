package com.localbill.util

import android.content.Context
import com.localbill.R

object Theme {
    val isDark: Boolean
        get() = Prefs.darkMode

    /** 根据主题色 + 亮暗返回对应主题样式 */
    fun themeStyle(): Int = when (Prefs.themeColor) {
        "green" -> if (isDark) R.style.Theme_LocalBill_Green_Dark else R.style.Theme_LocalBill_Green_Light
        "purple" -> if (isDark) R.style.Theme_LocalBill_Purple_Dark else R.style.Theme_LocalBill_Purple_Light
        "orange" -> if (isDark) R.style.Theme_LocalBill_Orange_Dark else R.style.Theme_LocalBill_Orange_Light
        else -> if (isDark) R.style.Theme_LocalBill_Blue_Dark else R.style.Theme_LocalBill_Blue_Light
    }

    fun pageBg(ctx: Context): Int = attrColor(ctx, R.attr.pageBg, C.BG)
    fun surface(ctx: Context): Int = attrColor(ctx, R.attr.surfaceColor, C.CARD)
    fun border(ctx: Context): Int = attrColor(ctx, R.attr.borderColor, C.BORDER)
    fun divider(ctx: Context): Int = attrColor(ctx, R.attr.dividerColor, C.DIVIDER)
    fun mainText(ctx: Context): Int = attrColor(ctx, R.attr.mainText, C.TEXT_MAIN)
    fun subText(ctx: Context): Int = attrColor(ctx, R.attr.subText, C.TEXT_SUB)
    fun lightText(ctx: Context): Int = attrColor(ctx, R.attr.lightText, C.TEXT_LIGHT)

    fun primary(ctx: Context): Int = attrColor(ctx, R.attr.primaryColor, C.PRIMARY)
    fun primaryDark(ctx: Context): Int = attrColor(ctx, R.attr.primaryDark, C.PRIMARY_DARK)
    fun primaryLight(ctx: Context): Int = attrColor(ctx, R.attr.primaryLight, C.PRIMARY_LIGHT)
    fun primaryBg(ctx: Context): Int = attrColor(ctx, R.attr.primaryBg, C.PRIMARY_BG)

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
