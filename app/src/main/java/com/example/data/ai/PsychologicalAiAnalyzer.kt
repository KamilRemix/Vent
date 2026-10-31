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
    val aiModelBadge: String = "Gemini • Firebase AI Logic"
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
        val currentYearMonth = String.format("%04d-%02d", currentYear, currentMonthZero + 1)

        val result = mutableListOf<AvailableAnalysisPeriod>()

        // 1. Daily option for today
        val todayEntries = unlocked.filter { it.dateIso == todayIso }
        val todayShortRu = "${cal.get(Calendar.DAY_OF_MONTH)} ${monthNamesRu[currentMonthZero].lowercase()}"
        val todayShortEn = "${monthNamesEn[currentMonthZero]} ${cal.get(Calendar.DAY_OF_MONTH)}"
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "day_$todayIso",
                periodType = AnalysisPeriodType.DAILY,
                titleEn = "Today ($todayShortEn)",
                titleRu = "Сегодня ($todayShortRu)",
                subtitleEn = "Gemini AI reflection on today's state",
                subtitleRu = "Разбор состояния за день от Gemini AI",
                entriesCount = todayEntries.size,
                isEndOfPeriodHighlight = true
            )
        )

        // 1b. Extra date option if clicked from calendar or entry
        if (extraDateIso != null && extraDateIso != todayIso) {
            val extraEntries = unlocked.filter { it.dateIso == extraDateIso }
            val extraCal = DateAndPhotoUtils.parseIsoToCalendar(extraDateIso)
            val extraMonth = extraCal.get(Calendar.MONTH)
            val fullRu = "${extraCal.get(Calendar.DAY_OF_MONTH)} ${monthNamesRu[extraMonth].lowercase()} ${extraCal.get(Calendar.YEAR)}"
            val fullEn = "${monthNamesEn[extraMonth]} ${extraCal.get(Calendar.DAY_OF_MONTH)}, ${extraCal.get(Calendar.YEAR)}"
            val dayCount = extraEntries.size
            result.add(
                AvailableAnalysisPeriod(
                    periodKey = "day_$extraDateIso",
                    periodType = AnalysisPeriodType.DAILY,
                    titleEn = fullEn,
                    titleRu = fullRu,
                    subtitleEn = "Day reflection ($dayCount entries)",
                    subtitleRu = "Разбор за день ($fullRu, записей: $dayCount)",
                    entriesCount = dayCount,
                    isEndOfPeriodHighlight = false
                )
            )
        }

        // 2. Weekly (Last 7 Days)
        val sevenDaysEntries = filterEntriesForPeriod(unlocked, AnalysisPeriodType.WEEKLY, todayIso = todayIso)
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "weekly_7d",
                periodType = AnalysisPeriodType.WEEKLY,
                titleEn = "Last 7 Days",
                titleRu = "Последние 7 дней",
                subtitleEn = "Weekly rhythm & emotional dynamics",
                subtitleRu = "Недельный ритм и динамика за 7 дней",
                entriesCount = sevenDaysEntries.size,
                isEndOfPeriodHighlight = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            )
        )

        // 3. Current Month
        val currentMonthEntries = unlocked.filter { it.dateIso.startsWith(currentYearMonth) }
        val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isEndOfMonthNow = dayOfMonth >= (maxDaysInCurrentMonth - 2)
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "month_$currentYearMonth",
                periodType = AnalysisPeriodType.MONTHLY,
                titleEn = "${monthNamesEn[currentMonthZero]} $currentYear",
                titleRu = "${monthNamesRu[currentMonthZero]} $currentYear",
                subtitleEn = if (isEndOfMonthNow) "Month closing reflection (Gemini AI)" else "Month to date (Gemini AI)",
                subtitleRu = if (isEndOfMonthNow) "Итоги месяца (Gemini AI)" else "Текущий месяц (Gemini AI)",
                entriesCount = currentMonthEntries.size,
                isEndOfPeriodHighlight = isEndOfMonthNow
            )
        )

        // 4. Last 30 Days rolling window
        val thirtyDaysEntries = filterEntriesForPeriod(unlocked, AnalysisPeriodType.LAST_30_DAYS, todayIso = todayIso)
        result.add(
            AvailableAnalysisPeriod(
                periodKey = "rolling_30d",
                periodType = AnalysisPeriodType.LAST_30_DAYS,
                titleEn = "Last 30 Days",
                titleRu = "Последние 30 дней",
                subtitleEn = "Deep perspective & habits across a month",
                subtitleRu = "Глубокий срез привычек за 30 дней",
                entriesCount = thirtyDaysEntries.size,
                isEndOfPeriodHighlight = false
            )
        )

        // 5. Yearly / Year in Review
        val yearsWithEntries = unlocked.mapNotNull {
            it.dateIso.take(4).toIntOrNull()
        }.toSet() + currentYear

        val isDecember = currentMonthZero == Calendar.DECEMBER
        yearsWithEntries.sortedDescending().take(2).forEach { yr ->
            val countForYr = unlocked.count { it.dateIso.startsWith(yr.toString()) }
            if (countForYr > 0 || yr == currentYear) {
                result.add(
                    AvailableAnalysisPeriod(
                        periodKey = "year_$yr",
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

    fun filterEntriesForPeriod(
        allEntries: List<DiaryEntry>,
        periodType: AnalysisPeriodType,
        periodKey: String = "",
        todayIso: String = DateAndPhotoUtils.todayIso()
    ): List<DiaryEntry> {
        val unlocked = unlockedEntriesOnly(allEntries, todayIso)
        return when (periodType) {
            AnalysisPeriodType.DAILY -> {
                val targetDate = if (periodKey.startsWith("day_")) periodKey.removePrefix("day_") else todayIso
                unlocked.filter { it.dateIso == targetDate }
            }
            AnalysisPeriodType.WEEKLY -> {
                val cal = DateAndPhotoUtils.parseIsoToCalendar(todayIso)
                val cutoff = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    add(Calendar.DAY_OF_YEAR, -6)
                }
                val cutoffIso = DateAndPhotoUtils.formatCalendarToIso(cutoff)
                unlocked.filter { it.dateIso in cutoffIso..todayIso }
            }
            AnalysisPeriodType.MONTHLY -> {
                val ym = if (periodKey.startsWith("month_")) periodKey.removePrefix("month_") else todayIso.take(7)
                unlocked.filter { it.dateIso.startsWith(ym) }
            }
            AnalysisPeriodType.LAST_30_DAYS -> {
                val cal = DateAndPhotoUtils.parseIsoToCalendar(todayIso)
                val cutoff = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    add(Calendar.DAY_OF_YEAR, -29)
                }
                val cutoffIso = DateAndPhotoUtils.formatCalendarToIso(cutoff)
                unlocked.filter { it.dateIso in cutoffIso..todayIso }
            }
            AnalysisPeriodType.YEARLY_REVIEW -> {
                val yr = if (periodKey.startsWith("year_")) periodKey.removePrefix("year_") else todayIso.take(4)
                unlocked.filter { it.dateIso.startsWith(yr) }
            }
        }.sortedBy { it.dateIso }
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
                dayEntries.groupBy { it.mood.name }.maxByOrNull { it.value.size }?.key
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

object PsychologicalAiAnalyzer {
    private const val TAG = "PsychologicalAiAnalyzer"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun generateReport(
        context: Context,
        period: AvailableAnalysisPeriod,
        allEntries: List<DiaryEntry>,
        lang: AppLanguage = AppLanguage.RU,
        onProgress: (Int, String, String) -> Unit = { _, _, _ -> }
    ): Result<AiPsychologicalReport> {
        val effectiveEntries = PeriodInsightDetector.filterEntriesForPeriod(
            allEntries = allEntries,
            periodType = period.periodType,
            periodKey = period.periodKey
        )

        if (effectiveEntries.isEmpty()) {
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
            if (lang == AppLanguage.RU) "Анализ ${effectiveEntries.size} записей дневника..." else "Processing ${effectiveEntries.size} entries..."
        )
        delay(250)

        // Stage 2: Aggregation
        val weekdayBreakdown = PeriodInsightDetector.computeWeekdayBreakdown(effectiveEntries)
        val dominantMood = effectiveEntries.groupBy { it.mood }
            .maxByOrNull { it.value.size }?.key ?: BotanicalMood.BALANCE
        val avgIntensity = effectiveEntries.map { it.moodIntensity }.average().toFloat()
        val topTags = effectiveEntries.flatMap { it.tags }
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
            if (lang == AppLanguage.RU) "Генерация теплого психологического анализа..." else "Generating empathetic reflections..."
        )

        val prompt = buildPsychologistPrompt(
            period = period,
            entries = effectiveEntries,
            dominantMood = dominantMood,
            avgIntensity = avgIntensity,
            topTags = topTags,
            weekdayBreakdown = weekdayBreakdown,
            lang = lang
        )

        val geminiResult = callGeminiWithFallback(prompt)

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
                entriesCount = effectiveEntries.size,
                avgIntensity = avgIntensity,
                dominantMoodKey = dominantMood.name,
                topTags = topTags,
                weekdayBreakdown = weekdayBreakdown,
                lang = lang
            )
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
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        var lastException: Throwable? = null

        val responseSchema = Schema.obj(
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
                        responseSchema = responseSchema
                    }
                )

                val response = withTimeout(35000L) {
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

    private fun buildPsychologistPrompt(
        period: AvailableAnalysisPeriod,
        entries: List<DiaryEntry>,
        dominantMood: BotanicalMood,
        avgIntensity: Float,
        topTags: List<String>,
        weekdayBreakdown: List<WeekdayMoodSummary>,
        lang: AppLanguage
    ): String {
        val isRu = lang == AppLanguage.RU
        val entriesText = buildString {
            entries.forEachIndexed { i, e ->
                appendLine("Запись #${i + 1} [Дата: ${e.dateIso}]:")
                appendLine("  Настроение: ${e.mood.titleRu} | Интенсивность: ${e.moodIntensity}/5")
                if (e.tags.isNotEmpty()) appendLine("  Теги: ${e.tags.joinToString(", ")}")
                if (!e.title.isNullOrBlank()) appendLine("  Заголовок: ${e.title}")
                appendLine("  Текст: ${e.content}")
                if (!e.reflectionAnswer.isNullOrBlank()) {
                    appendLine("  Ответ на вопрос дня: ${e.reflectionAnswer}")
                }
                appendLine()
            }
        }

        val weekdaySummary = weekdayBreakdown.filter { it.entriesCount > 0 }
            .joinToString("; ") { "${it.fullLabel(lang)}: записей=${it.entriesCount}, ср.интенсивность=${String.format("%.1f", it.averageIntensity)}" }

        return if (isRu) {
            """
            Ты — внимательный, теплый, мудрый друг и бережный психологический наставник.
            Пользователь делится с тобой своими личными записями из дневника за период «${period.localizedTitle(lang)}».

            ТВОЯ РОЛЬ И СТИЛЬ:
            1. Разговаривай живым, теплым языком близкого друга, который действительно заботится. Представь, что вы сидите в уютной комнате с чашкой чая и открыто говорите по душам.
            2. Категорически избегай менторского, сурового медицинского тона, не ставь диагнозы и не используй сухой заумный жаргон (никаких «когнитивных диссонансов», «интроектов», «дезадаптивных схем»).
            3. Обращайся к пользователю мягко и на «ты».
            4. Опирайся на факты: обязательно упомяни конкретные события, мысли, радости или переживания, о которых человек написал в записях. Не делай общих размытых фраз.
            5. Замечай «тихие победы»: даже если период был сложным, найди то, в чем человек молодец и о чем стоит себе напомнить (выделил время на отдых, честно выразил эмоции, преодолел трудный момент).
            6. Дай 2-3 практичных, ненавязчивых дружеских совета или вопроса для размышления.

            СТАТИСТИКА:
            - Записей: ${entries.size}
            - Преобладающее состояние: ${dominantMood.titleRu} (${dominantMood.subtitleRu})
            - Средняя интенсивность: ${String.format("%.1f", avgIntensity)} / 5.0
            - Топ-теги: ${if (topTags.isNotEmpty()) topTags.joinToString(", ") else "нет тегов"}
            - Дни недели: $weekdaySummary

            ЗАПИСИ ПОЛЬЗОВАТЕЛЯ:
            $entriesText

            Сформируй ответ строго в формате JSON со следующими полями:
            {
              "emotionalHeadline": "Теплый, цепляющий заголовок в одно предложение, отражающий суть периода",
              "warmLetter": "Искреннее, глубокое письмо пользователю от лица понимающего друга (3-4 абзаца с поддержкой, отсылками к его событиям и теплым признанием его пути)",
              "emotionalArcSummary": "Описание динамики чувств простыми живыми словами (что происходило с настроением, как оно менялось)",
              "energySources": ["2-4 конкретных источника радости, сил или вдохновения из записей"],
              "stressTriggers": ["1-3 вещи или ситуации, которые вызывали тревогу, усталость или напряжение"],
              "cognitivePatterns": ["2-3 повторяющиеся мысли, привычки или установки человека, замеченные в дневнике"],
              "quietWins": ["2-3 тихие, но важные победы человека за это время"],
              "weekdayAndTagInsights": "Короткое наблюдение о днях недели, привычках или тегах (например, в какие дни было легче, а когда требовался отдых)",
              "gentleAdviceAndQuestions": ["2-3 мягких дружеских совета или вопроса для заботы о себе"]
            }
            """.trimIndent()
        } else {
            """
            You are a warm, empathetic, and thoughtful friend and supportive psychological mentor.
            The user is sharing their personal diary entries for the period: "${period.localizedTitle(lang)}".

            STYLE:
            - Speak warmly as a caring close friend having a comforting conversation over tea.
            - No clinical jargon, no cold diagnoses.
            - Reference specific real details and events from their entries.
            - Notice quiet victories and progress.
            - Offer 2-3 gentle suggestions or reflective questions.

            ENTRIES:
            $entriesText

            Output strictly JSON matching the required schema.
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
            topTags = topTags,
            weekdayBreakdown = weekdayBreakdown,
            createdAtMillis = System.currentTimeMillis(),
            aiModelBadge = "Gemini • Firebase AI Logic"
        )
    }
}
