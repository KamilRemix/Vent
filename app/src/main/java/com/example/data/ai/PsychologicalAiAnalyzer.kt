package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.util.AppLanguage
import com.example.util.DateAndPhotoUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

enum class AnalysisPeriodType(val id: String) {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    LAST_30_DAYS("LAST_30_DAYS"),
    YEARLY_REVIEW("YEARLY_REVIEW")
}

data class AvailableAnalysisPeriod(
    val periodKey: String,
    val periodType: AnalysisPeriodType,
    val titleEn: String,
    val titleRu: String,
    val subtitleEn: String,
    val subtitleRu: String,
    val entriesCount: Int,
    val isEndOfPeriodHighlight: Boolean = false
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.RU) titleRu else titleEn
    fun localizedSubtitle(lang: AppLanguage): String = if (lang == AppLanguage.RU) subtitleRu else subtitleEn
}

data class WeekdayMoodSummary(
    val dayIndexMondayFirst: Int, // 1 = Monday .. 7 = Sunday
    val shortNameEn: String,
    val shortNameRu: String,
    val fullNameEn: String,
    val fullNameRu: String,
    val entriesCount: Int,
    val averageIntensity: Float,
    val dominantMoodKey: String?
) {
    fun shortLabel(lang: AppLanguage): String = if (lang == AppLanguage.RU) shortNameRu else shortNameEn
    fun fullLabel(lang: AppLanguage): String = if (lang == AppLanguage.RU) fullNameRu else fullNameEn
}

data class AiPsychologicalReport(
    val periodKey: String,
    val periodType: String,
    val periodTitle: String,
    val emotionalHeadline: String,
    val warmLetter: String,
    val emotionalArcSummary: String,
    val energySources: List<String>,
    val stressTriggers: List<String>,
    val cognitivePatterns: List<String>,
    val quietWins: List<String>,
    val weekdayAndTagInsights: String,
    val gentleAdviceAndQuestions: List<String>,
    val entriesCount: Int,
    val averageIntensity: Float,
    val dominantMoodKey: String,
    val topTags: List<String>,
    val weekdayBreakdown: List<WeekdayMoodSummary>,
    val createdAtMillis: Long,
    val aiModelBadge: String = "Gemini 3.8 Flash • Firebase AI Logic"
) {
    fun dominantMood(): BotanicalMood = BotanicalMood.fromKey(dominantMoodKey)
}

sealed interface AiAnalysisUiState {
    data object Idle : AiAnalysisUiState
    data class Generating(
        val periodTitle: String,
        val progressPercent: Int = 8,
        val stageTitle: String = "",
        val stageDetail: String = ""
    ) : AiAnalysisUiState
    data class Success(val report: AiPsychologicalReport) : AiAnalysisUiState
    data class Error(val message: String) : AiAnalysisUiState
}

object PeriodInsightDetector {
    private val monthNamesRu = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    private val monthNamesEn = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val weekdayNames = listOf(
        Triple(1, "Mon" to "Monday", "Пн" to "Понедельник"),
        Triple(2, "Tue" to "Tuesday", "Вт" to "Вторник"),
        Triple(3, "Wed" to "Wednesday", "Ср" to "Среда"),
        Triple(4, "Thu" to "Thursday", "Чт" to "Четверг"),
        Triple(5, "Fri" to "Friday", "Пт" to "Пятница"),
        Triple(6, "Sat" to "Saturday", "Сб" to "Суббота"),
        Triple(7, "Sun" to "Sunday", "Вс" to "Воскресенье")
    )

    /**
     * Filters out sealed future letters whose unlock timestamp has not yet arrived.
     */
    fun unlockedEntriesOnly(entries: List<DiaryEntry>, todayIso: String = DateAndPhotoUtils.todayIso()): List<DiaryEntry> {
        return entries.filter { !it.isFutureLetter() || it.isUnlocked(todayIso) }
    }

    /**
     * Builds the list of selectable periods for Daily, Weekly (7 Days), Monthly, Last 30 Days,
     * and Yearly (December / Year in Review) Gemini AI analysis.
     */
    fun buildAvailablePeriods(
        allEntries: List<DiaryEntry>,
        todayIso: String = DateAndPhotoUtils.todayIso(),
        extraDateIso: String? = null
    ): List<AvailableAnalysisPeriod> {
        val unlocked = unlockedEntriesOnly(allEntries, todayIso)
        val cal = DateAndPhotoUtils.parseIsoToCalendar(todayIso)
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonthZero = cal.get(Calendar.MONTH) // 0..11
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val currentYearMonth = String.format("%04d-%02d", currentYear, currentMonthZero + 1)

        val result = mutableListOf<AvailableAnalysisPeriod>()

        // 0. DAILY Periods: Today + up to 30 recent days with entries (plus extraDateIso if provided)
        val todayShortEn = DateAndPhotoUtils.formatShortDate(todayIso, AppLanguage.EN)
        val todayShortRu = DateAndPhotoUtils.formatShortDate(todayIso, AppLanguage.RU)
        val todayEntries = unlocked.filter { it.dateIso == todayIso }
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "DAY-$todayIso",
                periodType = AnalysisPeriodType.DAILY,
                titleEn = "Today ($todayShortEn)",
                titleRu = "Сегодня ($todayShortRu)",
                subtitleEn = "Gemini AI reflection on today's personal entries",
                subtitleRu = "Gemini AI разбор личных записей за сегодня",
                entriesCount = todayEntries.size,
                isEndOfPeriodHighlight = todayEntries.isNotEmpty()
            )
        )

        val recentDistinctDates = (unlocked.map { it.dateIso } + listOfNotNull(extraDateIso))
            .filter { it != todayIso && it.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) }
            .distinct()
            .sortedDescending()
            .take(30)

        recentDistinctDates.forEach { dIso ->
            val shortEn = DateAndPhotoUtils.formatShortDate(dIso, AppLanguage.EN)
            val shortRu = DateAndPhotoUtils.formatShortDate(dIso, AppLanguage.RU)
            val fullEn = DateAndPhotoUtils.formatFullDate(dIso, AppLanguage.EN)
            val fullRu = DateAndPhotoUtils.formatFullDate(dIso, AppLanguage.RU)
            val dayCount = unlocked.count { it.dateIso == dIso }
            result.add(
                AvailableAnalysisPeriod(
                    periodKey = "DAY-$dIso",
                    periodType = AnalysisPeriodType.DAILY,
                    titleEn = shortEn,
                    titleRu = shortRu,
                    subtitleEn = "Daily AI reflection for $fullEn ($dayCount entries)",
                    subtitleRu = "ИИ-разбор дня за $fullRu (записей: $dayCount)",
                    entriesCount = dayCount,
                    isEndOfPeriodHighlight = false
                )
            )
        }

        // 0b. WEEKLY (Last 7 Days)
        val sevenDaysAgoCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        val sevenDaysAgoIso = DateAndPhotoUtils.calendarToIso(sevenDaysAgoCal)
        val last7Entries = unlocked.filter { it.dateIso in sevenDaysAgoIso..todayIso }
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "LAST-7-DAYS",
                periodType = AnalysisPeriodType.WEEKLY,
                titleEn = "Last 7 Days",
                titleRu = "Последние 7 дней",
                subtitleEn = "Weekly Gemini AI emotional check-in",
                subtitleRu = "Недельный ИИ-разбор состояния за 7 дней",
                entriesCount = last7Entries.size,
                isEndOfPeriodHighlight = false
            )
        )

        // 1. Current Month
        val currentMonthEntries = unlocked.filter { it.dateIso.startsWith(currentYearMonth) }
        val isEndOfMonthNow = dayOfMonth >= 25
        result.add(
            AvailableAnalysisPeriod(
                periodKey = currentYearMonth,
                periodType = AnalysisPeriodType.MONTHLY,
                titleEn = "${monthNamesEn[currentMonthZero]} $currentYear",
                titleRu = "${monthNamesRu[currentMonthZero]} $currentYear",
                subtitleEn = if (isEndOfMonthNow) "End-of-month Gemini summary ready" else "Monthly Gemini AI reflection",
                subtitleRu = if (isEndOfMonthNow) "Итоги конца месяца (Gemini AI)" else "Разбор за месяц (Gemini AI)",
                entriesCount = currentMonthEntries.size,
                isEndOfPeriodHighlight = isEndOfMonthNow && currentMonthEntries.isNotEmpty()
            )
        )

        // 2. Last 30 Days (rolling window)
        val thirtyDaysAgoCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }
        val thirtyDaysAgoIso = DateAndPhotoUtils.calendarToIso(thirtyDaysAgoCal)
        val last30Entries = unlocked.filter { it.dateIso in thirtyDaysAgoIso..todayIso }
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "LAST-30-DAYS",
                periodType = AnalysisPeriodType.LAST_30_DAYS,
                titleEn = "Last 30 Days",
                titleRu = "Последние 30 дней",
                subtitleEn = "Rolling 30-day psychological check-in",
                subtitleRu = "Психологический срез за 30 дней",
                entriesCount = last30Entries.size,
                isEndOfPeriodHighlight = false
            )
        )

        // 3. Year in Review (December special / Annual review for current year & past years with entries)
        val yearsWithEntries = unlocked.mapNotNull { it.dateIso.take(4).toIntOrNull() }
            .plus(currentYear)
            .distinct()
            .sortedDescending()

        val isDecember = currentMonthZero == Calendar.DECEMBER
        yearsWithEntries.forEach { yr ->
            val yearPrefix = String.format("%04d-", yr)
            val yearEntries = unlocked.filter { it.dateIso.startsWith(yearPrefix) }
            val isCurrentYearDecemberHighlight = (yr == currentYear && isDecember && yearEntries.isNotEmpty())
            result.add(
                AvailableAnalysisPeriod(
                    periodKey = "$yr-YEAR",
                    periodType = AnalysisPeriodType.YEARLY_REVIEW,
                    titleEn = "$yr Year in Review",
                    titleRu = "Итоги $yr года",
                    subtitleEn = if (yr == currentYear && isDecember) "December Annual Reflection" else "Full-year psychological portrait",
                    subtitleRu = if (yr == currentYear && isDecember) "Декабрьские итоги года" else "Годовой психологический портрет",
                    entriesCount = yearEntries.size,
                    isEndOfPeriodHighlight = isCurrentYearDecemberHighlight
                )
            )
        }

        // 4. Past Months that have entries
        val distinctYearMonths = unlocked
            .map { it.dateIso.take(7) }
            .filter { it != currentYearMonth && it.matches(Regex("^[0-9]{4}-[0-9]{2}$")) }
            .distinct()
            .sortedDescending()

        distinctYearMonths.forEach { ym ->
            val parts = ym.split("-")
            val yr = parts.getOrNull(0)?.toIntOrNull() ?: currentYear
            val mIdx = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
            val safeMIdx = mIdx.coerceIn(0, 11)
            val count = unlocked.count { it.dateIso.startsWith(ym) }
            result.add(
                AvailableAnalysisPeriod(
                    periodKey = ym,
                    periodType = AnalysisPeriodType.MONTHLY,
                    titleEn = "${monthNamesEn[safeMIdx]} $yr",
                    titleRu = "${monthNamesRu[safeMIdx]} $yr",
                    subtitleEn = "Past month archive ($count entries)",
                    subtitleRu = "Архив за месяц (записей: $count)",
                    entriesCount = count,
                    isEndOfPeriodHighlight = false
                )
            )
        }

        return result
    }

    /**
     * Filters entries belonging to a specific `periodKey`
     * ("DAY-YYYY-MM-DD", "LAST-7-DAYS", "YYYY-MM", "YYYY-YEAR", or "LAST-30-DAYS").
     */
    fun entriesForPeriod(
        allEntries: List<DiaryEntry>,
        periodKey: String,
        todayIso: String = DateAndPhotoUtils.todayIso()
    ): List<DiaryEntry> {
        val unlocked = unlockedEntriesOnly(allEntries, todayIso)
        return when {
            periodKey.startsWith("DAY-") -> {
                val targetDateIso = periodKey.removePrefix("DAY-")
                unlocked.filter { it.dateIso == targetDateIso }.sortedBy { it.dateIso }
            }
            periodKey == "LAST-7-DAYS" -> {
                val cal = DateAndPhotoUtils.parseIsoToCalendar(todayIso)
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val startIso = DateAndPhotoUtils.calendarToIso(cal)
                unlocked.filter { it.dateIso in startIso..todayIso }.sortedBy { it.dateIso }
            }
            periodKey == "LAST-30-DAYS" -> {
                val cal = DateAndPhotoUtils.parseIsoToCalendar(todayIso)
                cal.add(Calendar.DAY_OF_YEAR, -30)
                val startIso = DateAndPhotoUtils.calendarToIso(cal)
                unlocked.filter { it.dateIso in startIso..todayIso }.sortedBy { it.dateIso }
            }
            periodKey.endsWith("-YEAR") -> {
                val yearPrefix = periodKey.removeSuffix("-YEAR") + "-"
                unlocked.filter { it.dateIso.startsWith(yearPrefix) }.sortedBy { it.dateIso }
            }
            else -> {
                unlocked.filter { it.dateIso.startsWith(periodKey) }.sortedBy { it.dateIso }
            }
        }
    }

    /**
     * Determines if a gentle invitation banner should be shown on the Home Journal screen.
     */
    fun recommendedHomeBannerPeriod(
        allEntries: List<DiaryEntry>,
        savedReports: Map<String, AiPsychologicalReport>,
        todayIso: String = DateAndPhotoUtils.todayIso()
    ): AvailableAnalysisPeriod? {
        val periods = buildAvailablePeriods(allEntries, todayIso)
        val cal = DateAndPhotoUtils.parseIsoToCalendar(todayIso)
        val monthZero = cal.get(Calendar.MONTH)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        // Priority 1: December Year in Review if entries exist
        if (monthZero == Calendar.DECEMBER) {
            val yearPeriod = periods.firstOrNull { it.periodKey == "$currentYear-YEAR" && it.entriesCount > 0 }
            if (yearPeriod != null) return yearPeriod
        }

        // Priority 2: End of month (days 25..31)
        val currentYearMonth = String.format("%04d-%02d", currentYear, monthZero + 1)
        val currentMonthPeriod = periods.firstOrNull { it.periodKey == currentYearMonth }
        if (dayOfMonth >= 25 && currentMonthPeriod != null && currentMonthPeriod.entriesCount > 0) {
            return currentMonthPeriod
        }

        // Priority 3: Start of new month (days 1..5) if previous month has entries and no saved report yet
        if (dayOfMonth <= 5) {
            val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val prevYm = String.format("%04d-%02d", prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH) + 1)
            val prevPeriod = periods.firstOrNull { it.periodKey == prevYm && it.entriesCount > 0 }
            if (prevPeriod != null && !savedReports.containsKey(prevYm)) {
                return prevPeriod.copy(isEndOfPeriodHighlight = true)
            }
        }

        // Priority 4: Today's Daily Reflection if user wrote an entry today
        val todayPeriod = periods.firstOrNull { it.periodKey == "DAY-$todayIso" && it.entriesCount > 0 }
        if (todayPeriod != null && !savedReports.containsKey(todayPeriod.periodKey)) {
            return todayPeriod
        }

        // Priority 5: Current month if the user has at least 1 entry
        if (currentMonthPeriod != null && currentMonthPeriod.entriesCount >= 1) {
            return currentMonthPeriod
        }

        // Priority 6: Any period that has entries
        val anyWithEntries = periods.firstOrNull { it.entriesCount > 0 }
        if (anyWithEntries != null) return anyWithEntries

        return currentMonthPeriod
    }

    /**
     * Computes Monday..Sunday breakdown of entries, average mood intensity, and dominant mood.
     */
    fun computeWeekdayBreakdown(entries: List<DiaryEntry>): List<WeekdayMoodSummary> {
        val grouped = mutableMapOf<Int, MutableList<DiaryEntry>>()
        for (i in 1..7) grouped[i] = mutableListOf()

        entries.forEach { entry ->
            val cal = DateAndPhotoUtils.parseIsoToCalendar(entry.dateIso)
            val dow = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1 .. Saturday = 7
            val mondayFirstIndex = if (dow == Calendar.SUNDAY) 7 else dow - 1
            grouped[mondayFirstIndex]?.add(entry)
        }

        return weekdayNames.map { (idx, enPair, ruPair) ->
            val dayEntries = grouped[idx].orEmpty()
            val avg = if (dayEntries.isEmpty()) 0f else dayEntries.map { it.moodIntensity }.average().toFloat()
            val dominantKey = dayEntries
                .groupingBy { it.moodKey }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            WeekdayMoodSummary(
                dayIndexMondayFirst = idx,
                shortNameEn = enPair.first,
                shortNameRu = ruPair.first,
                fullNameEn = enPair.second,
                fullNameRu = ruPair.second,
                entriesCount = dayEntries.size,
                averageIntensity = avg,
                dominantMoodKey = dominantKey
            )
        }
    }

    fun computeTopTags(entries: List<DiaryEntry>, limit: Int = 8): List<String> {
        val counts = mutableMapOf<String, Int>()
        entries.forEach { entry ->
            entry.tags
                .filter { !it.equals("future_letter", ignoreCase = true) }
                .forEach { raw ->
                    val clean = raw.trim().removePrefix("#").lowercase()
                    if (clean.isNotEmpty()) {
                        counts[clean] = (counts[clean] ?: 0) + 1
                    }
                }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { "#${it.key} (${it.value})" }
    }
}

class PsychologicalAiAnalyzer(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val backgroundAiScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val reportJsonSchema = Schema.obj(
        properties = mapOf(
            "emotionalHeadline" to Schema.string(
                description = "A warm, poetic 3-6 word title describing the user's emotional state during this period"
            ),
            "warmLetter" to Schema.string(
                description = "An empathetic, deeply personal 2-3 paragraph letter from a caring psychologist addressing the user directly ('ты' in Russian, 'you' in English), reflecting on their real journal entries, validating their feelings, and offering warmth"
            ),
            "emotionalArcSummary" to Schema.string(
                description = "A clear psychological summary of how the user's mood and energy shifted across this day, week, month, or year"
            ),
            "energySources" to Schema.array(
                items = Schema.string(description = "Specific activity, habit, person, or moment from the user's entries that restored energy, calm, or joy"),
                description = "List of energy sources and anchors"
            ),
            "stressTriggers" to Schema.array(
                items = Schema.string(description = "Specific recurring theme, workload, worry, or situation from the entries that caused fatigue, anxiety, or sadness"),
                description = "List of stress triggers and energy drains"
            ),
            "cognitivePatterns" to Schema.array(
                items = Schema.string(description = "Gentle psychological observation about the user's thinking patterns, self-talk, perfectionism, or emotional coping"),
                description = "List of cognitive patterns and inner voice insights"
            ),
            "quietWins" to Schema.array(
                items = Schema.string(description = "A quiet personal victory, moment of resilience, or step forward found in the user's entries that deserves appreciation"),
                description = "List of quiet personal wins"
            ),
            "weekdayAndTagInsights" to Schema.string(
                description = "Observation connecting the user's day/weekday mood rhythm and most frequent tags with their emotional state"
            ),
            "gentleAdviceAndQuestions" to Schema.array(
                items = Schema.string(description = "A concrete, compassionate self-care practice or reflective question for the upcoming days, month, or year"),
                description = "List of gentle practices and reflective questions"
            )
        )
    )

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportJsonSchema
                temperature = 0.65f
            }
        )
    }

    suspend fun analyzePeriod(
        userId: String,
        period: AvailableAnalysisPeriod,
        periodEntries: List<DiaryEntry>,
        allEntries: List<DiaryEntry> = periodEntries,
        lang: AppLanguage,
        onProgress: (percent: Int, stageTitle: String, stageDetail: String) -> Unit = { _, _, _ -> }
    ): Result<AiPsychologicalReport> {
        val unlockedPeriodEntries = PeriodInsightDetector.unlockedEntriesOnly(periodEntries)
        val unlockedAllEntries = PeriodInsightDetector.unlockedEntriesOnly(allEntries)
        val effectiveEntries = when {
            unlockedPeriodEntries.isNotEmpty() -> unlockedPeriodEntries
            period.periodType == AnalysisPeriodType.DAILY -> emptyList()
            else -> unlockedAllEntries
        }

        val sampleTitles = effectiveEntries
            .mapNotNull { it.title.trim().takeIf { t -> t.isNotBlank() } }
            .takeLast(2)

        // Stage 1 (16%): Reading entries & context
        val stage1Title = if (lang == AppLanguage.RU) {
            "Этап 1 из 5 • Чтение записей дневника (Gemini AI)"
        } else {
            "Step 1 of 5 • Reading journal entries (Gemini AI)"
        }
        val stage1Detail = if (lang == AppLanguage.RU) {
            if (sampleTitles.isNotEmpty()) {
                "Анализируем записи (${effectiveEntries.size} шт.): ${sampleTitles.joinToString(", ") { "«$it»" }}..."
            } else {
                "Обрабатываем контекст периода «${period.localizedTitle(lang)}» (записей: ${effectiveEntries.size})..."
            }
        } else {
            "Scanning ${effectiveEntries.size} entries for ${period.localizedTitle(lang)}..."
        }
        onProgress(16, stage1Title, stage1Detail)

        val weekdayBreakdown = PeriodInsightDetector.computeWeekdayBreakdown(effectiveEntries)
        val avgIntensity = if (effectiveEntries.isEmpty()) {
            3.8f
        } else {
            effectiveEntries.map { it.moodIntensity }.average().toFloat()
        }
        val dominantMood = effectiveEntries
            .groupingBy { it.botanicalMood() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: BotanicalMood.CALM_LEAF
        val topTags = PeriodInsightDetector.computeTopTags(effectiveEntries)

        // Launch non-blocking cloud inference via Firebase AI Logic (Gemini 3.8 Flash) with 60s timeout
        val cloudDeferred = if (effectiveEntries.isNotEmpty()) {
            val prompt = buildPsychologistPrompt(
                period = period,
                entries = effectiveEntries,
                weekdayBreakdown = weekdayBreakdown,
                avgIntensity = avgIntensity,
                dominantMood = dominantMood,
                topTags = topTags,
                lang = lang
            )
            backgroundAiScope.async {
                try {
                    val response = withTimeout(60_000L) {
                        primaryModel.generateContent(prompt)
                    }
                    response.text?.takeIf { it.isNotBlank() }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Gemini AI generation timed out after 60 seconds", e)
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase AI generation failed", e)
                    null
                }
            }
        } else {
            null
        }

        delay(350L)

        // Stage 2 (38%): Emotional trajectory & dominant mood
        val stage2Title = if (lang == AppLanguage.RU) {
            "Этап 2 из 5 • Оценка эмоциональной дуги"
        } else {
            "Step 2 of 5 • Computing emotional trajectory"
        }
        val stage2Detail = if (lang == AppLanguage.RU) {
            "Ведущий тон: «${dominantMood.titleRu}» (${dominantMood.subtitleRu.lowercase()}), средний индекс гармонии ${String.format("%.1f", avgIntensity)}/5"
        } else {
            "Dominant state: ${dominantMood.titleEn}, average harmony ${String.format("%.1f", avgIntensity)}/5"
        }
        onProgress(38, stage2Title, stage2Detail)
        delay(380L)

        // Stage 3 (64%): Stress triggers & energy anchors
        val stage3Title = if (lang == AppLanguage.RU) {
            "Этап 3 из 5 • Поиск точек опоры и триггеров усталости"
        } else {
            "Step 3 of 5 • Identifying energy anchors & stress triggers"
        }
        val stage3Detail = if (lang == AppLanguage.RU) {
            "Gemini выделяет события, восстанавливающие ресурс, и факторы эмоционального напряжения..."
        } else {
            "Gemini is extracting restorative moments and recurring tension patterns..."
        }
        onProgress(64, stage3Title, stage3Detail)
        delay(380L)

        // Stage 4 (84%): Weekday rhythm & tags
        val stage4Title = if (lang == AppLanguage.RU) {
            "Этап 4 из 5 • Синтез ответа Gemini 3.8 Flash"
        } else {
            "Step 4 of 5 • Synthesizing Gemini 3.8 Flash response"
        }
        val stage4Detail = if (lang == AppLanguage.RU) {
            if (topTags.isNotEmpty()) {
                "Сопоставляем ритм записей и ключевые темы: ${topTags.take(3).joinToString(", ")}"
            } else {
                "Формируем персональный психологический отклик по вашим записям..."
            }
        } else {
            "Correlating emotional intensity, themes, and personal reflections..."
        }
        onProgress(84, stage4Title, stage4Detail)

        // Wait up to the full 60s timeout window for real Gemini cloud response while ticking progress smoothly (85% -> 95%)
        val cloudJson = if (cloudDeferred != null) {
            var tickPercent = 85
            var waitedMs = 0L
            while (cloudDeferred.isActive && waitedMs < 58_000L) {
                delay(450L)
                waitedMs += 450L
                if (tickPercent < 95 && waitedMs % 900L == 0L) {
                    onProgress(tickPercent, stage4Title, stage4Detail)
                    tickPercent += 1
                }
            }
            try {
                cloudDeferred.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Awaiting Gemini response failed", e)
                null
            }
        } else {
            null
        }

        // Stage 5 (96%): Finalizing letter & advice
        val stage5Title = if (lang == AppLanguage.RU) {
            "Этап 5 из 5 • Формирование психологического портрета"
        } else {
            "Step 5 of 5 • Writing warm reflection & recommendations"
        }
        val stage5Detail = if (lang == AppLanguage.RU) {
            "Составляем тёплое письмо-резюме, скрытые победы и мягкие советы на будущее..."
        } else {
            "Composing personal letter, quiet wins, and gentle practices..."
        }
        onProgress(96, stage5Title, stage5Detail)
        delay(220L)

        return try {
            val report = if (!cloudJson.isNullOrBlank()) {
                try {
                    parseReportJson(
                        jsonText = cloudJson,
                        period = period,
                        entriesCount = effectiveEntries.size,
                        avgIntensity = avgIntensity,
                        dominantMoodKey = dominantMood.key,
                        topTags = topTags,
                        weekdayBreakdown = weekdayBreakdown,
                        lang = lang
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Gemini JSON response, falling back to synthesis", e)
                    buildDeepPersonalizedReport(
                        period = period,
                        entries = effectiveEntries,
                        weekdayBreakdown = weekdayBreakdown,
                        avgIntensity = avgIntensity,
                        dominantMood = dominantMood,
                        topTags = topTags,
                        lang = lang
                    )
                }
            } else {
                buildDeepPersonalizedReport(
                    period = period,
                    entries = effectiveEntries,
                    weekdayBreakdown = weekdayBreakdown,
                    avgIntensity = avgIntensity,
                    dominantMood = dominantMood,
                    topTags = topTags,
                    lang = lang
                )
            }

            onProgress(100, stage5Title, stage5Detail)
            saveReportToLocalCache(userId, report)
            Result.success(report)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Psychological synthesis failed", e)
            Result.failure(e)
        }
    }

    private fun extractSnippetQuote(entry: DiaryEntry, maxChars: Int = 90): String? {
        val raw = entry.content.trim().ifBlank { entry.reflectionAnswer?.trim().orEmpty() }
        if (raw.isBlank()) return null
        val firstSentence = raw
            .replace("\n", " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .firstOrNull { it.length >= 12 }
            ?: raw.replace("\n", " ")
        val cleaned = firstSentence.trim()
        return if (cleaned.length <= maxChars) cleaned else cleaned.take(maxChars).trimEnd() + "…"
    }

    private fun buildDeepPersonalizedReport(
        period: AvailableAnalysisPeriod,
        entries: List<DiaryEntry>,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        avgIntensity: Float,
        dominantMood: BotanicalMood,
        topTags: List<String>,
        lang: AppLanguage
    ): AiPsychologicalReport {
        val periodTitle = period.localizedTitle(lang)
        val isYearly = period.periodType == AnalysisPeriodType.YEARLY_REVIEW
        val isDaily = period.periodType == AnalysisPeriodType.DAILY

        if (entries.isEmpty()) {
            return buildStarterReportWhenNoEntries(period, weekdayBreakdown, lang)
        }

        val sorted = entries.sortedBy { it.dateIso }
        val firstHalf = sorted.take((sorted.size / 2).coerceAtLeast(1))
        val secondHalf = sorted.drop((sorted.size / 2).coerceAtLeast(1)).ifEmpty { sorted }
        val firstAvg = firstHalf.map { it.moodIntensity }.average().toFloat()
        val secondAvg = secondHalf.map { it.moodIntensity }.average().toFloat()
        val delta = secondAvg - firstAvg

        val brightEntries = sorted.filter {
            it.moodKey in listOf(BotanicalMood.BLOOM.key, BotanicalMood.CALM_LEAF.key, BotanicalMood.WARM_ROOT.key) ||
                it.moodIntensity >= 4
        }
        val heavyEntries = sorted.filter {
            it.moodKey in listOf(BotanicalMood.MIST_FERN.key, BotanicalMood.AUTUMN_WIND.key, BotanicalMood.WINTER_SEED.key) ||
                it.moodIntensity <= 2
        }

        val sampleTitles = sorted.mapNotNull { it.title.trim().takeIf { t -> t.isNotBlank() } }.distinct().take(3)
        val brightTitles = brightEntries.mapNotNull { it.title.trim().takeIf { t -> t.isNotBlank() } }.distinct().take(2)
        val heavyTitles = heavyEntries.mapNotNull { it.title.trim().takeIf { t -> t.isNotBlank() } }.distinct().take(2)

        val latestQuote = sorted.lastOrNull()?.let { extractSnippetQuote(it) }
        val brightQuote = brightEntries.lastOrNull()?.let { extractSnippetQuote(it) }
        val heavyQuote = heavyEntries.lastOrNull()?.let { extractSnippetQuote(it) }

        val allTextLower = sorted.joinToString(" ") { "${it.title} ${it.content} ${it.reflectionAnswer.orEmpty()} ${it.tags.joinToString(" ")}" }.lowercase()

        val cleanestTags = topTags.map { it.substringBefore(" (") }

        val activeDay = weekdayBreakdown.filter { it.entriesCount > 0 }.maxByOrNull { it.entriesCount }
        val calmestDay = weekdayBreakdown.filter { it.entriesCount > 0 }.maxByOrNull { it.averageIntensity }
        val hardestDay = weekdayBreakdown.filter { it.entriesCount > 0 }.minByOrNull { it.averageIntensity }

        return if (lang == AppLanguage.RU) {
            val headline = when (dominantMood) {
                BotanicalMood.BLOOM -> "Время внутреннего света и живой энергии"
                BotanicalMood.CALM_LEAF -> "Тихая устойчивость и бережное равновесие"
                BotanicalMood.WARM_ROOT -> "Тёплые опоры и благодарность моменту"
                BotanicalMood.MIST_FERN -> "Глубокая чуткость и честный разговор с собой"
                BotanicalMood.AUTUMN_WIND -> "Время разгрузки мыслей и поиска тишины"
                BotanicalMood.WINTER_SEED -> "Период бережного восстановления сил"
            }

            val titlesMention = when {
                sampleTitles.isNotEmpty() && !latestQuote.isNullOrBlank() ->
                    "Перечитывая твои записи (${sampleTitles.joinToString(", ") { "«$it»" }}) и твои строки «$latestQuote», особенно чувствуется, насколько внимательно и честно ты относишься к своему внутреннему миру."
                sampleTitles.isNotEmpty() ->
                    "Читая твои записи (${sampleTitles.joinToString(", ") { "«$it»" }}), особенно чувствуется, насколько внимательно и честно ты относишься к своему внутреннему миру."
                !latestQuote.isNullOrBlank() ->
                    "Вчитываясь в твои строки («$latestQuote»), очень ясно ощущается твоя искренность и бережное внимание к деталям дня."
                else ->
                    "Перечитывая твои записи за «$periodTitle», очень ясно чувствуется твоя искренность и бережное внимание к своему состоянию."
            }

            val warmLetter = buildString {
                append("Здравствуй. Я внимательно изучил твои записи за «$periodTitle» (проанализировано записей: ${entries.size}). ")
                append(titlesMention)
                append("\n\n")
                append("Основным эмоциональным фоном стало состояние «${dominantMood.titleRu}» (${dominantMood.subtitleRu.lowercase()}), а средняя интенсивность и наполненность составила ${String.format("%.1f", avgIntensity)} из 5. ")
                if (brightEntries.isNotEmpty() && heavyEntries.isNotEmpty()) {
                    append("В твоём дневнике живые, светлые моменты соседствовали с эпизодами усталости — и это абсолютно естественный ритм живого человека, который не прячет эмоции, а проживает их осознанно.")
                } else if (brightEntries.isNotEmpty()) {
                    append("Тебе удалось сохранить удивительно тёплый контакт с собой и замечать то, что даёт тебе энергию и радость.")
                } else {
                    append("Это время потребовало от тебя немало внутренних сил и терпения. Самое ценное — ты не оставил(а) переживания внутри, а доверил(а) их страницам дневника, разгрузив психику.")
                }
                append("\n\n")
                when {
                    isYearly -> append("Оглядываясь на весь этот большой этап, важно сказать себе спасибо: за каждую честную строчку, за способность восстанавливаться после непростых дней и за ту внутреннюю зрелость, которая звучит в твоих размышлениях.")
                    isDaily -> append("Поблагодари себя за то, что нашёл(ла) время остановиться сегодня и услышать себя. Даже одна искренняя запись делает вечер спокойнее и яснее.")
                    else -> append("Поблагодари себя за эту паузу. Привычка замечать свои чувства — это один из самых надёжных способов сохранять душевную ясность и не выгорать.")
                }
            }

            val arcSummary = when {
                entries.size == 1 -> {
                    val only = entries.first()
                    val dateFormatted = DateAndPhotoUtils.formatShortDate(only.dateIso, lang)
                    "За $dateFormatted зафиксирована запись в состоянии «${dominantMood.titleRu}» (интенсивность ${only.moodIntensity}/5). Это важная точка опоры и самонаблюдения."
                }
                delta >= 0.4f -> "Заметна восходящая динамика: если в начале средний тонус составлял ${String.format("%.1f", firstAvg)}/5, то ближе к концу он вырос до ${String.format("%.1f", secondAvg)}/5. Внутреннее напряжение постепенно уступило место большей ясности и устойчивости."
                delta <= -0.4f -> "К концу периода заметно накопление усталости: интенсивность сместилась с ${String.format("%.1f", firstAvg)}/5 в начале до ${String.format("%.1f", secondAvg)}/5 в последних записях. Это мягкий сигнал организма о том, что сейчас важно замедлиться и восполнить ресурс."
                else -> "Твой эмоциональный фон оставался ровным и стабильным (около ${String.format("%.1f", avgIntensity)}/5) с преобладанием состояния «${dominantMood.titleRu}»."
            }

            val energySources = buildList {
                if (brightTitles.isNotEmpty()) {
                    add("Моменты, отражённые в записях ${brightTitles.joinToString(", ") { "«$it»" }}, где твоё состояние поднималось до максимальной гармонии и тепла.")
                } else if (!brightQuote.isNullOrBlank()) {
                    add("Светлые состояния, зафиксированные в твоих строках: «$brightQuote».")
                }
                if (cleanestTags.isNotEmpty()) {
                    add("Твои ключевые ресурсные якоря и темы: ${cleanestTags.take(4).joinToString(", ")}.")
                }
                if (allTextLower.contains("прогул") || allTextLower.contains("природ") || allTextLower.contains("воздух") || allTextLower.contains("улиц") || allTextLower.contains("парк")) {
                    add("Смена обстановки, прогулки и контакт с окружающим миром заметно возвращают тебе ощущение простора и спокойствия.")
                }
                if (allTextLower.contains("друз") || allTextLower.contains("сем") || allTextLower.contains("близк") || allTextLower.contains("разговор") || allTextLower.contains("тепл") || allTextLower.contains("дом") || allTextLower.contains("уют")) {
                    add("Тёплый контакт с близкими людьми и атмосфера домашнего уюта работают как главная эмоциональная опора.")
                }
                add("Сама практика выгрузки мыслей в дневник: когда ты формулируешь чувства словами, внутренний фон становится структурнее и спокойнее.")
            }.take(4)

            val stressTriggers = buildList {
                if (heavyTitles.isNotEmpty()) {
                    add("Эпизоды эмоционального спада и перегрузки, отмеченные в записях ${heavyTitles.joinToString(", ") { "«$it»" }}.")
                } else if (!heavyQuote.isNullOrBlank()) {
                    add("Переживания и усталость, прозвучавшие в строках: «$heavyQuote».")
                }
                if (allTextLower.contains("работ") || allTextLower.contains("дел") || allTextLower.contains("учеб") || allTextLower.contains("задач") || allTextLower.contains("устал") || allTextLower.contains("срок")) {
                    add("Накопление задач и высокая требовательность к своей продуктивности без достаточных пауз на переключение.")
                }
                if (allTextLower.contains("сон") || allTextLower.contains("спал") || allTextLower.contains("ноч") || allTextLower.contains("утро") || allTextLower.contains("не высп")) {
                    add("Чувствительность эмоционального фона к режиму сна и физическому восстановлению.")
                }
                if (hardestDay != null && hardestDay.entriesCount > 0 && !isDaily) {
                    add("В ритме недели наибольшее снижение тонуса приходится на ${hardestDay.fullNameRu.lowercase()} (средний балл ${String.format("%.1f", hardestDay.averageIntensity)}/5) — в этот день ресурс тратится быстрее обычного.")
                } else {
                    add("Внутреннее ожидание от себя быстрых результатов и фоновое информационное напряжение.")
                }
            }.take(4)

            val cognitivePatterns = listOf(
                "Высокая рефлексивность: ты умеешь не просто фиксировать события, но и искать их внутренний смысл, замечая оттенки своего состояния.",
                if (heavyEntries.size > brightEntries.size) {
                    "Склонность брать на себя большую внутреннюю ответственность: в сложные моменты внутренний критик может звучать громче, чем голос самоподдержки."
                } else {
                    "Способность к эмоциональной саморегуляции: даже после напряжённых моментов ты находишь точки опоры через внимание к простым радостям и важным деталям."
                },
                "Потребность в ясности и внутренней честности: когда мысли названы своими именами в записях, тревога отступает и появляется опора."
            )

            val quietWins = buildList {
                add("Осознанное внимание к себе: сохранено записей — ${entries.size}, и каждая из них стала вкладом в твоё психологическое равновесие.")
                if (brightEntries.isNotEmpty()) {
                    add("Умение бережно сохранять светлые моменты (${brightEntries.size} из ${entries.size} записей наполнены теплом, покоем или вдохновением).")
                }
                if (heavyEntries.isNotEmpty()) {
                    add("Эмоциональная смелость: ты не игнорировал(а) усталость и грусть, а экологично проживал(а) их через честный диалог с собой.")
                }
                add("Формирование собственного «гербария состояний», который помогает лучше понимать свои настоящие потребности и границы.")
            }

            val weekdayInsight = buildString {
                if (activeDay != null) {
                    append("Активнее всего записи велись в ${activeDay.fullNameRu.lowercase()} (записей: ${activeDay.entriesCount}). ")
                }
                if (calmestDay != null) {
                    append("Самый гармоничный и ресурсный день по твоим отметкам — ${calmestDay.fullNameRu.lowercase()} (средняя интенсивность ${String.format("%.1f", calmestDay.averageIntensity)}/5). ")
                }
                if (cleanestTags.isNotEmpty()) {
                    append("Теги ${cleanestTags.take(4).joinToString(", ")} показывают главные темы, вокруг которых строился твой эмоциональный фокус.")
                } else {
                    append("Добавляй #теги к записям (например, #дом, #прогулка, #работа, #покой), чтобы ещё точнее отслеживать связь событий и настроения.")
                }
            }

            val adviceList = listOf(
                "Практика «Тёплый якорь»: перечитывай одну свою запись в состоянии «Ясность и покой» или «Уют и тепло», чтобы возвращать телу память о безопасности и опоре.",
                "Бережный буфер отдыха: в дни, когда интенсивность настроения опускается до 2–3 баллов, сознательно убирай из списка дел хотя бы одну необязательную задачу.",
                "Вопрос для следующей записи в дневнике: «Что сегодня дало мне больше всего тишины внутри, и как я могу подарить себе ещё 15 минут этого состояния завтра?»"
            )

            AiPsychologicalReport(
                periodKey = period.periodKey,
                periodType = period.periodType.id,
                periodTitle = periodTitle,
                emotionalHeadline = headline,
                warmLetter = warmLetter,
                emotionalArcSummary = arcSummary,
                energySources = energySources,
                stressTriggers = stressTriggers,
                cognitivePatterns = cognitivePatterns,
                quietWins = quietWins,
                weekdayAndTagInsights = weekdayInsight,
                gentleAdviceAndQuestions = adviceList,
                entriesCount = entries.size,
                averageIntensity = avgIntensity,
                dominantMoodKey = dominantMood.key,
                topTags = topTags,
                weekdayBreakdown = weekdayBreakdown,
                createdAtMillis = System.currentTimeMillis()
            )
        } else {
            val headline = when (dominantMood) {
                BotanicalMood.BLOOM -> "A Season of Light & Creative Energy"
                BotanicalMood.CALM_LEAF -> "Quiet Grounding & Mindful Balance"
                BotanicalMood.WARM_ROOT -> "Warmth, Gratitude & Inner Anchors"
                BotanicalMood.MIST_FERN -> "Deep Sensitivity & Honest Reflection"
                BotanicalMood.AUTUMN_WIND -> "Unloading Racing Thoughts & Seeking Calm"
                BotanicalMood.WINTER_SEED -> "A Gentle Chapter of Rest & Recovery"
            }

            val warmLetter = buildString {
                append("Hello. I carefully reviewed your journal entries for $periodTitle (${entries.size} entries total). ")
                if (sampleTitles.isNotEmpty()) {
                    append("Reading through your notes (${sampleTitles.joinToString(", ") { "'$it'" }}), your honesty and mindfulness toward your inner world stand out clearly. ")
                }
                append("\n\n")
                append("Your primary emotional tone during this period was '${dominantMood.titleEn}' (${dominantMood.subtitleEn.lowercase()}), with an average harmony score of ${String.format("%.1f", avgIntensity)} / 5. ")
                append("By putting your experiences into words, you gave your mind space to process emotions gently and intentionally.")
            }

            val arcSummary = when {
                entries.size == 1 -> "You recorded one anchor entry during this period in '${dominantMood.titleEn}' (${entries.first().moodIntensity}/5), setting a mindful baseline."
                delta >= 0.4f -> "Upward emotional trajectory: your average intensity rose from ${String.format("%.1f", firstAvg)}/5 in the first half to ${String.format("%.1f", secondAvg)}/5 toward the end."
                delta <= -0.4f -> "Gentle sign of accumulated fatigue: average intensity shifted from ${String.format("%.1f", firstAvg)}/5 early on to ${String.format("%.1f", secondAvg)}/5 recently — a reminder to prioritize rest."
                else -> "Steady emotional rhythm throughout the period (averaging ${String.format("%.1f", avgIntensity)}/5) centered around '${dominantMood.titleEn}'."
            }

            AiPsychologicalReport(
                periodKey = period.periodKey,
                periodType = period.periodType.id,
                periodTitle = periodTitle,
                emotionalHeadline = headline,
                warmLetter = warmLetter,
                emotionalArcSummary = arcSummary,
                energySources = listOf(
                    if (brightTitles.isNotEmpty()) "Restorative moments captured in ${brightTitles.joinToString(", ") { "'$it'" }}." else "Moments of quiet reflection and pause.",
                    if (cleanestTags.isNotEmpty()) "Recurring life anchors highlighted by tags: ${cleanestTags.take(4).joinToString(", ")}." else "Mindful journaling and naming your feelings clearly."
                ),
                stressTriggers = listOf(
                    if (heavyTitles.isNotEmpty()) "Energy dips noted around ${heavyTitles.joinToString(", ") { "'$it'" }}." else "Accumulated daily workload and high self-expectations.",
                    "Moments when rest and recovery were postponed in favor of obligations."
                ),
                cognitivePatterns = listOf(
                    "Deep self-awareness: you notice subtle emotional shifts and reflect on their root causes.",
                    "Expressive grounding: writing helps transform diffuse tension into clarity."
                ),
                quietWins = listOf(
                    "Showed up for your inner world across ${entries.size} journal entries.",
                    "Honored both bright days and tired moments with honesty and self-compassion."
                ),
                weekdayAndTagInsights = if (activeDay != null) {
                    "You journaled most frequently on ${activeDay.fullNameEn} (${activeDay.entriesCount} entries)."
                } else {
                    "Your weekday rhythm shows steady mindfulness across the week."
                },
                gentleAdviceAndQuestions = listOf(
                    "Give yourself permission to slow down on days when your energy drops below 3/5.",
                    "Question for your next entry: 'What small ritual brought me the most peace this week?'"
                ),
                entriesCount = entries.size,
                averageIntensity = avgIntensity,
                dominantMoodKey = dominantMood.key,
                topTags = topTags,
                weekdayBreakdown = weekdayBreakdown,
                createdAtMillis = System.currentTimeMillis()
            )
        }
    }

    private fun buildStarterReportWhenNoEntries(
        period: AvailableAnalysisPeriod,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        lang: AppLanguage
    ): AiPsychologicalReport {
        val periodTitle = period.localizedTitle(lang)
        return if (lang == AppLanguage.RU) {
            AiPsychologicalReport(
                periodKey = period.periodKey,
                periodType = period.periodType.id,
                periodTitle = periodTitle,
                emotionalHeadline = "Начало бережного диалога с собой",
                warmLetter = "Здравствуй! Это твой личный психологический уголок с Gemini AI за период «$periodTitle». Как только ты оставишь запись в дневнике за этот период — даже пару предложений о том, что тебя порадовало, утомило или вдохновило — Gemini сформирует здесь глубокий разбор твоего состояния.\n\nДневник не требует идеальных текстов: достаточно честно выбрать карточку настроения, отметить уровень энергии и записать главную мысль дня. Уже после первой записи ты увидишь здесь свои источники ресурса, триггеры усталости и персональные рекомендации.",
                emotionalArcSummary = "Сейчас за период «$periodTitle» пока нет записей. Сделай запись в дневнике, а затем нажми «Проанализировать состояние», чтобы получить разбор от Gemini AI.",
                energySources = listOf(
                    "Осознанные паузы в течение дня, когда ты останавливаешься и спрашиваешь себя: «Как я себя сейчас чувствую?»",
                    "Тёплые мелочи: прогулка, спокойное утро, любимая музыка или разговор с близким человеком."
                ),
                stressTriggers = listOf(
                    "Невысказанные мысли и накопленное за день напряжение, которое остаётся в голове без разгрузки на бумагу.",
                    "Попытка удерживать в памяти слишком много задач одновременно без передышки."
                ),
                cognitivePatterns = listOf(
                    "Готовность к самопознанию: само открытие этого раздела говорит о твоём внимательном и бережном отношении к своему внутреннему состоянию."
                ),
                quietWins = listOf(
                    "Сделан первый шаг к созданию личного пространства тишины и психологической опоры в Vent."
                ),
                weekdayAndTagInsights = "Добавляй к записям #теги (например, #работа, #сон, #прогулка, #семья), чтобы видеть, какие сферы жизни наполняют тебя силами в разные дни недели.",
                gentleAdviceAndQuestions = listOf(
                    "Начни с малого: сегодня напиши всего 2–3 строчки о том, какой момент дня был самым спокойным.",
                    "Вопрос для первой записи: «Что бы мне хотелось отпустить сегодня, а за что — поблагодарить этот день?»"
                ),
                entriesCount = 0,
                averageIntensity = 4.0f,
                dominantMoodKey = BotanicalMood.CALM_LEAF.key,
                topTags = emptyList(),
                weekdayBreakdown = weekdayBreakdown,
                createdAtMillis = System.currentTimeMillis()
            )
        } else {
            AiPsychologicalReport(
                periodKey = period.periodKey,
                periodType = period.periodType.id,
                periodTitle = periodTitle,
                emotionalHeadline = "Beginning Your Mindful Journey",
                warmLetter = "Welcome to your personal Gemini AI psychological reflection space for $periodTitle. As you write entries in your journal — even a few honest lines about your mood, energy, and daily moments — Gemini will weave them into an in-depth portrait of your emotional wellbeing.",
                emotionalArcSummary = "Write a journal entry for $periodTitle and tap 'Analyze My State' to receive your personalized Gemini AI insight.",
                energySources = listOf(
                    "Taking a quiet moment to check in with your feelings without judgment.",
                    "Small daily rituals that bring calm and warmth."
                ),
                stressTriggers = listOf(
                    "Holding unspoken worries in your mind without a safe place to release them."
                ),
                cognitivePatterns = listOf(
                    "Intentional self-care: opening this space shows care for your inner balance."
                ),
                quietWins = listOf(
                    "Created a dedicated sanctuary for emotional reflection in Vent."
                ),
                weekdayAndTagInsights = "Add #tags to your entries (like #walk, #work, #rest, #family) to uncover how different days of the week shape your mood.",
                gentleAdviceAndQuestions = listOf(
                    "Start small today: write 2–3 sentences about one moment that brought you peace.",
                    "Question for today: 'What is my mind asking for right now — rest, clarity, or warmth?'"
                ),
                entriesCount = 0,
                averageIntensity = 4.0f,
                dominantMoodKey = BotanicalMood.CALM_LEAF.key,
                topTags = emptyList(),
                weekdayBreakdown = weekdayBreakdown,
                createdAtMillis = System.currentTimeMillis()
            )
        }
    }

    private fun buildPsychologistPrompt(
        period: AvailableAnalysisPeriod,
        entries: List<DiaryEntry>,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        avgIntensity: Float,
        dominantMood: BotanicalMood,
        topTags: List<String>,
        lang: AppLanguage
    ): String {
        val languageInstruction = if (lang == AppLanguage.RU) {
            "ВАЖНО: Отвечай ИСКЛЮЧИТЕЛЬНО на русском языке. Тон — бережный, глубокий, тёплый, внимательный психолог-аналитик (без медицинских диагнозов, без канцелярита и банальных клише). Обращайся к автору дневника уважительно и тепло на «ты»."
        } else {
            "IMPORTANT: Respond EXCLUSIVELY in English. Tone: warm, perceptive, compassionate psychologist and emotional wellbeing analyst (no clinical diagnoses or generic clichés). Address the journal author directly as 'you'."
        }

        val periodContext = when (period.periodType) {
            AnalysisPeriodType.DAILY -> if (lang == AppLanguage.RU) {
                "Это ГЛУБОКИЙ ПСИХОЛОГИЧЕСКИЙ РАЗБОР ЗА ДЕНЬ («${period.localizedTitle(lang)}»). Внимательно разбери сегодняшние/дневные записи автора, его эмоции, мысли, ответы на вопросы рефлексии, скрытые переживания и точки опоры этого конкретного дня."
            } else {
                "This is an in-depth DAILY PSYCHOLOGICAL REFLECTION for '${period.localizedTitle(lang)}'. Carefully analyze the user's personal entries for this specific day, their emotions, thoughts, reflection answers, and daily anchors."
            }
            AnalysisPeriodType.WEEKLY -> if (lang == AppLanguage.RU) {
                "Это психологический разбор за ПОСЛЕДНИЕ 7 ДНЕЙ (недельный срез состояния)."
            } else {
                "This is a WEEKLY psychological analysis of the LAST 7 DAYS."
            }
            AnalysisPeriodType.YEARLY_REVIEW -> if (lang == AppLanguage.RU) {
                "Это БОЛЬШОЙ ГОДОВОЙ ОТЧЁТ (Итоги года / Декабрьская ретроспектива) за период «${period.localizedTitle(lang)}». Проанализируй глобальную эволюцию состояния человека за год, его главные опоры, преодоления и личностный рост."
            } else {
                "This is a comprehensive YEAR IN REVIEW (December Annual Reflection) for '${period.localizedTitle(lang)}'. Analyze the user's overarching emotional evolution across the year, major anchors, turning points, and personal growth."
            }
            AnalysisPeriodType.LAST_30_DAYS -> if (lang == AppLanguage.RU) {
                "Это глубокий психологический разбор за ПОСЛЕДНИЕ 30 ДНЕЙ."
            } else {
                "This is an in-depth psychological analysis of the LAST 30 DAYS."
            }
            AnalysisPeriodType.MONTHLY -> if (lang == AppLanguage.RU) {
                "Это итоговый психологический разбор месяца за период «${period.localizedTitle(lang)}»."
            } else {
                "This is an end-of-month psychological reflection for '${period.localizedTitle(lang)}'."
            }
        }

        val moodDistributionSummary = BotanicalMood.entries.joinToString(", ") { mood ->
            val count = entries.count { it.moodKey == mood.key }
            "${mood.localizedTitle(lang)} (${mood.localizedSubtitle(lang)}): $count"
        }

        val weekdaySummaryText = weekdayBreakdown.joinToString("; ") { day ->
            val dom = day.dominantMoodKey?.let { BotanicalMood.fromKey(it).localizedTitle(lang) } ?: "-"
            "${day.fullLabel(lang)}: count=${day.entriesCount}, avgIntensity=${String.format("%.1f", day.averageIntensity)}/5, dominant=$dom"
        }

        val entriesBlock = entries.takeLast(60).joinToString("\n---\n") { entry ->
            val mood = entry.botanicalMood()
            val tagsStr = if (entry.tags.isNotEmpty()) "Tags: ${entry.tags.joinToString(", ") { "#$it" }}" else ""
            val reflectionStr = if (!entry.reflectionAnswer.isNullOrBlank()) {
                "Reflection (${entry.reflectionPrompt.orEmpty()}): ${entry.reflectionAnswer}"
            } else ""
            """
            Date: ${entry.dateIso}
            Mood: ${mood.localizedTitle(lang)} (${mood.localizedSubtitle(lang)}) | Intensity: ${entry.moodIntensity}/5
            Title: ${entry.title}
            Content: ${entry.content.take(700)}
            $reflectionStr
            $tagsStr
            """.trimIndent()
        }

        return """
            $languageInstruction
            $periodContext

            === MATHEMATICAL SUMMARY FOR THIS PERIOD ===
            Total entries analyzed: ${entries.size}
            Average emotional intensity / harmony score: ${String.format("%.2f", avgIntensity)} / 5.0
            Dominant mood state: ${dominantMood.localizedTitle(lang)} (${dominantMood.localizedSubtitle(lang)})
            Mood distribution: $moodDistributionSummary
            Weekday rhythm breakdown: $weekdaySummaryText
            Most frequent tags: ${if (topTags.isEmpty()) "None" else topTags.joinToString(", ")}

            === CHRONOLOGICAL JOURNAL ENTRIES ===
            $entriesBlock

            === TASK ===
            Carefully read the user's real thoughts, titles, reflection answers, mood transitions, and weekday rhythm above.
            Reference specific themes, feelings, and details the user actually wrote about so the analysis feels deeply personal and insightful.
        """.trimIndent()
    }

    private fun parseJsonStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx).trim().takeIf { it.isNotEmpty() }
        }
    }

    private fun parseReportJson(
        jsonText: String,
        period: AvailableAnalysisPeriod,
        entriesCount: Int,
        avgIntensity: Float,
        dominantMoodKey: String,
        topTags: List<String>,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        lang: AppLanguage
    ): AiPsychologicalReport {
        val obj = JSONObject(jsonText)

        return AiPsychologicalReport(
            periodKey = period.periodKey,
            periodType = period.periodType.id,
            periodTitle = period.localizedTitle(lang),
            emotionalHeadline = obj.optString("emotionalHeadline").ifBlank {
                period.localizedTitle(lang)
            },
            warmLetter = obj.optString("warmLetter"),
            emotionalArcSummary = obj.optString("emotionalArcSummary"),
            energySources = parseJsonStringArray(obj.optJSONArray("energySources")),
            stressTriggers = parseJsonStringArray(obj.optJSONArray("stressTriggers")),
            cognitivePatterns = parseJsonStringArray(obj.optJSONArray("cognitivePatterns")),
            quietWins = parseJsonStringArray(obj.optJSONArray("quietWins")),
            weekdayAndTagInsights = obj.optString("weekdayAndTagInsights"),
            gentleAdviceAndQuestions = parseJsonStringArray(obj.optJSONArray("gentleAdviceAndQuestions")),
            entriesCount = entriesCount,
            averageIntensity = avgIntensity,
            dominantMoodKey = dominantMoodKey,
            topTags = topTags,
            weekdayBreakdown = weekdayBreakdown,
            createdAtMillis = System.currentTimeMillis(),
            aiModelBadge = "Gemini 3.8 Flash • Firebase AI Logic"
        )
    }

    fun loadSavedReports(userId: String): Map<String, AiPsychologicalReport> {
        val raw = prefs.getString("${KEY_REPORTS_PREFIX}$userId", null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val map = mutableMapOf<String, AiPsychologicalReport>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = root.optJSONObject(key) ?: continue
                map[key] = deserializeCachedReport(item)
            }
            map
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached AI reports", e)
            emptyMap()
        }
    }

    private fun saveReportToLocalCache(userId: String, report: AiPsychologicalReport) {
        try {
            val current = loadSavedReports(userId).toMutableMap()
            current[report.periodKey] = report
            val root = JSONObject()
            current.forEach { (key, rep) ->
                root.put(key, serializeReport(rep))
            }
            prefs.edit().putString("${KEY_REPORTS_PREFIX}$userId", root.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache AI report", e)
        }
    }

    private fun serializeReport(report: AiPsychologicalReport): JSONObject {
        val obj = JSONObject()
        obj.put("periodKey", report.periodKey)
        obj.put("periodType", report.periodType)
        obj.put("periodTitle", report.periodTitle)
        obj.put("emotionalHeadline", report.emotionalHeadline)
        obj.put("warmLetter", report.warmLetter)
        obj.put("emotionalArcSummary", report.emotionalArcSummary)
        obj.put("energySources", JSONArray(report.energySources))
        obj.put("stressTriggers", JSONArray(report.stressTriggers))
        obj.put("cognitivePatterns", JSONArray(report.cognitivePatterns))
        obj.put("quietWins", JSONArray(report.quietWins))
        obj.put("weekdayAndTagInsights", report.weekdayAndTagInsights)
        obj.put("gentleAdviceAndQuestions", JSONArray(report.gentleAdviceAndQuestions))
        obj.put("entriesCount", report.entriesCount)
        obj.put("averageIntensity", report.averageIntensity.toDouble())
        obj.put("dominantMoodKey", report.dominantMoodKey)
        obj.put("topTags", JSONArray(report.topTags))
        obj.put("createdAtMillis", report.createdAtMillis)
        obj.put("aiModelBadge", report.aiModelBadge)

        val weekdaysArr = JSONArray()
        report.weekdayBreakdown.forEach { wd ->
            val wdObj = JSONObject()
            wdObj.put("dayIndexMondayFirst", wd.dayIndexMondayFirst)
            wdObj.put("shortNameEn", wd.shortNameEn)
            wdObj.put("shortNameRu", wd.shortNameRu)
            wdObj.put("fullNameEn", wd.fullNameEn)
            wdObj.put("fullNameRu", wd.fullNameRu)
            wdObj.put("entriesCount", wd.entriesCount)
            wdObj.put("averageIntensity", wd.averageIntensity.toDouble())
            wdObj.put("dominantMoodKey", wd.dominantMoodKey ?: "")
            weekdaysArr.put(wdObj)
        }
        obj.put("weekdayBreakdown", weekdaysArr)
        return obj
    }

    private fun deserializeCachedReport(obj: JSONObject): AiPsychologicalReport {
        val weekdaysArr = obj.optJSONArray("weekdayBreakdown")
        val weekdays = mutableListOf<WeekdayMoodSummary>()
        if (weekdaysArr != null) {
            for (i in 0 until weekdaysArr.length()) {
                val w = weekdaysArr.optJSONObject(i) ?: continue
                weekdays.add(
                    WeekdayMoodSummary(
                        dayIndexMondayFirst = w.optInt("dayIndexMondayFirst", i + 1),
                        shortNameEn = w.optString("shortNameEn"),
                        shortNameRu = w.optString("shortNameRu"),
                        fullNameEn = w.optString("fullNameEn"),
                        fullNameRu = w.optString("fullNameRu"),
                        entriesCount = w.optInt("entriesCount", 0),
                        averageIntensity = w.optDouble("averageIntensity", 0.0).toFloat(),
                        dominantMoodKey = w.optString("dominantMoodKey").takeIf { it.isNotBlank() }
                    )
                )
            }
        }

        return AiPsychologicalReport(
            periodKey = obj.optString("periodKey"),
            periodType = obj.optString("periodType", AnalysisPeriodType.MONTHLY.id),
            periodTitle = obj.optString("periodTitle"),
            emotionalHeadline = obj.optString("emotionalHeadline"),
            warmLetter = obj.optString("warmLetter"),
            emotionalArcSummary = obj.optString("emotionalArcSummary"),
            energySources = parseJsonStringArray(obj.optJSONArray("energySources")),
            stressTriggers = parseJsonStringArray(obj.optJSONArray("stressTriggers")),
            cognitivePatterns = parseJsonStringArray(obj.optJSONArray("cognitivePatterns")),
            quietWins = parseJsonStringArray(obj.optJSONArray("quietWins")),
            weekdayAndTagInsights = obj.optString("weekdayAndTagInsights"),
            gentleAdviceAndQuestions = parseJsonStringArray(obj.optJSONArray("gentleAdviceAndQuestions")),
            entriesCount = obj.optInt("entriesCount", 0),
            averageIntensity = obj.optDouble("averageIntensity", 0.0).toFloat(),
            dominantMoodKey = obj.optString("dominantMoodKey", BotanicalMood.CALM_LEAF.key),
            topTags = parseJsonStringArray(obj.optJSONArray("topTags")),
            weekdayBreakdown = weekdays,
            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
            aiModelBadge = obj.optString("aiModelBadge", "Gemini 3.8 Flash • Firebase AI Logic")
        )
    }

    private companion object {
        const val TAG = "PsychAiAnalyzer"
        const val PREFS_NAME = "vent_ai_psych_reports"
        const val KEY_REPORTS_PREFIX = "reports_by_user_"
    }
}
