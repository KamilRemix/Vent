package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.data.repository.DiaryRepository
import com.example.util.DateAndPhotoUtils
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
    private val currentUserId: String
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

    // Two-Tier Kotlin Flow Error Handling Architecture
    private val allUserDocumentsState: StateFlow<UiState<List<DiaryEntry>>> = repository
        .observeEntries(currentUserId)
        .map<List<DiaryEntry>, UiState<List<DiaryEntry>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing diary entries", error)
            emit(UiState.Error(error.localizedMessage ?: "Не удалось загрузить записи дневника"))
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
        photosBase64: List<String>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
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
                photosBase64 = photosBase64,
                isExistingDocument = isExisting
            )
            _isSaving.value = false
            result.onSuccess {
                _selectedDateIso.value = dateIso
                _statusBannerMessage.value = "Запись за ${DateAndPhotoUtils.formatShortRussianDate(dateIso)} сохранена"
                onSaved()
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Ошибка сохранения записи"
            }
        }
    }

    fun deleteDiaryEntry(entryId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteEntry(entryId)
            result.onSuccess {
                _statusBannerMessage.value = "Запись удалена"
                onDeleted()
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Не удалось удалить запись"
            }
        }
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.deleteAllEntries()
            _isSaving.value = false
            res.onSuccess {
                _statusBannerMessage.value = "Все записи очищены"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Не удалось очистить записи"
            }
        }
    }

    fun saveFutureLetter(
        unlockDateIso: String,
        mood: BotanicalMood,
        title: String,
        content: String,
        hopeOrGoal: String?,
        photosBase64: List<String>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val writtenToday = DateAndPhotoUtils.todayIso()
            val capsuleId = "capsule_${unlockDateIso}_${java.util.UUID.randomUUID().toString().take(6)}"
            val result = repository.saveEntry(
                entryId = capsuleId,
                dateIso = unlockDateIso,
                mood = mood,
                moodIntensity = mood.defaultIntensity,
                title = title.trim().ifBlank { "Письмо себе в будущее" },
                content = content.trim(),
                reflectionPrompt = "WRITTEN_AT:$writtenToday",
                reflectionAnswer = hopeOrGoal?.trim()?.takeIf { it.isNotBlank() },
                tags = listOf("письмо_в_будущее"),
                photosBase64 = photosBase64,
                isExistingDocument = false
            )
            _isSaving.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Письмо запечатано до ${DateAndPhotoUtils.formatShortRussianDate(unlockDateIso)}"
                onSaved()
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Не удалось сохранить письмо"
            }
        }
    }

    fun unlockLetterEarly(letter: DiaryEntry) {
        if (letter.id.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            val today = DateAndPhotoUtils.todayIso()
            val result = repository.saveEntry(
                entryId = letter.id,
                dateIso = today,
                mood = letter.botanicalMood(),
                moodIntensity = letter.moodIntensity,
                title = letter.title,
                content = letter.content,
                reflectionPrompt = letter.reflectionPrompt ?: "WRITTEN_AT:${letter.writtenDateIso()}",
                reflectionAnswer = letter.reflectionAnswer,
                tags = letter.tags,
                photosBase64 = letter.photosBase64,
                isExistingDocument = true
            )
            _isSaving.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Печать снята — письмо открыто"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Не удалось открыть письмо"
            }
        }
    }

    private companion object {
        const val TAG = "DiaryViewModel"
    }
}
