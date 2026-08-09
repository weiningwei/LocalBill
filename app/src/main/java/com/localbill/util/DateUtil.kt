package com.localbill.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {
    private val WEEK = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    fun now(): Long = System.currentTimeMillis()

    fun today(): Int {
        val cal = Calendar.getInstance()
        return dayOf(cal)
    }

    fun monthNow(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH) + 1)
    }

    fun dayOf(cal: Calendar): Int {
        return cal.get(Calendar.YEAR) * 10000 +
                (cal.get(Calendar.MONTH) + 1) * 100 +
                cal.get(Calendar.DAY_OF_MONTH)
    }

    fun calOf(day: Int): Calendar {
        val y = day / 10000
        val m = (day / 100) % 100
        val d = day % 100
        val cal = Calendar.getInstance()
        cal.set(y, m - 1, d, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun addDays(day: Int, n: Int): Int {
        val cal = calOf(day)
        cal.add(Calendar.DAY_OF_MONTH, n)
        return dayOf(cal)
    }

    fun monthOf(day: Int): Int = day / 100

    fun yearOf(monthKey: Int): Int = monthKey / 100

    fun monthIdx(monthKey: Int): Int = monthKey % 100

    fun addMonths(monthKey: Int, n: Int): Int {
        val y = yearOf(monthKey)
        val m = monthIdx(monthKey)
        val total = y * 12 + (m - 1) + n
        val ny = Math.floorDiv(total, 12)
        val nm = Math.floorMod(total, 12) + 1
        return ny * 100 + nm
    }

    fun firstDayOfMonth(monthKey: Int): Int = monthKey * 100 + 1

    fun lastDayOfMonth(monthKey: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(yearOf(monthKey), monthIdx(monthKey) - 1, 1)
        return dayOf(cal).let {
            val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            monthKey * 100 + last
        }
    }

    fun dayTitle(day: Int): String {
        val today = today()
        return when (day) {
            today -> "今天"
            addDays(today, -1) -> "昨天"
            else -> "${monthIdx(monthOf(day))}月${day % 100}日 " + weekday(day)
        }
    }

    fun fullDate(day: Int): String {
        val cal = calOf(day)
        val sdf = SimpleDateFormat("yyyy年MM月dd日 E", Locale.CHINA)
        return sdf.format(cal.time)
    }

    fun monthTitle(monthKey: Int): String =
        "${yearOf(monthKey)}年${String.format(Locale.CHINA, "%02d", monthIdx(monthKey))}月"

    fun monthTitleShort(monthKey: Int): String =
        "${String.format(Locale.CHINA, "%02d", monthIdx(monthKey))}月"

    fun weekday(day: Int): String {
        return WEEK[calOf(day).get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun weekStart(day: Int): Int {
        val cal = calOf(day)
        val diff = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -diff)
        return dayOf(cal)
    }

    fun weekEnd(day: Int): Int {
        val cal = calOf(day)
        val diff = 6 - (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_MONTH, diff)
        return dayOf(cal)
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
        return sdf.format(Date(millis))
    }
}
