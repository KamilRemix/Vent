package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarDayCell(
    val dateIso: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

object DateAndPhotoUtils {

    private val russianMonthsGenitive = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val russianMonthsNominative = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
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
        return kotlin.math.round(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()
    }

    fun formatDaysRemainingLabel(days: Int): String {
        return when {
            days <= 0 -> "Готово к открытию"
            days % 10 == 1 && days % 100 != 11 -> "Откроется через $days день"
            days % 10 in 2..4 && days % 100 !in 12..14 -> "Откроется через $days дня"
            else -> "Откроется через $days дней"
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

    fun formatFullRussianDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val year = cal.get(Calendar.YEAR)
        val dow = russianDaysOfWeek[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        return "$day ${russianMonthsGenitive[monthIdx]} $year • $dow"
    }

    fun formatShortRussianDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val year = cal.get(Calendar.YEAR)
        return "$day ${russianMonthsGenitive[monthIdx]} $year"
    }

    fun formatMonthYearHeader(year: Int, monthZeroBased: Int): String {
        val name = russianMonthsNominative[monthZeroBased.coerceIn(0, 11)]
        return "$name $year"
    }

    fun getYearsAgoLabel(entryDateIso: String, targetDateIso: String): String {
        val entryYear = entryDateIso.take(4).toIntOrNull() ?: return "В прошлом"
        val targetYear = targetDateIso.take(4).toIntOrNull() ?: return "В прошлом"
        val diff = targetYear - entryYear
        return when {
            diff <= 0 -> "В этом году"
            diff == 1 -> "1 год назад"
            diff in 2..4 -> "$diff года назад"
            else -> "$diff лет назад"
        }
    }

    fun buildMonthGrid(year: Int, monthZeroBased: Int): List<CalendarDayCell> {
        val today = todayIso()
        val firstOfMonth = Calendar.getInstance().apply {
            set(year, monthZeroBased, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Convert Sunday=1..Saturday=7 to Monday=0..Sunday=6
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

    fun compressUriToBase64(context: Context, uri: Uri, maxDimPx: Int = 520): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
            val width = originalBitmap.width.coerceAtLeast(1)
            val height = originalBitmap.height.coerceAtLeast(1)
            val scale = minOf(maxDimPx.toFloat() / width, maxDimPx.toFloat() / height, 1f)
            val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (height * scale).toInt().coerceAtLeast(1)

            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            } else {
                originalBitmap
            }

            val output = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 72, output)
            val bytes = output.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
