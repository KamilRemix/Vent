package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiAnalysisUiState
import com.example.data.ai.AiPsychologicalReport
import com.example.data.ai.AnalysisPeriodType
import com.example.data.ai.AvailableAnalysisPeriod
import com.example.data.ai.PeriodInsightDetector
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.ui.components.BotanicalMoodIllustration
import com.example.ui.components.MoodDistributionBar
import com.example.ui.theme.NunitoFontFamily
import com.example.util.AppLanguage
import com.example.util.AppLanguageMode
import com.example.util.DateAndPhotoUtils
import com.example.util.LocalAppLanguage
import com.example.util.tr

@Composable
fun CalendarTab(
    entries: List<DiaryEntry>,
    selectedDateIso: String,
    calendarYear: Int,
    calendarMonthZeroBased: Int,
    onSelectDate: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCreateEntryForDate: (String) -> Unit,
    onViewEntry: (DiaryEntry) -> Unit
) {
    val lang = LocalAppLanguage.current
    val entriesByDate = remember(entries) { entries.groupBy { it.dateIso } }
    val selectedDateEntries = entriesByDate[selectedDateIso] ?: emptyList()
    val monthGridCells = remember(calendarYear, calendarMonthZeroBased) {
        DateAndPhotoUtils.buildMonthGrid(calendarYear, calendarMonthZeroBased)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Month Header Card + Mood Calendar Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Month Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = tr("Previous month", "Предыдущий месяц")
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = DateAndPhotoUtils.formatMonthYearHeader(
                                    calendarYear,
                                    calendarMonthZeroBased,
                                    lang
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Mood Calendar", "Календарь настроений"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = tr("Next month", "Следующий месяц")
                            )
                        }
                    }

                    // Weekday Headers (Mon - Sun / Пн - Вс)
                    val weekdays = if (lang == AppLanguage.RU) {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    } else {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdays.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Days Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        monthGridCells.chunked(7).forEach { weekCells ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                weekCells.forEach { cell ->
                                    if (cell.isCurrentMonth) {
                                        val dayEntries = entriesByDate[cell.dateIso] ?: emptyList()
                                        val primaryMood = dayEntries.firstOrNull()?.botanicalMood()
                                        val isSelected = cell.dateIso == selectedDateIso

                                        CalendarDayItemCell(
                                            dayNumber = cell.dayOfMonth,
                                            mood = primaryMood,
                                            isSelected = isSelected,
                                            isToday = cell.isToday,
                                            onClick = { onSelectDate(cell.dateIso) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .testTag("calendar_day_${cell.dateIso}")
                                        )
                                    } else {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Selected Date Details & Single Action Button (No overlapping FAB!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateAndPhotoUtils.formatFullDate(selectedDateIso, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedDateEntries.isEmpty()) {
                                tr("No entries for this day", "За этот день нет записей")
                            } else {
                                tr(
                                    "${selectedDateEntries.size} ${if (selectedDateEntries.size == 1) "entry" else "entries"} — tap below to read",
                                    "Записей: ${selectedDateEntries.size} — нажмите ниже, чтобы прочитать"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onCreateEntryForDate(selectedDateIso) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("calendar_add_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Write Entry", "Написать"))
                    }
                }
            }
        }

        items(selectedDateEntries, key = { "cal_${it.id}" }) { entry ->
            DiaryEntryCard(
                entry = entry,
                onClick = { onViewEntry(entry) }
            )
        }
    }
}

@Composable
private fun CalendarDayItemCell(
    dayNumber: Int,
    mood: BotanicalMood?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        mood != null -> mood.softBgColor
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.secondary
        mood != null -> mood.primaryColor.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected || isToday) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (mood != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BotanicalMoodIllustration(
                    mood = mood,
                    size = 20.dp,
                    showSoftBackground = false
                )
                Text(
                    text = dayNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchAndTagsTab(
    filteredEntries: List<DiaryEntry>,
    allTags: List<String>,
    searchQuery: String,
    selectedMoodFilter: BotanicalMood?,
    selectedTagFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onMoodFilterChange: (BotanicalMood?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onViewEntry: (DiaryEntry) -> Unit
) {
    val lang = LocalAppLanguage.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Search Input Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        tr(
                            "Search by title, thoughts, notes, or tag...",
                            "Поиск по заголовку, тексту, мыслям или тегу..."
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = tr("Search", "Поиск")
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = tr("Clear search", "Очистить поиск")
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_query_input")
            )
        }

        // 2. Filter Chips (Moods + Tags)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Filter by Mood & Tags", "Фильтр по настроению и тегам"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (searchQuery.isNotBlank() || selectedMoodFilter != null || selectedTagFilter != null) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.testTag("clear_filters_button")
                        ) {
                            Text(tr("Reset All", "Сбросить"))
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BotanicalMood.entries.forEach { mood ->
                        val selected = selectedMoodFilter == mood
                        FilterChip(
                            selected = selected,
                            onClick = { onMoodFilterChange(mood) },
                            label = { Text(mood.localizedTitle(lang)) },
                            modifier = Modifier.testTag("filter_mood_${mood.key}")
                        )
                    }
                }

                if (allTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            val selected = selectedTagFilter == tag
                            FilterChip(
                                selected = selected,
                                onClick = { onTagFilterChange(tag) },
                                label = { Text("#$tag") },
                                modifier = Modifier.testTag("filter_tag_$tag")
                            )
                        }
                    }
                }
            }
        }

        // 3. Results Count
        item {
            Text(
                text = tr(
                    "Found ${filteredEntries.size} ${if (filteredEntries.size == 1) "entry" else "entries"}",
                    "Найдено записей: ${filteredEntries.size}"
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filteredEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tr("No matching entries found", "Записи не найдены"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(
                                "Try adjusting your search keywords or clearing active mood and tag filters.",
                                "Попробуйте изменить поисковый запрос или сбросить выбранные фильтры."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredEntries, key = { "search_${it.id}" }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onViewEntry(entry) }
                )
            }
        }
    }
}

/**
 * Pure Analytics / Insights Tab:
 * Displays ONLY emotional statistics, streaks, and mood distribution.
 * PIN lock, Biometrics, Language, and Google Account Sign-Out live in SettingsScreen.
 */
@Composable
fun AnalyticsTab(
    entries: List<DiaryEntry>,
    availableAiPeriods: List<AvailableAnalysisPeriod> = remember(entries) {
        PeriodInsightDetector.buildAvailablePeriods(entries)
    },
    selectedAiCategory: AnalysisPeriodType = AnalysisPeriodType.MONTHLY,
    selectedAiPeriodKey: String = availableAiPeriods.firstOrNull()?.periodKey.orEmpty(),
    savedAiReports: Map<String, AiPsychologicalReport> = emptyMap(),
    aiUiState: AiAnalysisUiState = AiAnalysisUiState.Idle,
    onSelectAiCategory: (AnalysisPeriodType) -> Unit = {},
    onSelectAiPeriodKey: (String) -> Unit = {},
    onGenerateOrRefreshAiReport: (AvailableAnalysisPeriod) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val moodCounts = remember(entries) {
        val counts = BotanicalMood.entries.associateWith { 0 }.toMutableMap()
        entries.forEach { entry ->
            val mood = entry.botanicalMood()
            counts[mood] = (counts[mood] ?: 0) + 1
        }
        counts
    }

    val totalPhotos = remember(entries) { entries.sumOf { it.photoUrls.size } }
    val uniqueDays = remember(entries) { entries.map { it.dateIso }.distinct().size }
    val dominantMood = remember(moodCounts, entries) {
        if (entries.isEmpty()) null else moodCounts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Key Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = tr("Entries", "Записи"),
                    value = entries.size.toString(),
                    subtitle = tr("Total notes", "Всего в дневнике"),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = tr("Active Days", "Дней"),
                    value = "$uniqueDays",
                    subtitle = tr("Journaled days", "С записями"),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = tr("Photos", "Фото"),
                    value = "$totalPhotos",
                    subtitle = tr("Cloud WebP", "В облаке"),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 1b. Gemini AI Psychological Analysis Hub (Monthly / Last 30 Days / Year in Review)
        item {
            AiPsychologicalHubSection(
                availablePeriods = availableAiPeriods,
                selectedPeriodKey = selectedAiPeriodKey,
                selectedCategory = selectedAiCategory,
                savedReports = savedAiReports,
                aiUiState = aiUiState,
                onSelectCategory = onSelectAiCategory,
                onSelectPeriodKey = onSelectAiPeriodKey,
                onGenerateOrRefreshReport = onGenerateOrRefreshAiReport
            )
        }

        // 2. Dominant Mood Highlight Card
        if (dominantMood != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = dominantMood.softBgColor
                    ),
                    border = BorderStroke(1.dp, dominantMood.primaryColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BotanicalMoodIllustration(
                            mood = dominantMood,
                            size = 54.dp,
                            showSoftBackground = false
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("MOST FREQUENT MOOD", "ПРЕОБЛАДАЮЩЕЕ НАСТРОЕНИЕ"),
                                style = MaterialTheme.typography.labelSmall,
                                color = dominantMood.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dominantMood.localizedTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dominantMood.localizedSubtitle(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Mood Distribution Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = tr("Emotional Balance", "Эмоциональный баланс"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "Distribution of moods across your journal entries",
                                "Распределение состояний по всем вашим записям"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MoodDistributionBar(
                        moodCounts = moodCounts,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BotanicalMood.entries.forEach { mood ->
                            val count = moodCounts[mood] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BotanicalMoodIllustration(
                                    mood = mood,
                                    size = 32.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mood.localizedTitle(lang),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = mood.localizedSubtitle(lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = mood.softBgColor
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = mood.primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Settings Screen opened from the gear IconButton in the main TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    languageMode: AppLanguageMode,
    onLanguageModeChange: (AppLanguageMode) -> Unit,
    isPinEnabled: Boolean,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
    userEmail: String?,
    isSignedInWithGoogle: Boolean = userEmail != null,
    onSignInWithGoogle: () -> Unit = {},
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSetupOrChangePin: () -> Unit,
    onDisablePin: () -> Unit,
    onLockNow: () -> Unit,
    onClearAllEntries: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text(tr("Delete All Entries?", "Удалить все записи?")) },
            text = {
                Text(
                    tr(
                        "This will permanently remove all journal entries and time capsule letters from your cloud account. This action cannot be undone.",
                        "Все записи дневника и письма в будущее будут безвозвратно удалены из облака. Это действие нельзя отменить."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClearAllEntries()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("Delete All", "Удалить всё"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text(tr("Cancel", "Отмена"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("Settings", "Настройки"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "Назад")
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. App Language Selector Card (Auto / System, English, Русский)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("App Language", "Язык приложения"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Automatically detects your phone language or lets you choose manually",
                                        "Автоматически определяет язык телефона или позволяет выбрать вручную"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                AppLanguageMode.SYSTEM to tr("Auto (Phone)", "Авто (Телефон)"),
                                AppLanguageMode.EN to "English",
                                AppLanguageMode.RU to "Русский"
                            )
                            options.forEach { (mode, label) ->
                                val selected = languageMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onLanguageModeChange(mode) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.testTag("lang_mode_${mode.key}")
                                )
                            }
                        }
                    }
                }
            }

            // 2. Privacy, PIN Lock & Biometrics (Fingerprint) Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "PIN & Biometric Protection",
                                        "Защита PIN-кодом и биометрией"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPinEnabled) {
                                        tr(
                                            "4-digit PIN protection is active",
                                            "Защита 4-значным PIN-кодом включена"
                                        )
                                    } else {
                                        tr(
                                            "Set a 4-digit PIN to unlock with PIN or fingerprint",
                                            "Установите 4-значный PIN, чтобы входить по PIN или отпечатку пальца"
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSetupOrChangePin,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("setup_pin_button")
                            ) {
                                Text(
                                    text = if (isPinEnabled) tr("Change PIN", "Изменить PIN")
                                    else tr("Enable PIN", "Включить PIN"),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isPinEnabled) {
                                OutlinedButton(
                                    onClick = onLockNow,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("lock_now_button")
                                ) {
                                    Text(
                                        text = tr("Lock Now", "Заблокировать"),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = onDisablePin,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("disable_pin_button")
                                ) {
                                    Text(
                                        text = tr("Disable PIN", "Отключить"),
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Biometric (Fingerprint) Toggle when PIN is enabled
                        if (isPinEnabled) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = tr(
                                                "Fingerprint / Biometrics",
                                                "Отпечаток пальца / Биометрия"
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isBiometricAvailable) {
                                                tr(
                                                    "Use fingerprint sensor on unlock",
                                                    "Вход по сканеру отпечатка пальца"
                                                )
                                            } else {
                                                tr(
                                                    "Fingerprint not enrolled or unsupported on this device",
                                                    "На этом устройстве отпечаток пальца не настроен или не поддерживается"
                                                )
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isBiometricEnabled && isBiometricAvailable,
                                    onCheckedChange = onBiometricEnabledChange,
                                    enabled = isBiometricAvailable,
                                    modifier = Modifier.testTag("biometric_toggle_switch")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Notifications Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Time Capsule Notifications",
                                        "Уведомления о письмах в будущее"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (notificationsGranted) {
                                        tr(
                                            "Notifications enabled — Android will alert you at the exact unlock hour & minute even when Vent is closed.",
                                            "Уведомления включены — Android пришлет пуш-уведомление в точную минуту открытия письма, даже если приложение закрыто."
                                        )
                                    } else {
                                        tr(
                                            "Allow notifications so Vent can alert you when a future letter unlocks.",
                                            "Разрешите уведомления, чтобы Vent сообщил вам, когда откроется письмо в будущее."
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!notificationsGranted) {
                            OutlinedButton(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(tr("Allow Notifications", "Разрешить уведомления"))
                            }
                        }
                    }
                }
            }

            // 4. Account & Cloud Sync Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("Google Cloud Account", "Аккаунт Google и облако"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSignedInWithGoogle) {
                                        userEmail ?: tr("Signed in with Google", "Вход выполнен через Google")
                                    } else {
                                        tr(
                                            "Not signed in (entries stored on device)",
                                            "Вход не выполнен (записи хранятся на устройстве)"
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            if (isSignedInWithGoogle) {
                                OutlinedButton(
                                    onClick = onSignOut,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = tr("Sign Out", "Выйти"),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr("Sign Out", "Выйти"), maxLines = 1)
                                }
                            } else {
                                Button(
                                    onClick = onSignInWithGoogle,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("settings_google_sign_in_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr("Sign in with Google", "Войти в Google"),
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Clear all cloud entries", "Очистить все записи в облаке"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { showConfirmClearDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("clear_all_entries_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Clear All", "Очистить всё"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = NunitoFontFamily,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
