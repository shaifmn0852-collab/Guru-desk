package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {
    private val inLocale = Locale("en", "IN")

    fun formatInr(amount: Double): String {
        val rounded = amount.roundToLong()
        if (rounded == 0L) return "₹0"
        val isNegative = rounded < 0
        val absVal = kotlin.math.abs(rounded)
        val formatted = formatIndianDigits(absVal)
        return if (isNegative) "-₹$formatted" else "₹$formatted"
    }

    fun formatInrCompact(amount: Double): String {
        val absVal = kotlin.math.abs(amount)
        return when {
            absVal >= 100000 -> String.format(inLocale, "₹%.1fL", amount / 100000.0)
            absVal >= 1000 -> String.format(inLocale, "₹%.1fK", amount / 1000.0)
            else -> formatInr(amount)
        }
    }

    private fun formatIndianDigits(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val lastThree = s.takeLast(3)
        val rest = s.dropLast(3)
        val sb = StringBuilder()
        var i = rest.length
        while (i > 0) {
            val start = (i - 2).coerceAtLeast(0)
            if (sb.isNotEmpty()) sb.insert(0, ",")
            sb.insert(0, rest.substring(start, i))
            i = start
        }
        return "$sb,$lastThree"
    }

    fun formatPercent(value: Double): String {
        val df = DecimalFormat("0.#", DecimalFormatSymbols(Locale.US))
        return "${df.format(value)}%"
    }

    fun formatNumber(value: Double): String {
        val df = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }
}

object DateUtils {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val monthShortFormat = SimpleDateFormat("MMM", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun todayIso(): String = isoDateFormat.format(Calendar.getInstance().time)

    fun currentMonthKey(): String = monthKeyFormat.format(Calendar.getInstance().time)

    fun daysAgoIso(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return isoDateFormat.format(cal.time)
    }

    fun shiftDateIso(dateIso: String, deltaDays: Int): String {
        val date = runCatching { isoDateFormat.parse(dateIso) }.getOrNull() ?: Calendar.getInstance().time
        val cal = Calendar.getInstance().apply { time = date }
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return isoDateFormat.format(cal.time)
    }

    fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
        val date = runCatching { monthKeyFormat.parse(monthKey) }.getOrNull() ?: Calendar.getInstance().time
        val cal = Calendar.getInstance().apply { time = date }
        cal.add(Calendar.MONTH, deltaMonths)
        return monthKeyFormat.format(cal.time)
    }

    fun recentMonthKeys(count: Int, endingAt: String = currentMonthKey()): List<String> {
        return (count - 1 downTo 0).map { offset ->
            shiftMonthKey(endingAt, -offset)
        }
    }

    fun formatMonthDisplay(monthKey: String): String {
        val date = runCatching { monthKeyFormat.parse(monthKey) }.getOrNull() ?: return monthKey
        return monthDisplayFormat.format(date)
    }

    fun formatMonthShort(monthKey: String): String {
        val date = runCatching { monthKeyFormat.parse(monthKey) }.getOrNull() ?: return monthKey
        return monthShortFormat.format(date)
    }

    fun formatDateDisplay(dateIso: String): String {
        val date = runCatching { isoDateFormat.parse(dateIso) }.getOrNull() ?: return dateIso
        return displayDateFormat.format(date)
    }

    fun monthKeyFromDate(dateIso: String): String {
        return if (dateIso.length >= 7) dateIso.substring(0, 7) else currentMonthKey()
    }
}
