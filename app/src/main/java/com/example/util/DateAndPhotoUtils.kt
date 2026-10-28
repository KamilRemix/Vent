package com.example.util

import java.util.Calendar
import java.util.Locale
import kotlin.math.round

data class CalendarDayCell(
    val dateIso: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

object DateAndPhotoUtils {

    private val englishMonthsShort = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private val russianMonthsGenitive = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val russianMonthsShort = arrayOf(
        "янв.", "февр.", "мар.", "апр.", "мая", "июн.",
        "июл.", "авг.", "сент.", "окт.", "нояб.", "дек."
    )

    private val englishMonthsFull = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val russianMonthsNominative = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    private val englishDaysOfWeek = mapOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday",
        Calendar.SUNDAY to "Sunday"
    )

    private val russianDaysOfWeek = mapOf(
        Calendar.MONDAY to "Понедельник",
        Calendar.TUESDAY to "Вторник",
        Calendar.WEDNESDAY to "Среда",
        Calendar.THURSDAY to "Четверг",
        Calendar.FRIDAY to "Пятница",
        Calendar.SATURDAY to "Суббота",
        Calendar.SUNDAY to "Воскресенье"
    )

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return calendarToIso(cal)
    }

    fun shiftDateIso(dateIso: String, daysOffset: Int = 0, yearsOffset: Int = 0): String {
        val cal = parseIsoToCalendar(dateIso)
        if (daysOffset != 0) cal.add(Calendar.DAY_OF_MONTH, daysOffset)
        if (yearsOffset != 0) cal.add(Calendar.YEAR, yearsOffset)
        return calendarToIso(cal)
    }

    fun shiftMonthsIso(dateIso: String, monthsOffset: Int): String {
        val cal = parseIsoToCalendar(dateIso)
        cal.add(Calendar.MONTH, monthsOffset)
        return calendarToIso(cal)
    }

    fun daysBetween(fromDateIso: String, toDateIso: String): Int {
        val fromCal = parseIsoToCalendar(fromDateIso)
        val toCal = parseIsoToCalendar(toDateIso)
        val diffMillis = toCal.timeInMillis - fromCal.timeInMillis
        return round(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()
    }

    fun formatTimeHhMm(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun formatExactCountdown(
        unlockDateIso: String,
        unlockHourOfDay: Int = 9,
        unlockMinute: Int = 0,
        lang: AppLanguage = AppLocaleHolder.currentLanguage
    ): String {
        val nowMillis = System.currentTimeMillis()
        val targetCal = parseIsoToCalendar(unlockDateIso).apply {
            set(Calendar.HOUR_OF_DAY, unlockHourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, unlockMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - nowMillis
        if (diffMillis <= 0L) {
            return if (lang == AppLanguage.RU) "Готово к открытию" else "Ready to unlock now"
        }
        val totalMinutes = ((diffMillis + 59_999L) / (1000L * 60)).toInt()
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60

        return if (lang == AppLanguage.RU) {
            when {
                days > 0 && hours > 0 -> "Осталось: $days дн. $hours ч. $minutes мин."
                days > 0 -> "Осталось: $days дн. $minutes мин."
                hours > 0 -> "Осталось: $hours ч. $minutes мин."
                else -> "Осталось: $minutes мин."
            }
        } else {
            when {
                days > 0 && hours > 0 -> "$days d $hours h $minutes m remaining"
                days > 0 -> "$days d $minutes m remaining"
                hours > 0 -> "$hours h $minutes m remaining"
                else -> "$minutes min remaining"
            }
        }
    }

    fun calendarToIso(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun parseIsoToCalendar(dateIso: String): Calendar {
        val cal = Calendar.getInstance()
        val parts = dateIso.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            val d = parts[2].toIntOrNull() ?: 1
            cal.set(y, m.coerceIn(0, 11), d.coerceIn(1, 31), 12, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return cal
    }

    fun formatFullDate(dateIso: String, lang: AppLanguage = AppLocaleHolder.currentLanguage): String {
        val cal = parseIsoToCalendar(dateIso)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val year = cal.get(Calendar.YEAR)
        return if (lang == AppLanguage.RU) {
            val dow = russianDaysOfWeek[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
            "$day ${russianMonthsGenitive[monthIdx]} $year • $dow"
        } else {
            val dow = englishDaysOfWeek[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
            "${englishMonthsShort[monthIdx]} $day, $year • $dow"
        }
    }

    fun formatShortDate(dateIso: String, lang: AppLanguage = AppLocaleHolder.currentLanguage): String {
        val cal = parseIsoToCalendar(dateIso)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val year = cal.get(Calendar.YEAR)
        return if (lang == AppLanguage.RU) {
            "$day ${russianMonthsShort[monthIdx]} $year"
        } else {
            "${englishMonthsShort[monthIdx]} $day, $year"
        }
    }

    fun formatMonthYearHeader(
        year: Int,
        monthZeroBased: Int,
        lang: AppLanguage = AppLocaleHolder.currentLanguage
    ): String {
        val idx = monthZeroBased.coerceIn(0, 11)
        return if (lang == AppLanguage.RU) {
            "${russianMonthsNominative[idx]} $year"
        } else {
            "${englishMonthsFull[idx]} $year"
        }
    }

    fun getYearsAgoLabel(
        entryDateIso: String,
        targetDateIso: String,
        lang: AppLanguage = AppLocaleHolder.currentLanguage
    ): String {
        val entryYear = entryDateIso.take(4).toIntOrNull()
            ?: return if (lang == AppLanguage.RU) "Прошлые годы" else "Past year"
        val targetYear = targetDateIso.take(4).toIntOrNull()
            ?: return if (lang == AppLanguage.RU) "Прошлые годы" else "Past year"
        val diff = targetYear - entryYear
        return if (lang == AppLanguage.RU) {
            when {
                diff <= 0 -> "В этом году"
                diff == 1 -> "1 год назад"
                diff in 2..4 -> "$diff года назад"
                else -> "$diff лет назад"
            }
        } else {
            when {
                diff <= 0 -> "This year"
                diff == 1 -> "1 year ago"
                else -> "$diff years ago"
            }
        }
    }

    fun buildMonthGrid(year: Int, monthZeroBased: Int): List<CalendarDayCell> {
        val today = todayIso()
        val firstOfMonth = Calendar.getInstance().apply {
            set(year, monthZeroBased, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dow = firstOfMonth.get(Calendar.DAY_OF_WEEK)
        val leadingEmptyDays = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY

        val startCal = (firstOfMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadingEmptyDays)
        }

        val cells = ArrayList<CalendarDayCell>(42)
        for (i in 0 until 42) {
            val iso = calendarToIso(startCal)
            val isCurr = startCal.get(Calendar.MONTH) == monthZeroBased &&
                startCal.get(Calendar.YEAR) == year
            cells.add(
                CalendarDayCell(
                    dateIso = iso,
                    dayOfMonth = startCal.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = isCurr,
                    isToday = iso == today
                )
            )
            startCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }
}
