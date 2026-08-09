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
}
