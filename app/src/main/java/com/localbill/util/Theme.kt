package com.localbill.util

import android.content.Context
import com.localbill.R

object Theme {
    val isDark: Boolean
        get() = Prefs.darkMode

    /** 当前是否为自定义 RGB 主题色 */
    val isCustomTheme: Boolean
        get() = Prefs.themeColor == "custom"

    /** 根据主题色 + 亮暗返回对应主题样式（自定义色走蓝色基座，实际色值在 accessor 里动态计算） */
    fun themeStyle(): Int = when (Prefs.themeColor) {
        "green" -> if (isDark) R.style.Theme_LocalBill_Green_Dark else R.style.Theme_LocalBill_Green_Light
        "purple" -> if (isDark) R.style.Theme_LocalBill_Purple_Dark else R.style.Theme_LocalBill_Purple_Light
        "orange" -> if (isDark) R.style.Theme_LocalBill_Orange_Dark else R.style.Theme_LocalBill_Orange_Light
        "cyan" -> if (isDark) R.style.Theme_LocalBill_Cyan_Dark else R.style.Theme_LocalBill_Cyan_Light
        "pink" -> if (isDark) R.style.Theme_LocalBill_Pink_Dark else R.style.Theme_LocalBill_Pink_Light
        "indigo" -> if (isDark) R.style.Theme_LocalBill_Indigo_Dark else R.style.Theme_LocalBill_Indigo_Light
        "lime" -> if (isDark) R.style.Theme_LocalBill_Lime_Dark else R.style.Theme_LocalBill_Lime_Light
        "sage" -> if (isDark) R.style.Theme_LocalBill_Sage_Dark else R.style.Theme_LocalBill_Sage_Light
        else -> if (isDark) R.style.Theme_LocalBill_Blue_Dark else R.style.Theme_LocalBill_Blue_Light
    }

    fun pageBg(ctx: Context): Int = attrColor(ctx, R.attr.pageBg, C.BG)
    fun surface(ctx: Context): Int = attrColor(ctx, R.attr.surfaceColor, C.CARD)
    fun border(ctx: Context): Int = attrColor(ctx, R.attr.borderColor, C.BORDER)
    fun divider(ctx: Context): Int = attrColor(ctx, R.attr.dividerColor, C.DIVIDER)
    fun mainText(ctx: Context): Int = attrColor(ctx, R.attr.mainText, C.TEXT_MAIN)
    fun subText(ctx: Context): Int = attrColor(ctx, R.attr.subText, C.TEXT_SUB)
    fun lightText(ctx: Context): Int = attrColor(ctx, R.attr.lightText, C.TEXT_LIGHT)

    fun primary(ctx: Context): Int =
        if (isCustomTheme) Prefs.customThemeColor
        else attrColor(ctx, R.attr.primaryColor, C.PRIMARY)

    fun primaryDark(ctx: Context): Int =
        if (isCustomTheme) shade(Prefs.customThemeColor, 0.3f)
        else attrColor(ctx, R.attr.primaryDark, C.PRIMARY_DARK)

    fun primaryLight(ctx: Context): Int =
        if (isCustomTheme) tint(Prefs.customThemeColor, 0.4f)
        else attrColor(ctx, R.attr.primaryLight, C.PRIMARY_LIGHT)

    fun primaryBg(ctx: Context): Int =
        if (isCustomTheme) {
            if (isDark) shade(Prefs.customThemeColor, 0.87f) else tint(Prefs.customThemeColor, 0.9f)
        } else {
            attrColor(ctx, R.attr.primaryBg, C.PRIMARY_BG)
        }

    fun attrColor(ctx: Context, attr: Int, fallback: Int): Int {
        val a = ctx.theme.obtainStyledAttributes(intArrayOf(attr))
        return try {
            a.getColor(0, fallback)
        } finally {
            a.recycle()
        }
    }

    /** 与目标色混色，ratio 为目标色占比（0~1） */
    private fun mix(color: Int, target: Int, ratio: Float): Int {
        val r = (color shr 16 and 0xFF) * (1f - ratio) + (target shr 16 and 0xFF) * ratio
        val g = (color shr 8 and 0xFF) * (1f - ratio) + (target shr 8 and 0xFF) * ratio
        val b = (color and 0xFF) * (1f - ratio) + (target and 0xFF) * ratio
        return (0xFF shl 24) or ((r.toInt() and 0xFF) shl 16) or ((g.toInt() and 0xFF) shl 8) or (b.toInt() and 0xFF)
    }

    /** 向白色混色，得到更浅的颜色 */
    private fun tint(color: Int, whiteRatio: Float): Int = mix(color, 0xFFFFFF, whiteRatio)

    /** 向黑色混色，得到更深的颜色 */
    private fun shade(color: Int, blackRatio: Float): Int = mix(color, 0x000000, blackRatio)

    fun dp(ctx: Context, value: Int): Int {
        return (value * ctx.resources.displayMetrics.density + 0.5f).toInt()
    }
}
