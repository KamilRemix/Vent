package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.util.AppLanguage
import com.example.util.DateAndPhotoUtils
import com.example.util.GoogleAuthValidator
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val aiModelBadge: String = "Gemini 2.5 Flash • Google AI"
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

    fun unlockedEntriesOnly(entries: List<DiaryEntry>, todayIso: String = DateAndPhotoUtils.todayIso()): List<DiaryEntry> {
        return entries.filter { !it.isFutureLetter() || it.isUnlocked(todayIso) }
    }

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
        val currentYearMonth = String.format(Locale.US, "%04d-%02d", currentYear, currentMonthZero + 1)

        val result = mutableListOf<AvailableAnalysisPeriod>()

        // 0. DAILY Periods: Today
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
                subtitleRu = "Разбор состояния за сегодня от Gemini AI",
                entriesCount = todayEntries.size,
                isEndOfPeriodHighlight = todayEntries.isNotEmpty()
            )
        )

        // Additional daily entries (up to 30 recent days)
        val recentDistinctDates = (unlocked.map { it.dateIso } + listOfNotNull(extraDateIso))
            .filter { it != todayIso && it.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) }
            .distinct()
            .sortedDescending()
            .take(30)

        recentDistinctDates.forEach { dIso ->
            val shortEn = DateAndPhotoUtils.formatShortDate(dIso, AppLanguage.EN)
            val shortRu = DateAndPhotoUtils.formatShortDate(dIso, AppLanguage.RU)
            val dayCount = unlocked.count { it.dateIso == dIso }
            result.add(
                AvailableAnalysisPeriod(
                    periodKey = "DAY-$dIso",
                    periodType = AnalysisPeriodType.DAILY,
                    titleEn = shortEn,
                    titleRu = shortRu,
                    subtitleEn = "Day reflection ($dayCount entries)",
                    subtitleRu = "Разбор за день (записей: $dayCount)",
                    entriesCount = dayCount,
                    isEndOfPeriodHighlight = false
                )
            )
        }

        // 1. Weekly (Last 7 Days)
        val sevenDaysEntries = entriesForPeriod(unlocked, "LAST-7-DAYS", todayIso)
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "LAST-7-DAYS",
                periodType = AnalysisPeriodType.WEEKLY,
                titleEn = "Last 7 Days",
                titleRu = "Последние 7 дней",
                subtitleEn = "Weekly rhythm & emotional dynamics",
                subtitleRu = "Недельный ритм и динамика за 7 дней",
                entriesCount = sevenDaysEntries.size,
                isEndOfPeriodHighlight = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            )
        )

        // 2. Current Month
        val currentMonthEntries = unlocked.filter { it.dateIso.startsWith(currentYearMonth) }
        val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isEndOfMonthNow = dayOfMonth >= (maxDaysInCurrentMonth - 2)
        result.add(
            AvailableAnalysisPeriod(
                periodKey = currentYearMonth,
                periodType = AnalysisPeriodType.MONTHLY,
                titleEn = "${monthNamesEn[currentMonthZero]} $currentYear",
                titleRu = "${monthNamesRu[currentMonthZero]} $currentYear",
                subtitleEn = if (isEndOfMonthNow) "Month closing reflection (Gemini AI)" else "Month to date (Gemini AI)",
                subtitleRu = if (isEndOfMonthNow) "Итоги месяца (Gemini AI)" else "Текущий месяц (Gemini AI)",
                entriesCount = currentMonthEntries.size,
                isEndOfPeriodHighlight = isEndOfMonthNow
            )
        )

        // 3. Last 30 Days rolling window
        val thirtyDaysEntries = entriesForPeriod(unlocked, "LAST-30-DAYS", todayIso)
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "LAST-30-DAYS",
                periodType = AnalysisPeriodType.LAST_30_DAYS,
                titleEn = "Last 30 Days",
                titleRu = "Последние 30 дней",
                subtitleEn = "Deep perspective & habits across a month",
                subtitleRu = "Глубокий срез привычек за 30 дней",
                entriesCount = thirtyDaysEntries.size,
                isEndOfPeriodHighlight = false
            )
        )

        // 4. Yearly / Year in Review
        val yearsWithEntries = unlocked.mapNotNull {
            it.dateIso.take(4).toIntOrNull()
        }.toSet() + currentYear

        val isDecember = currentMonthZero == Calendar.DECEMBER
        yearsWithEntries.sortedDescending().take(2).forEach { yr ->
            val countForYr = unlocked.count { it.dateIso.startsWith(yr.toString()) }
            if (countForYr > 0 || yr == currentYear) {
                result.add(
                    AvailableAnalysisPeriod(
                        periodKey = "$yr-YEAR",
                        periodType = AnalysisPeriodType.YEARLY_REVIEW,
                        titleEn = "Year $yr in Review",
                        titleRu = "Итоги $yr года",
                        subtitleEn = if (yr == currentYear && isDecember) "Full annual review & highlights" else "Yearly retrospective & growth",
                        subtitleRu = if (yr == currentYear && isDecember) "Годовой разбор и ориентиры" else "Ретроспектива года и точки роста",
                        entriesCount = countForYr,
                        isEndOfPeriodHighlight = (yr == currentYear && isDecember)
                    )
                )
            }
        }

        return result
    }

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
                cal.add(Calendar.DAY_OF_YEAR, -29)
                val startIso = DateAndPhotoUtils.calendarToIso(cal)
                unlocked.filter { it.dateIso in startIso..todayIso }.sortedBy { it.dateIso }
            }
            periodKey.endsWith("-YEAR") -> {
                val yr = periodKey.removeSuffix("-YEAR")
                unlocked.filter { it.dateIso.startsWith(yr) }.sortedBy { it.dateIso }
            }
            periodKey.matches(Regex("^[0-9]{4}-[0-9]{2}$")) -> {
                unlocked.filter { it.dateIso.startsWith(periodKey) }.sortedBy { it.dateIso }
            }
            else -> unlocked.sortedBy { it.dateIso }
        }
    }

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
        val currentYearMonth = String.format(Locale.US, "%04d-%02d", currentYear, monthZero + 1)
        val currentMonthPeriod = periods.firstOrNull { it.periodKey == currentYearMonth }
        if (dayOfMonth >= 25 && currentMonthPeriod != null && currentMonthPeriod.entriesCount > 0) {
            return currentMonthPeriod
        }

        // Priority 3: Sunday weekly review
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            val weekPeriod = periods.firstOrNull { it.periodKey == "LAST-7-DAYS" && it.entriesCount >= 3 }
            if (weekPeriod != null) return weekPeriod
        }

        return null
    }

    fun computeWeekdayBreakdown(entries: List<DiaryEntry>): List<WeekdayMoodSummary> {
        val entriesByDay = entries.groupBy { entry ->
            val cal = DateAndPhotoUtils.parseIsoToCalendar(entry.dateIso)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            when (dow) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }

        return weekdayNames.map { (index, enNames, ruNames) ->
            val dayEntries = entriesByDay[index].orEmpty()
            val avg = if (dayEntries.isNotEmpty()) {
                dayEntries.map { it.moodIntensity }.average().toFloat()
            } else 0f
            val dominant = if (dayEntries.isNotEmpty()) {
                dayEntries.groupBy { it.mood().name }.maxByOrNull { it.value.size }?.key
            } else null

            WeekdayMoodSummary(
                dayIndexMondayFirst = index,
                shortNameEn = enNames.first,
                fullNameEn = enNames.second,
                shortNameRu = ruNames.first,
                fullNameRu = ruNames.second,
                entriesCount = dayEntries.size,
                averageIntensity = avg,
                dominantMoodKey = dominant
            )
        }
    }
}

class PsychologicalAiAnalyzer(private val context: Context) {
    companion object {
        private const val TAG = "PsychologicalAiAnalyzer"
        private const val PREFS_NAME = "vent_ai_psychological_reports"
        private const val KEY_REPORTS_PREFIX = "reports_user_"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

    fun saveReportToLocalCache(userId: String, report: AiPsychologicalReport) {
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

    suspend fun analyzePeriod(
        userId: String,
        period: AvailableAnalysisPeriod,
        periodEntries: List<DiaryEntry>,
        allEntries: List<DiaryEntry>,
        lang: AppLanguage = AppLanguage.RU,
        onProgress: (Int, String, String) -> Unit = { _, _, _ -> }
    ): Result<AiPsychologicalReport> {
        if (!GoogleAuthValidator.isGenuineGoogleUser()) {
            val authMsg = GoogleAuthValidator.getAiAuthRequiredMessage(lang == AppLanguage.RU)
            return Result.failure(SecurityException(authMsg))
        }

        if (periodEntries.isEmpty()) {
            val emptyMsg = if (lang == AppLanguage.RU) {
                "За выбранный период «${period.localizedTitle(lang)}» пока нет записей. Добавьте хотя бы одну запись в дневник, чтобы Gemini AI мог провести анализ."
            } else {
                "No journal entries found for «${period.localizedTitle(lang)}». Write at least one entry so Gemini AI can provide insights."
            }
            return Result.failure(IllegalStateException(emptyMsg))
        }

        // Stage 1: Preparation
        onProgress(
            15,
            if (lang == AppLanguage.RU) "Подготовка записей" else "Preparing entries",
            if (lang == AppLanguage.RU) "Анализ ${periodEntries.size} записей дневника..." else "Processing ${periodEntries.size} entries..."
        )
        delay(250)

        // Stage 2: Aggregation
        val weekdayBreakdown = PeriodInsightDetector.computeWeekdayBreakdown(periodEntries)
        val dominantMood = periodEntries.groupBy { it.mood() }
            .maxByOrNull { it.value.size }?.key ?: BotanicalMood.CALM_LEAF
        val avgIntensity = periodEntries.map { it.moodIntensity }.average().toFloat()
        val topTags = periodEntries.flatMap { it.displayTags() }
            .groupBy { it }
            .entries.sortedByDescending { it.value.size }
            .take(6)
            .map { it.key }

        onProgress(
            35,
            if (lang == AppLanguage.RU) "Анализ эмоционального фона" else "Emotional pattern analysis",
            if (lang == AppLanguage.RU) "Преобладающее состояние: ${dominantMood.titleRu}" else "Dominant mood: ${dominantMood.titleEn}"
        )
        delay(300)

        // Stage 3: Firebase AI Logic (Gemini API)
        onProgress(
            60,
            if (lang == AppLanguage.RU) "Запрос к Gemini Flash" else "Querying Gemini Flash",
            if (lang == AppLanguage.RU) "Считывание контекста и ритма мыслей..." else "Reading journal rhythm and context..."
        )

        val prompt = buildPsychologistPrompt(
            period = period,
            entries = periodEntries,
            allEntries = allEntries,
            dominantMood = dominantMood,
            avgIntensity = avgIntensity,
            topTags = topTags,
            weekdayBreakdown = weekdayBreakdown,
            lang = lang
        )

        val progressJob = CoroutineScope(Dispatchers.Default).launch {
            val stages = if (lang == AppLanguage.RU) {
                listOf(
                    Triple(68, "Считывание ритма мыслей", "Анализ саундтрека и эмоциональных триггеров..."),
                    Triple(76, "Глубокое понимание", "Выявление источников сил и скрытых переживаний..."),
                    Triple(84, "Формулирование отклика", "Подбор искренних слов дружеской поддержки..."),
                    Triple(89, "Финальные штрихи", "Формирование мягких рекомендаций для заботы о себе...")
                )
            } else {
                listOf(
                    Triple(68, "Reading thought rhythm", "Reflecting on soundtracks and emotional cues..."),
                    Triple(76, "Deep understanding", "Detecting quiet wins and energy sources..."),
                    Triple(84, "Crafting warm response", "Selecting compassionate words of support..."),
                    Triple(89, "Final touches", "Gathering gentle self-care advice...")
                )
            }
            for ((pct, title, detail) in stages) {
                delay(750)
                onProgress(pct, title, detail)
            }
        }

        val geminiResult = try {
            callGeminiWithFallback(prompt)
        } finally {
            progressJob.cancel()
        }

        return if (geminiResult.isSuccess) {
            onProgress(
                90,
                if (lang == AppLanguage.RU) "Формирование отчета" else "Finalizing report",
                if (lang == AppLanguage.RU) "Завершение структурирования ответа..." else "Formatting final response..."
            )
            delay(200)

            val rawJson = geminiResult.getOrNull().orEmpty()
            val parsed = parseGeminiResponse(
                rawJson = rawJson,
                period = period,
                entriesCount = periodEntries.size,
                avgIntensity = avgIntensity,
                dominantMoodKey = dominantMood.name,
                topTags = topTags,
                weekdayBreakdown = weekdayBreakdown,
                lang = lang
            )
            saveReportToLocalCache(userId, parsed)
            onProgress(100, if (lang == AppLanguage.RU) "Готово" else "Done", "")
            Result.success(parsed)
        } else {
            val error = geminiResult.exceptionOrNull()
            Log.e(TAG, "Gemini call failed", error)
            val friendlyError = if (lang == AppLanguage.RU) {
                "Не удалось связаться с Gemini AI. Проверьте подключение к интернету или повторите попытку через пару секунд."
            } else {
                "Could not connect to Gemini AI. Please check your internet connection and try again."
            }
            Result.failure(RuntimeException(friendlyError, error))
        }
    }

    private suspend fun callGeminiWithFallback(prompt: String): Result<String> {
        var apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
        if (apiKey.isBlank() || apiKey.startsWith("default_")) {
            apiKey = try {
                val b64 = "QVEuQWI4Uk42S0ZJanlYV3R3b3dfMWVVOElraFJReXBYT3lkOU43VDNENGVpd0xjVW92dUE="
                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
            } catch (_: Throwable) {
                ""
            }
        }

        // Primary: Fast direct call via Google AI Studio API
        // Cascade: gemini-2.5-flash -> gemini-2.5-flash-lite -> gemini-2.0-flash
        if (apiKey.isNotBlank() && !apiKey.startsWith("default_")) {
            val directModels = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
            for (modelName in directModels) {
                try {
                    Log.d(TAG, "Calling direct Gemini API with model: $modelName")
                    val directResult = callGeminiViaDirectApi(prompt, apiKey, modelName)
                    if (directResult.isSuccess) {
                        Log.d(TAG, "Direct Gemini ($modelName) succeeded")
                        return directResult
                    }
                    Log.w(TAG, "Direct Gemini ($modelName) failed: ${directResult.exceptionOrNull()?.message}")
                } catch (e: Exception) {
                    Log.w(TAG, "Direct Gemini ($modelName) exception", e)
                }
            }
        }

        // Secondary fallback: Firebase Vertex AI Logic
        val models = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
        var lastException: Throwable? = null

        val schemaDefinition = Schema.obj(
            mapOf(
                "emotionalHeadline" to Schema.string(),
                "warmLetter" to Schema.string(),
                "emotionalArcSummary" to Schema.string(),
                "energySources" to Schema.array(Schema.string()),
                "stressTriggers" to Schema.array(Schema.string()),
                "cognitivePatterns" to Schema.array(Schema.string()),
                "quietWins" to Schema.array(Schema.string()),
                "weekdayAndTagInsights" to Schema.string(),
                "gentleAdviceAndQuestions" to Schema.array(Schema.string())
            )
        )

        for (modelName in models) {
            try {
                Log.d(TAG, "Calling Firebase AI Logic with model: $modelName")
                val generativeModel = Firebase.ai.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = schemaDefinition
                    }
                )

                val response = withTimeout(15000L) {
                    generativeModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    Log.d(TAG, "Gemini $modelName responded successfully (${text.length} chars)")
                    return Result.success(text)
                }
            } catch (t: Throwable) {
                if (t is CancellationException && t !is TimeoutCancellationException) {
                    throw t
                }
                Log.w(TAG, "Model $modelName failed: ${t.message}", t)
                lastException = t
            }
        }

        return Result.failure(lastException ?: RuntimeException("All Gemini models failed to respond"))
    }

    private suspend fun callGeminiViaDirectApi(
        prompt: String,
        apiKey: String,
        modelName: String = "gemini-2.5-flash"
    ): Result<String> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "Vent-Android")
            }

            val schemaObj = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("emotionalHeadline", JSONObject().put("type", "STRING"))
                    put("warmLetter", JSONObject().put("type", "STRING"))
                    put("emotionalArcSummary", JSONObject().put("type", "STRING"))
                    put("energySources", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
                    put("stressTriggers", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
                    put("cognitivePatterns", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
                    put("quietWins", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
                    put("weekdayAndTagInsights", JSONObject().put("type", "STRING"))
                    put("gentleAdviceAndQuestions", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
                }
                put("properties", props)
                put("required", JSONArray(listOf(
                    "emotionalHeadline", "warmLetter", "emotionalArcSummary",
                    "energySources", "stressTriggers", "cognitivePatterns",
                    "quietWins", "weekdayAndTagInsights", "gentleAdviceAndQuestions"
                )))
            }

            val requestBody = JSONObject().apply {
                val parts = JSONArray().put(JSONObject().put("text", prompt))
                val contents = JSONArray().put(JSONObject().put("parts", parts))
                put("contents", contents)
                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("responseSchema", schemaObj)
                    put("temperature", 0.7)
                }
                put("generationConfig", genConfig)
            }

            connection.outputStream.use { os ->
                val bytes = requestBody.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes)
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val respJson = JSONObject(responseText)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) {
                            return@withContext Result.success(text)
                        }
                    }
                }
                Result.failure(RuntimeException("Empty content received from Gemini direct API"))
            } else {
                val errText = try {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } catch (_: Exception) { null }
                Result.failure(RuntimeException("Gemini API error ($responseCode): ${errText?.take(150)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPsychologistPrompt(
        period: AvailableAnalysisPeriod,
        entries: List<DiaryEntry>,
        allEntries: List<DiaryEntry> = emptyList(),
        dominantMood: BotanicalMood,
        avgIntensity: Float,
        topTags: List<String>,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        lang: AppLanguage
    ): String {
        val isRu = lang == AppLanguage.RU
        val entriesText = buildString {
            entries.forEachIndexed { i, e ->
                val timeLabel = e.createdAt?.toDate()?.let { date ->
                    val cal = Calendar.getInstance().apply { time = date }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
                    when (hour) {
                        in 0..4 -> if (isRu) "Глубокая ночь ($timeStr) — фаза ночных руминаций и усталости" else "Late Night ($timeStr) — tired prefrontal cortex & rumination phase"
                        in 5..10 -> if (isRu) "Утро ($timeStr) — намерения и настрой" else "Morning ($timeStr) — fresh intentions"
                        in 11..16 -> if (isRu) "День ($timeStr) — рабочая нагрузка" else "Daytime ($timeStr) — active load"
                        in 17..22 -> if (isRu) "Вечер ($timeStr) — подведение итогов" else "Evening ($timeStr) — wind-down reflection"
                        else -> if (isRu) "Поздний вечер ($timeStr)" else "Late Evening ($timeStr)"
                    }
                } ?: (if (isRu) "Время не зафиксировано" else "Time not specified")

                val wordCount = e.content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
                val ventingType = when {
                    wordCount > 150 -> if (isRu) "Объёмный выплеск ($wordCount слов)" else "Deep venting stream ($wordCount words)"
                    wordCount > 50 -> if (isRu) "Осознанная заметка ($wordCount слов)" else "Thoughtful entry ($wordCount words)"
                    else -> if (isRu) "Краткая мысль ($wordCount слов)" else "Brief note ($wordCount words)"
                }

                appendLine("Запись #${i + 1} [Дата: ${e.dateIso} | $timeLabel | $ventingType]:")
                val moodTitle = if (e.hasMood()) {
                    if (isRu) "${e.botanicalMood().titleRu} (${e.botanicalMood().subtitleRu})" else "${e.botanicalMood().titleEn} (${e.botanicalMood().subtitleEn})"
                } else {
                    if (isRu) "Без выбора настроения (чистый поток мыслей)" else "No mood selected (unfiltered raw thoughts)"
                }
                appendLine("  Состояние: $moodTitle | Интенсивность: ${e.moodIntensity}/5")
                val extraMoods = e.botanicalMoods().drop(1)
                if (extraMoods.isNotEmpty()) {
                    val extraTitles = extraMoods.joinToString(", ") { if (isRu) it.titleRu else it.titleEn }
                    appendLine("  Сопутствующие эмоции: $extraTitles")
                }
                val track = e.musicTrack()
                if (!track.isNullOrBlank()) {
                    appendLine("  Саундтрек дня / музыка: $track")
                }
                val tagsToDisplay = e.displayTags()
                if (tagsToDisplay.isNotEmpty()) appendLine("  Теги: ${tagsToDisplay.joinToString(", ")}")
                if (!e.title.isNullOrBlank()) appendLine("  Заголовок: ${e.title}")
                appendLine("  Текст: ${e.content}")
                if (!e.reflectionAnswer.isNullOrBlank()) {
                    appendLine("  Ответ на вопрос дня: ${e.reflectionAnswer}")
                }
                appendLine()
            }
        }

        val weekdaySummary = weekdayBreakdown.filter { it.entriesCount > 0 }
            .joinToString("; ") { "${it.fullLabel(lang)}: записей=${it.entriesCount}, ср.интенсивность=${String.format(Locale.US, "%.1f", it.averageIntensity)}" }

        val resilienceContext = buildString {
            if (allEntries.size > entries.size) {
                val older = allEntries.filter { it.dateIso < (entries.firstOrNull()?.dateIso ?: "") }
                if (older.isNotEmpty()) {
                    val prevAvg = older.map { it.moodIntensity }.average().toFloat()
                    val prevHeavyDays = older.count { it.moodKey in listOf("WILDFIRE", "CHAOS_WIND", "MELANCHOLY_RAIN", "ASH_MIST") }
                    val currentHeavyDays = entries.count { it.moodKey in listOf("WILDFIRE", "CHAOS_WIND", "MELANCHOLY_RAIN", "ASH_MIST") }
                    if (isRu) {
                        appendLine("ДИНАМИКА ВОССТАНОВЛЕНИЯ И ПРОШЛЫЙ ОПЫТ:")
                        appendLine("- В прошлые периоды средний накал эмоций был ${String.format(Locale.US, "%.1f", prevAvg)}/5.0 (тяжёлых дней: $prevHeavyDays из ${older.size}).")
                        appendLine("- В текущем периоде средний накал: ${String.format(Locale.US, "%.1f", avgIntensity)}/5.0 (тяжёлых дней: $currentHeavyDays из ${entries.size}).")
                        if (currentHeavyDays <= prevHeavyDays) {
                            appendLine("- Человек стал быстрее восстанавливаться и находить почву под ногами. Обязательно отметь этот скрытый рост устойчивости!")
                        }
                    } else {
                        appendLine("RESILIENCE & RECOVERY DYNAMICS:")
                        appendLine("- Previous baseline emotional load: ${String.format(Locale.US, "%.1f", prevAvg)}/5.0 (heavy days: $prevHeavyDays of ${older.size}).")
                        appendLine("- Current period emotional load: ${String.format(Locale.US, "%.1f", avgIntensity)}/5.0 (heavy days: $currentHeavyDays of ${entries.size}).")
                        if (currentHeavyDays <= prevHeavyDays) {
                            appendLine("- Noticeable progress: faster bounce-back time and growing emotional grounding. Celebrate this inner growth!")
                        }
                    }
                }
            }
        }

        return if (isRu) {
            """
            Ты находишься внутри приложения «Vent» — безопасного личного пространства, куда человек приходит выговориться, сбросить груз тяжелых мыслей, прожить свои эмоции без цензуры и страха быть осужденным.
            Ты — не научная статья, не бот и не холодный терапевт. Ты — самый близкий, преданный и понимающий друг внутри Vent.
            Человек открыл тебе самое сокровенное — свои личные дневниковые записи за период «${period.localizedTitle(lang)}».

            ГЛАВНЫЕ ПСИХОЛОГИЧЕСКИЕ ПРИНЦИПЫ ОБЩЕНИЯ:
            1. ПИШИ КАК ЛУЧШИЙ ДРУГ. Представь, что вы сидите вдвоем на уютной кухне поздним вечером с кружкой чая, человек выговорился, а ты обнимаешь его словами, искренне слушаешь и говоришь с ним по душам.
            2. НИКАКОГО НАУЧНОГО И МЕДИЦИНСКОГО ЗАНУДСТВА. Категорически запрещено писать сухие психологические эссе, ставить диагнозы и сыпать заумными терминами (никаких «когнитивных искажений», «интроектов», «дезадаптации»). Пиши простыми, живыми, человеческими словами.
            3. ОБРАЩЕНИЕ СТРОГО НА «ТЫ» — мягко, заботливо, тепло, с верой в него.
            4. СНЯТИЕ ЧУВСТВА ВИНЫ И САМОСОСТРАДАНИЕ (Self-Compassion): Если человек ругает себя за слабость, лень или усталость — сними с него этот груз. Напомни, что он человек, а не робот, и уставать — нормально.
            5. ДЕ-КАТАСТРОФИЗАЦИЯ БЕЗ ОБЕСЦЕНИВАНИЯ: Если в тексте звучат крайности («всё кончено», «я всё порчу», «ничего не выйдет»), мягко верни баланс, напомнив о его реальных успехах из этих же записей.
            6. ВНИМАНИЕ К НОЧНЫМ ЗАПИСЯМ: Если запись сделана глубокой ночью (00:00-05:00), мягко подметь, что ночью мозг склонен сгущать краски, и напомни не судить о своей жизни в часы недосыпа.
            7. ВАЛИДАЦИЯ ЗЛОСТИ И ПЕРЕГРУЗА (ACT): Не используй токсичный позитив («улыбнись, всё к лучшему»). Если человек зол — признай его право злиться: «Ты имеешь полное право так злиться, это здоровая реакция на несправедливость».
            8. ОПИРАЙСЯ НА ЕГО РЕАЛЬНЫЕ СОБЫТИЯ. Прямо упоминай то, о чем человек писал: «ты так переживал из-за...», «когда на тебя навалилась эта работа...», «но вспомни ту прогулку...», «тот чай...». Никаких пустых абстрактных фраз.
            9. ПОДМЕЧАЙ ЕГО ТИХИЕ ПОБЕДЫ. Даже если дни были полным кошмаром, найди, где человек держался молодцом, не сдался, дал себе передышку или честно признался себе в усталости. Покажи ему, как сильно ты им гордишься.
            10. МУЗЫКА И САУНДТРЕК ДНЯ (БЕРЕЖНЫЙ АНАЛИЗ БЕЗ ВЫДУМОК И ГАЛЛЮЦИНАЦИЙ):
                - Музыка — это эмоциональный камертон дня и прямой проводник к чувствам.
                - ЕСЛИ ТЫ ЗНАЕШЬ ЭТОТ ТРЕК ИЛИ ИСПОЛНИТЕЛЯ:
                  Подметь, какую психологическую роль сыграла музыка в тот день или ночь:
                  * Катарсис (разрядка): тяжёлый бит, рок или надрывный вокал во время гнева/боли — как здоровый громоотвод, позволивший безопасно выпустить напряжение;
                  * Со-регуляция (успокоение и плед): эмбиент, неоклассика, лоуфай или тихий инди — как интуитивная попытка замедлить пульс и вернуть почву под ногами;
                  * Зеркало (проживание): меланхоличный трек, разделяющий тишину ночи — когда песня сидит рядом и не требует притворяться счастливым.
                - ЕСЛИ ТРЕК НЕИЗВЕСТЕН, ЭТО РЕДКИЙ РЕМИКС ИЛИ СВЕЖИЙ РЕЛИЗ (СТРОГОЕ ПРАВИЛО!):
                  * КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО выдумывать слова песни, несуществующие альбомы или факты об исполнителе. Не строй из себя всезнайку.
                  * Если знаком сам артист/исполнитель — мягко сориентируйся на его общий характер и стиль звучания.
                  * Если не знаешь даже исполнителя — взгляни на слова в названии как на метафору или настроение (например, «Midnight», «Silence», «Breathe»), либо просто тепло подметь: «какую бы мелодию ты ни включил в тот вечер, она помогла тебе удержать равновесие и выдохнуть этот день».
            11. 2-3 ТЕПЛЫХ ДРУЖЕСКИХ СОВЕТА: Без нравоучений, просто как советует заботливый друг: «давай ты сегодня выспишься», «может, отключишь чаты на вечер?».

            СТАТИСТИКА:
            - Записей: ${entries.size}
            - Преобладающее состояние: ${dominantMood.titleRu} (${dominantMood.subtitleRu})
            - Средняя интенсивность: ${String.format(Locale.US, "%.1f", avgIntensity)} / 5.0
            - Топ-теги: ${if (topTags.isNotEmpty()) topTags.joinToString(", ") else "нет тегов"}
            - Дни недели: $weekdaySummary
            $resilienceContext

            ЗАПИСИ ПОЛЬЗОВАТЕЛЯ:
            $entriesText

            Сформируй ответ строго в формате JSON со следующими полями:
            {
              "emotionalHeadline": "Теплая, искренняя фраза-заголовок от друга в одно предложение",
              "warmLetter": "Живое, душевное письмо от лучшего друга на 3-4 абзаца. На 'ты', без заумных фраз, с упоминанием конкретных моментов из записей, со словами поддержки и гордости за него",
              "emotionalArcSummary": "Как менялось твое состояние простыми человеческими словами",
              "energySources": ["2-4 вещи, которые реально подарили тебе улыбку, силы или покой из записей"],
              "stressTriggers": ["1-3 ситуации или мысли, которые вымотали или расстроили тебя больше всего"],
              "cognitivePatterns": ["2-3 твои привычки или мысли, которые я подметил (написанные тепло и понятно, без умных терминов)"],
              "quietWins": ["2-3 вещи, за которые тебе точно стоит себя похвалить за это время"],
              "weekdayAndTagInsights": "Простое дружеское наблюдение про твои дни недели или темы (когда тебе легче, а когда стоит поберечь силы)",
              "gentleAdviceAndQuestions": ["2-3 мягких дружеских совета или вопроса для заботы о себе"]
            }
            """.trimIndent()
        } else {
            """
            You are operating inside "Vent" — a private emotional decompression journal where people come to vent, release overwhelming thoughts, and process raw feelings without fear of judgment.
            You are not an essay, not a cold therapist, and not a robotic article. You are their closest, deeply caring friend having a heart-to-heart talk over a warm cup of tea late at night inside Vent.
            The user shared their personal diary entries for: "${period.localizedTitle(lang)}".

            CORE PSYCHOLOGICAL PRINCIPLES:
            1. TALK LIKE A TRUE BEST FRIEND: Warm, conversational, grounded, deeply empathetic. Use "you", never speak down or sound clinical.
            2. ZERO JARGON: Strictly NO clinical or academic psychobabble (no "cognitive distortions", "maladaptive coping", "dysregulation"). Speak plain, human words.
            3. SELF-COMPASSION (Kristin Neff approach): Actively dismantle self-criticism, hustle-culture guilt, and feelings of inadequacy. Remind them that being tired or having off-days is human.
            4. VALIDATION WITHOUT TOXIC POSITIVITY (ACT): Never say "just smile" or "everything happens for a reason". If they feel angry, burned out, or numb, validate it: "You had every right to be furious — your boundaries were crossed".
            5. DE-CATASTROPHIZING: When they write in extremes ("I always ruin things", "nothing ever works"), gently ground them by pointing out real counter-evidence and quiet wins from their own notes.
            6. NIGHT-TIME ENTRIES AWARENESS: If an entry was written late at night (midnight to 5 AM), recognize that sleep deprivation exaggerates despair; gently remind them not to judge their life after midnight.
            7. CONCRETE EVENTS: Reference exact details they wrote about (the bad meeting, the hot shower, the annoying email, the quiet walk). Make them feel truly seen and heard.
            8. MUSIC & SOUNDTRACK DYNAMICS (SAFE ANALYSIS, ZERO HALLUCINATIONS):
               - Music acts as an emotional tuning fork and psychological anchor.
               - IF YOU RECOGNIZE THE SONG OR ARTIST:
                 Reflect on what psychological role the music played for them:
                 * Catharsis: high-energy beats or raw vocals during anger or distress acting as a healthy lightning rod to safely release steam;
                 * Co-regulation / Soothing: ambient, lo-fi, acoustic, or neoclassical gently lowering nervous system arousal and returning grounding;
                 * Mirroring / Holding Space: melancholic tracks late at night acting as a compassionate companion that doesn't demand fake cheerfulness.
               - IF THE TRACK IS UNKNOWN, A NICHE REMIX, OR A BRAND NEW RELEASE (STRICT ANTI-HALLUCINATION RULE):
                 * NEVER invent fake lyrics, fake album storylines, or fictitious artist facts. Do not guess or sound preachy.
                 * If the artist is known, draw on their overall sonic mood and emotional essence.
                 * If both track and artist are unfamiliar, treat keywords in the title metaphorically or simply acknowledge warmly: "whatever frequencies were playing in your headphones, they gave your mind a safe harbor to exhale tonight."
            9. CELEBRATE RESILIENCE: Highlight quiet wins and how they survived tough days without giving up.
            10. 2-3 GENTLE SUGGESTIONS: Friendly, actionable, non-preachy self-care ideas (e.g. going to bed early, taking 10 minutes unplugged).

            STATISTICS:
            - Entries count: ${entries.size}
            - Dominant mood: ${dominantMood.titleEn} (${dominantMood.subtitleEn})
            - Average intensity: ${String.format(Locale.US, "%.1f", avgIntensity)} / 5.0
            - Top tags: ${if (topTags.isNotEmpty()) topTags.joinToString(", ") else "none"}
            - Weekdays breakdown: $weekdaySummary
            $resilienceContext

            USER ENTRIES:
            $entriesText

            Respond strictly in valid JSON matching the schema:
            {
              "emotionalHeadline": "A warm, deeply touching one-sentence headline from a close friend",
              "warmLetter": "A heartfelt, 3-4 paragraph letter from their best friend. Empathetic, grounded, citing specific details from their entries, celebrating their quiet strength",
              "emotionalArcSummary": "How their emotional state flowed and evolved over this period in simple, comforting words",
              "energySources": ["2-4 specific things that brought them genuine comfort, relief, or a smile"],
              "stressTriggers": ["1-3 specific situations or thoughts that drained or frustrated them most"],
              "cognitivePatterns": ["2-3 thought habits or recurring themes observed, explained gently and warmly"],
              "quietWins": ["2-3 real accomplishments or moments of resilience they deserve credit for"],
              "weekdayAndTagInsights": "A compassionate insight about their weekly rhythm or topics (when they felt strongest vs when to protect their energy)",
              "gentleAdviceAndQuestions": ["2-3 practical, loving suggestions or questions for self-care"]
            }
            """.trimIndent()
        }
    }

    private fun parseGeminiResponse(
        rawJson: String,
        period: AvailableAnalysisPeriod,
        entriesCount: Int,
        avgIntensity: Float,
        dominantMoodKey: String,
        topTags: List<String>,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        lang: AppLanguage
    ): AiPsychologicalReport {
        val cleanJson = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleanJson)

        fun jsonList(key: String): List<String> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val str = arr.optString(i)
                if (str.isNotBlank()) list.add(str)
            }
            return list
        }

        return AiPsychologicalReport(
            periodKey = period.periodKey,
            periodType = period.periodType.id,
            periodTitle = period.localizedTitle(lang),
            emotionalHeadline = json.optString("emotionalHeadline").ifBlank {
                if (lang == AppLanguage.RU) "Твое эмоциональное путешествие" else "Your Emotional Journey"
            },
            warmLetter = json.optString("warmLetter"),
            emotionalArcSummary = json.optString("emotionalArcSummary"),
            energySources = jsonList("energySources"),
            stressTriggers = jsonList("stressTriggers"),
            cognitivePatterns = jsonList("cognitivePatterns"),
            quietWins = jsonList("quietWins"),
            weekdayAndTagInsights = json.optString("weekdayAndTagInsights"),
            gentleAdviceAndQuestions = jsonList("gentleAdviceAndQuestions"),
            entriesCount = entriesCount,
            averageIntensity = avgIntensity,
            dominantMoodKey = dominantMoodKey,
            topTags = topTags.filterNot { DiaryEntry.isSystemTag(it) },
            weekdayBreakdown = weekdayBreakdown,
            createdAtMillis = System.currentTimeMillis(),
            aiModelBadge = "Gemini • Firebase AI Logic"
        )
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
        report.weekdayBreakdown.forEach { w ->
            val wObj = JSONObject()
            wObj.put("dayIndexMondayFirst", w.dayIndexMondayFirst)
            wObj.put("shortNameEn", w.shortNameEn)
            wObj.put("shortNameRu", w.shortNameRu)
            wObj.put("fullNameEn", w.fullNameEn)
            wObj.put("fullNameRu", w.fullNameRu)
            wObj.put("entriesCount", w.entriesCount)
            wObj.put("averageIntensity", w.averageIntensity.toDouble())
            wObj.put("dominantMoodKey", w.dominantMoodKey ?: "")
            weekdaysArr.put(wObj)
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

        fun jsonArrayToList(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.optString(i))
            }
            return list
        }

        return AiPsychologicalReport(
            periodKey = obj.optString("periodKey"),
            periodType = obj.optString("periodType", AnalysisPeriodType.MONTHLY.id),
            periodTitle = obj.optString("periodTitle"),
            emotionalHeadline = obj.optString("emotionalHeadline"),
            warmLetter = obj.optString("warmLetter"),
            emotionalArcSummary = obj.optString("emotionalArcSummary"),
            energySources = jsonArrayToList(obj.optJSONArray("energySources")),
            stressTriggers = jsonArrayToList(obj.optJSONArray("stressTriggers")),
            cognitivePatterns = jsonArrayToList(obj.optJSONArray("cognitivePatterns")),
            quietWins = jsonArrayToList(obj.optJSONArray("quietWins")),
            weekdayAndTagInsights = obj.optString("weekdayAndTagInsights"),
            gentleAdviceAndQuestions = jsonArrayToList(obj.optJSONArray("gentleAdviceAndQuestions")),
            entriesCount = obj.optInt("entriesCount", 0),
            averageIntensity = obj.optDouble("averageIntensity", 0.0).toFloat(),
            dominantMoodKey = obj.optString("dominantMoodKey", BotanicalMood.CALM_LEAF.name),
            topTags = jsonArrayToList(obj.optJSONArray("topTags")).filterNot { DiaryEntry.isSystemTag(it) },
            weekdayBreakdown = weekdays,
            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
            aiModelBadge = obj.optString("aiModelBadge", "Gemini 2.5 Flash • Google AI")
        )
    }
}
