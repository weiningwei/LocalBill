package com.localbill.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "localbill_prefs"
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    var darkMode: Boolean
        get() = sp.getBoolean("dark_mode", false)
        set(v) = sp.edit().putBoolean("dark_mode", v).apply()

    /** 主题色：blue / green / purple / orange / cyan / pink / indigo / lime / custom */
    var themeColor: String
        get() = sp.getString("theme_color", "blue").orEmpty()
        set(v) = sp.edit().putString("theme_color", v).apply()

    /** 自定义主题色当前值（themeColor == "custom" 时生效） */
    var customThemeColor: Int
        get() = sp.getInt("custom_theme_color", C.PRIMARY)
        set(v) = sp.edit().putInt("custom_theme_color", v).apply()

    /** 已添加的自定义主题色列表 */
    var customThemeColors: List<Int>
        get() = sp.getString("custom_theme_colors", "").orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
        set(v) = sp.edit().putString("custom_theme_colors", v.joinToString(",")).apply()

    /** 已添加的自定义主题色名称（与 customThemeColors 按索引对应，可为空串） */
    var customThemeColorNames: List<String>
        get() = sp.getString("custom_theme_color_names", "").orEmpty()
            .let { if (it.isEmpty()) emptyList() else it.split(',') }
        set(v) = sp.edit()
            .putString("custom_theme_color_names", v.joinToString(",") { it.replace(",", "，") })
            .apply()

    var activeLedgerId: Long
        get() = sp.getLong("active_ledger", 1)
        set(v) = sp.edit().putLong("active_ledger", v).apply()

    var passwordEnabled: Boolean
        get() = sp.getBoolean("password_enabled", false)
        set(v) = sp.edit().putBoolean("password_enabled", v).apply()

    var password: String
        get() = sp.getString("password", "").orEmpty()
        set(v) = sp.edit().putString("password", v).apply()

    var rememberedRecordDay: Int
        get() = sp.getInt("record_day", 0)
        set(v) = sp.edit().putInt("record_day", v).apply()

    fun rememberRecordDay(day: Int) {
        sp.edit().putInt("record_day", day).apply()
    }

    /** 自动记账开关 */
    var importEnabled: Boolean
        get() = sp.getBoolean("import_enabled", false)
        set(v) = sp.edit().putBoolean("import_enabled", v).apply()

    /** 上次导入的交易签名，用于去重 */
    var lastImportSignature: String
        get() = sp.getString("last_import_signature", "").orEmpty()
        set(v) = sp.edit().putString("last_import_signature", v).apply()

    /** 上次导入时间戳，用于防抖 */
    var lastImportAtMillis: Long
        get() = sp.getLong("last_import_at", 0)
        set(v) = sp.edit().putLong("last_import_at", v).apply()
}
