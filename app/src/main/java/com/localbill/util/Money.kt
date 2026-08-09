package com.localbill.util

import java.text.DecimalFormat

object Money {
    private val fmt = DecimalFormat("#,##0.00")

    /** cents -> "1,234.56" */
    fun format(cents: Long): String {
        val yuan = cents / 100.0
        return fmt.format(yuan)
    }

    fun formatYuan(yuan: Double): String = fmt.format(yuan)

    /** "123.45" -> cents, null if invalid or empty */
    fun parseToCents(input: String): Long? {
        val s = input.trim().replace(",", "")
        if (s.isEmpty()) return null
        return try {
            val d = s.toDouble()
            if (d < 0) null else (d * 100).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
