package com.localbill.util

object C {
    const val PRIMARY = 0xFF1890FF.toInt()
    const val PRIMARY_DARK = 0xFF096DD9.toInt()
    const val PRIMARY_LIGHT = 0xFF40A9FF.toInt()
    const val PRIMARY_BG = 0xFFE6F7FF.toInt()

    const val ANT_RED = 0xFFF5222D.toInt()
    const val ANT_ORANGE = 0xFFFA8C16.toInt()
    const val ANT_GOLD = 0xFFFAAD14.toInt()
    const val ANT_CYAN = 0xFF13C2C2.toInt()
    const val ANT_PURPLE = 0xFF722ED1.toInt()
    const val ANT_MAGENTA = 0xFFEB2F96.toInt()
    const val ANT_GREEN = 0xFF52C41A.toInt()
    const val ANT_INDIGO = 0xFF2F54EB.toInt()
    const val ANT_BLUE = 0xFF1890FF.toInt()
    const val ANT_GRAY = 0xFF999999.toInt()
    const val ANT_LIME = 0xFFA0D911.toInt()

    const val EXPENSE = 0xFFF5222D.toInt()
    const val INCOME = 0xFF52C41A.toInt()

    // 小清新主题色（与 colors.xml 对应）
    const val CYAN_PRIMARY = 0xFF13C2C2.toInt()
    const val PINK_PRIMARY = 0xFFFF85C0.toInt()
    const val INDIGO_PRIMARY = 0xFF2F54EB.toInt()
    const val LIME_PRIMARY = 0xFFA0D911.toInt()

    const val TEXT_MAIN = 0xFF314659.toInt()
    const val TEXT_SUB = 0xFF697B8C.toInt()
    const val TEXT_LIGHT = 0xFFA8A8A8.toInt()
    const val BG = 0xFFF5F5F5.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val BORDER = 0xFFE8E8E8.toInt()
    const val DIVIDER = 0xFFF0F0F0.toInt()

    const val TEXT_MAIN_DARK = 0xFFD9D9D9.toInt()
    const val TEXT_SUB_DARK = 0xFF8C8C8C.toInt()
    const val TEXT_LIGHT_DARK = 0xFF5A5A5A.toInt()
    const val BG_DARK = 0xFF1F1F1F.toInt()
    const val CARD_DARK = 0xFF2A2A2A.toInt()
    const val BORDER_DARK = 0xFF434343.toInt()
    const val DIVIDER_DARK = 0xFF333333.toInt()

    val CATEGORY_COLORS = intArrayOf(
        ANT_RED, ANT_ORANGE, ANT_GOLD, ANT_CYAN, ANT_PURPLE,
        ANT_MAGENTA, ANT_GREEN, ANT_INDIGO, ANT_BLUE, ANT_LIME, ANT_GRAY
    )

    /** 内置主题色（默认色板） */
    val THEME_COLORS = intArrayOf(
        ANT_BLUE, ANT_GREEN, ANT_PURPLE, ANT_ORANGE,
        CYAN_PRIMARY, PINK_PRIMARY, INDIGO_PRIMARY, LIME_PRIMARY
    )
}
