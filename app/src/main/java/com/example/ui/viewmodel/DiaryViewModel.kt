package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAnalysisUiState
import com.example.data.ai.AiPsychologicalReport
import com.example.data.ai.AnalysisPeriodType
import com.example.data.ai.AvailableAnalysisPeriod
import com.example.data.ai.PeriodInsightDetector
import com.example.data.ai.PsychologicalAiAnalyzer
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.data.repository.DiaryRepository
import com.example.data.repository.PhotoStorageRepository
import com.example.notifications.LetterUnlockScheduler
import com.example.util.AppLocaleHolder
import com.example.util.DateAndPhotoUtils
import com.example.util.trText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MoodAnalyticsData(
    val totalEntries: Int = 0,
    val averageHarmony: Float = 0f,
    val dominantMood: BotanicalMood? = null,
    val moodCounts: Map<BotanicalMood, Int> = emptyMap(),
    val recentTrendPoints: List<Pair<String, Int>> = emptyList(),
    val topResourceTags: List<Pair<String, Int>> = emptyList(),
    val pastYearsMemoriesCount: Int = 0
)

class DiaryViewModel(
    private val repository: DiaryRepository,
    private val currentUserId: String,
    private val photoStorageRepository: PhotoStorageRepository = PhotoStorageRepository(),
    private val aiAnalyzer: PsychologicalAiAnalyzer? = null
) : ViewModel() {

    private val _selectedDateIso = MutableStateFlow(DateAndPhotoUtils.todayIso())
    val selectedDateIso: StateFlow<String> = _selectedDateIso.asStateFlow()

    private val initialCal = Calendar.getInstance()
    private val _calendarYear = MutableStateFlow(initialCal.get(Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonthZeroBased = MutableStateFlow(initialCal.get(Calendar.MONTH))
    val calendarMonthZeroBased: StateFlow<Int> = _calendarMonthZeroBased.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _selectedMoodFilter = MutableStateFlow<BotanicalMood?>(null)
    val selectedMoodFilter: StateFlow<BotanicalMood?> = _selectedMoodFilter.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    // Psychological AI Insights State
    private val initialYearMonth = String.format(
        "%04d-%02d",
        initialCal.get(Calendar.YEAR),
        initialCal.get(Calendar.MONTH) + 1
    )
    private val _selectedAiCategory = MutableStateFlow(
        if (initialCal.get(Calendar.MONTH) == Calendar.DECEMBER) {
            AnalysisPeriodType.YEARLY_REVIEW
        } else {
            AnalysisPeriodType.MONTHLY
        }
    )
    val selectedAiCategory: StateFlow<AnalysisPeriodType> = _selectedAiCategory.asStateFlow()

    private val _selectedAiPeriodKey = MutableStateFlow(
        if (initialCal.get(Calendar.MONTH) == Calendar.DECEMBER) {
            "${initialCal.get(Calendar.YEAR)}-YEAR"
        } else {
            initialYearMonth
        }
    )
    val selectedAiPeriodKey: StateFlow<String> = _selectedAiPeriodKey.asStateFlow()

    private val _savedAiReports = MutableStateFlow<Map<String, AiPsychologicalReport>>(
        aiAnalyzer?.loadSavedReports(currentUserId).orEmpty()
    )
    val savedAiReports: StateFlow<Map<String, AiPsychologicalReport>> = _savedAiReports.asStateFlow()

    private val _aiAnalysisUiState = MutableStateFlow<AiAnalysisUiState>(AiAnalysisUiState.Idle)
    val aiAnalysisUiState: StateFlow<AiAnalysisUiState> = _aiAnalysisUiState.asStateFlow()

    // Two-Tier Kotlin Flow Error Handling Architecture
    private val allUserDocumentsState: StateFlow<UiState<List<DiaryEntry>>> = repository
        .observeEntries(currentUserId)
        .map<List<DiaryEntry>, UiState<List<DiaryEntry>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing diary entries", error)
            val lang = AppLocaleHolder.currentLanguage
            emit(
                UiState.Error(
                    error.localizedMessage ?: trText(
                        lang,
                        "Failed to load journal entries",
                        "Не удалось загрузить записи дневника"
                    )
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val entriesState: StateFlow<UiState<List<DiaryEntry>>> = allUserDocumentsState
        .map { state ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> UiState.Success(state.data.filter { !it.isFutureLetter() })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val futureLettersState: StateFlow<List<DiaryEntry>> = allUserDocumentsState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { it.isFutureLetter() }
                ?.sortedBy { it.dateIso }
                .orEmpty()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val filteredSearchEntries: StateFlow<List<DiaryEntry>> = combine(
        entriesState,
        _searchQuery,
        _selectedTagFilter,
        _selectedMoodFilter
    ) { state, query, tag, mood ->
        val all = (state as? UiState.Success)?.data.orEmpty()
        val q = query.trim().removePrefix("#").lowercase()
        all.filter { entry ->
            val matchesQuery = q.isEmpty() ||
                entry.title.lowercase().contains(q) ||
                entry.content.lowercase().contains(q) ||
                entry.reflectionAnswer.orEmpty().lowercase().contains(q) ||
                entry.tags.any { it.lowercase().contains(q) } ||
                entry.dateIso.contains(q)
            val matchesTag = tag == null || entry.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesMood = mood == null || entry.moodKey == mood.key
            matchesQuery && matchesTag && matchesMood
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val analyticsData: StateFlow<MoodAnalyticsData> = entriesState
        .map { state ->
            val entries = (state as? UiState.Success)?.data.orEmpty()
            if (entries.isEmpty()) {
                MoodAnalyticsData()
            } else {
                val avg = entries.map { it.moodIntensity }.average().toFloat()
                val counts = BotanicalMood.entries.associateWith { mood ->
                    entries.count { it.moodKey == mood.key }
                }
                val dominant = counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
                val recentPoints = entries
                    .sortedBy { it.dateIso }
                    .takeLast(12)
                    .map { it.dateIso.takeLast(5) to it.moodIntensity }

                val tagFreq = mutableMapOf<String, Int>()
                entries.forEach { entry ->
                    entry.tags.forEach { tag ->
                        val clean = tag.lowercase()
                        tagFreq[clean] = (tagFreq[clean] ?: 0) + 1
                    }
                }
                val topTags = tagFreq.entries
                    .sortedByDescending { it.value }
                    .take(8)
                    .map { it.key to it.value }

                val currYearPrefix = DateAndPhotoUtils.todayIso().take(4)
                val pastYearCount = entries.count { !it.dateIso.startsWith(currYearPrefix) }

                MoodAnalyticsData(
                    totalEntries = entries.size,
                    averageHarmony = avg,
                    dominantMood = dominant,
                    moodCounts = counts,
                    recentTrendPoints = recentPoints,
                    topResourceTags = topTags,
                    pastYearsMemoriesCount = pastYearCount
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MoodAnalyticsData()
        )

    fun selectDate(dateIso: String) {
        _selectedDateIso.value = dateIso
        val cal = DateAndPhotoUtils.parseIsoToCalendar(dateIso)
        _calendarYear.value = cal.get(Calendar.YEAR)
        _calendarMonthZeroBased.value = cal.get(Calendar.MONTH)
    }

    fun navigateCalendarMonth(deltaMonths: Int) {
        val cal = Calendar.getInstance().apply {
            set(_calendarYear.value, _calendarMonthZeroBased.value, 1, 12, 0, 0)
            add(Calendar.MONTH, deltaMonths)
        }
        _calendarYear.value = cal.get(Calendar.YEAR)
        _calendarMonthZeroBased.value = cal.get(Calendar.MONTH)
    }

    fun navigateCalendarYear(deltaYears: Int) {
        _calendarYear.value = (_calendarYear.value + deltaYears).coerceIn(1990, 2100)
    }

    fun jumpToToday() {
        selectDate(DateAndPhotoUtils.todayIso())
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTagFilter(tag: String?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tag) null else tag
    }

    fun toggleMoodFilter(mood: BotanicalMood?) {
        _selectedMoodFilter.value = if (_selectedMoodFilter.value == mood) null else mood
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun uploadPhotoAsWebp(
        context: Context,
        uri: Uri,
        onUploaded: (String) -> Unit
    ) {
        viewModelScope.launch {
            val lang = AppLocaleHolder.currentLanguage
            _isUploadingPhoto.value = true
            val result = photoStorageRepository.compressAndUploadWebpPhoto(context, uri)
            _isUploadingPhoto.value = false
            result.onSuccess { downloadUrl ->
                onUploaded(downloadUrl)
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: trText(
                    lang,
                    "Failed to upload WebP photo",
                    "Не удалось загрузить фото (WebP)"
                )
            }
        }
    }

    fun saveDiaryEntry(
        existingEntryId: String?,
        dateIso: String,
        mood: BotanicalMood,
        moodIntensity: Int,
        title: String,
        content: String,
        reflectionPrompt: String?,
        reflectionAnswer: String?,
        tags: List<String>,
        photoUrls: List<String>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val lang = AppLocaleHolder.currentLanguage
            _isSaving.value = true
            val isExisting = !existingEntryId.isNullOrBlank()
            val result = repository.saveEntry(
                entryId = existingEntryId,
                dateIso = dateIso,
                mood = mood,
                moodIntensity = moodIntensity,
                title = title,
                content = content,
                reflectionPrompt = reflectionPrompt,
                reflectionAnswer = reflectionAnswer,
                tags = tags,
                photoUrls = photoUrls,
                isExistingDocument = isExisting
            )
            _isSaving.value = false
            result.onSuccess {
                _selectedDateIso.value = dateIso
                val formatted = DateAndPhotoUtils.formatShortDate(dateIso, lang)
                _statusBannerMessage.value = trText(
                    lang,
                    "Entry for $formatted saved",
                    "Запись за $formatted сохранена"
                )
                onSaved()
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: trText(
                    lang,
                    "Failed to save entry",
                    "Не удалось сохранить запись"
                )
            }
        }
    }

    fun deleteDiaryEntry(context: Context? = null, entryId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val lang = AppLocaleHolder.currentLanguage
            val result = repository.deleteEntry(entryId)
            result.onSuccess {
                if (context != null) {
                    LetterUnlockScheduler.cancelUnlockNotification(context, entryId)
                }
                _statusBannerMessage.value = trText(lang, "Entry deleted", "Запись удалена")
                onDeleted()
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: trText(
                    lang,
                    "Failed to delete entry",
                    "Не удалось удалить запись"
                )
            }
        }
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            val lang = AppLocaleHolder.currentLanguage
            _isSaving.value = true
            val res = repository.deleteAllEntries()
            _isSaving.value = false
            res.onSuccess {
                _statusBannerMessage.value = trText(
                    lang,
                    "All journal entries cleared",
                    "Все записи дневника очищены"
                )
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: trText(
                    lang,
                    "Failed to clear entries",
                    "Не удалось очистить записи"
                )
            }
        }
    }

    fun saveFutureLetter(
        context: Context,
        unlockDateIso: String,
        unlockHour: Int,
        unlockMinute: Int,
        mood: BotanicalMood,
        title: String,
        content: String,
        hopeOrGoal: String?,
        photoUrls: List<String>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val lang = AppLocaleHolder.currentLanguage
            _isSaving.value = true
            val writtenToday = DateAndPhotoUtils.todayIso()
            val capsuleId = "capsule_${unlockDateIso}_${UUID.randomUUID().toString().take(6)}"
            val defaultSubject = trText(lang, "Letter to Future Self", "Письмо себе в будущее")
            val metadata = DiaryEntry.buildFutureLetterMetadata(
                writtenDateIso = writtenToday,
                hour = unlockHour,
                minute = unlockMinute
            )
            val result = repository.saveEntry(
                entryId = capsuleId,
                dateIso = unlockDateIso,
                mood = mood,
                moodIntensity = mood.defaultIntensity,
                title = title.trim().ifBlank { defaultSubject },
                content = content.trim(),
                reflectionPrompt = metadata,
                reflectionAnswer = hopeOrGoal?.trim()?.takeIf { it.isNotBlank() },
                tags = listOf("future_letter"),
                photoUrls = photoUrls,
                isExistingDocument = false
            )
            _isSaving.value = false
            result.onSuccess {
                LetterUnlockScheduler.scheduleUnlockNotification(
                    context = context,
                    letterId = capsuleId,
                    unlockDateIso = unlockDateIso,
                    writtenDateIso = writtenToday,
                    unlockHourOfDay = unlockHour,
                    unlockMinute = unlockMinute,
                    replaceExisting = true
                )
                val formattedDate = DateAndPhotoUtils.formatShortDate(unlockDateIso, lang)
                val formattedTime = DateAndPhotoUtils.formatTimeHhMm(unlockHour, unlockMinute)
                _statusBannerMessage.value = trText(
                    lang,
                    "Letter sealed until $formattedDate at $formattedTime",
                    "Письмо запечатано до $formattedDate в $formattedTime"
                )
                onSaved()
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: trText(
                    lang,
                    "Failed to seal letter",
                    "Не удалось запечатать письмо"
                )
            }
        }
    }

    fun selectAiCategory(category: AnalysisPeriodType, allEntries: List<DiaryEntry>) {
        _selectedAiCategory.value = category
        val periods = PeriodInsightDetector.buildAvailablePeriods(allEntries)
        val firstMatch = periods.firstOrNull { it.periodType == category && it.entriesCount > 0 }
            ?: periods.firstOrNull { it.periodType == category }
        if (firstMatch != null) {
            _selectedAiPeriodKey.value = firstMatch.periodKey
        }
        if (_aiAnalysisUiState.value !is AiAnalysisUiState.Generating) {
            _aiAnalysisUiState.value = AiAnalysisUiState.Idle
        }
    }

    fun selectAiPeriodKey(periodKey: String, allEntries: List<DiaryEntry>) {
        _selectedAiPeriodKey.value = periodKey
        val periods = PeriodInsightDetector.buildAvailablePeriods(allEntries)
        periods.firstOrNull { it.periodKey == periodKey }?.let { matched ->
            _selectedAiCategory.value = matched.periodType
        }
        if (_aiAnalysisUiState.value !is AiAnalysisUiState.Generating) {
            _aiAnalysisUiState.value = AiAnalysisUiState.Idle
        }
    }

    fun generateAiPsychologicalReport(
        context: Context,
        period: AvailableAnalysisPeriod,
        allEntries: List<DiaryEntry>
    ) {
        viewModelScope.launch {
            val lang = AppLocaleHolder.currentLanguage
            val analyzer = aiAnalyzer ?: PsychologicalAiAnalyzer(context)
            val periodTitle = period.localizedTitle(lang)
            _selectedAiPeriodKey.value = period.periodKey
            _selectedAiCategory.value = period.periodType
            _aiAnalysisUiState.value = AiAnalysisUiState.Generating(
                periodTitle = periodTitle,
                progressPercent = 8,
                stageTitle = trText(lang, "Starting analysis...", "Запуск психологического разбора..."),
                stageDetail = trText(lang, "Preparing journal context for $periodTitle...", "Подготавливаем записи за период «$periodTitle»...")
            )

            val periodEntries = PeriodInsightDetector.entriesForPeriod(allEntries, period.periodKey)
            val result = analyzer.analyzePeriod(
                userId = currentUserId,
                period = period,
                periodEntries = periodEntries,
                allEntries = allEntries,
                lang = lang,
                onProgress = { percent, stageTitle, stageDetail ->
                    _aiAnalysisUiState.value = AiAnalysisUiState.Generating(
                        periodTitle = periodTitle,
                        progressPercent = percent,
                        stageTitle = stageTitle,
                        stageDetail = stageDetail
                    )
                }
            )

            result.onSuccess { report ->
                val updatedMap = analyzer.loadSavedReports(currentUserId).toMutableMap()
                updatedMap[report.periodKey] = report
                _savedAiReports.value = updatedMap
                _aiAnalysisUiState.value = AiAnalysisUiState.Success(report)
                _statusBannerMessage.value = trText(
                    lang,
                    "Psychological report for $periodTitle is ready",
                    "Психологический разбор за $periodTitle готов"
                )
            }.onFailure { err ->
                val message = err.localizedMessage ?: trText(
                    lang,
                    "Failed to generate psychological analysis",
                    "Не удалось выполнить психологический разбор"
                )
                _aiAnalysisUiState.value = AiAnalysisUiState.Error(message)
            }
        }
    }

    private companion object {
        const val TAG = "DiaryViewModel"
    }
}
