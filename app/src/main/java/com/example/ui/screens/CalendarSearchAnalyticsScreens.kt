package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.ui.components.BotanicalMoodIllustration
import com.example.ui.viewmodel.MoodAnalyticsData
import com.example.util.DateAndPhotoUtils

@Composable
fun CalendarTab(
    entries: List<DiaryEntry>,
    calendarYear: Int,
    calendarMonthZeroBased: Int,
    selectedDateIso: String,
    onSelectDate: (String) -> Unit,
    onNavigateMonth: (Int) -> Unit,
    onNavigateYear: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenNewEntryForDate: (String) -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onTagClicked: (String) -> Unit
) {
    val cells = remember(calendarYear, calendarMonthZeroBased) {
        DateAndPhotoUtils.buildMonthGrid(calendarYear, calendarMonthZeroBased)
    }

    val entriesByDate = remember(entries) {
        entries.groupBy { it.dateIso }
    }

    val selectedDayEntries = entriesByDate[selectedDateIso].orEmpty()
    val selectedMonthDay = if (selectedDateIso.length >= 10) selectedDateIso.substring(5, 10) else ""
    val selectedYearPrefix = selectedDateIso.take(4)
    val sameDayOtherYears = remember(entries, selectedMonthDay, selectedYearPrefix) {
        entries.filter {
            it.monthDay == selectedMonthDay && !it.dateIso.startsWith(selectedYearPrefix)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Календарь записей",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Выберите день для просмотра или создания записи",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Year & Month Navigation + Calendar Grid Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Month switcher row (never overflows!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onNavigateMonth(-1) },
                            modifier = Modifier.testTag("calendar_prev_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Предыдущий месяц"
                            )
                        }
                        Text(
                            text = DateAndPhotoUtils.formatMonthYearHeader(
                                calendarYear,
                                calendarMonthZeroBased
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { onNavigateMonth(1) },
                            modifier = Modifier.testTag("calendar_next_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Следующий месяц"
                            )
                        }
                    }

                    // Compact Year Jumpers row (fits comfortably on all screen widths)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { onNavigateYear(-1) },
                            label = { Text("← ${calendarYear - 1}") },
                            modifier = Modifier.testTag("calendar_prev_year_chip")
                        )
                        AssistChip(
                            onClick = onJumpToToday,
                            label = { Text("Сегодня") },
                            modifier = Modifier.testTag("calendar_today_chip")
                        )
                        AssistChip(
                            onClick = { onNavigateYear(1) },
                            label = { Text("${calendarYear + 1} →") },
                            modifier = Modifier.testTag("calendar_next_year_chip")
                        )
                    }

                    // Day of week headers
                    val weekHeaders = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekHeaders.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 6-row Month Grid
                    cells.chunked(7).forEach { weekRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekRow.forEach { cell ->
                                val dayEntries = entriesByDate[cell.dateIso].orEmpty()
                                val primaryMood = dayEntries.firstOrNull()?.botanicalMood()
                                val isSelected = cell.dateIso == selectedDateIso

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.95f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                primaryMood != null -> primaryMood.softBgColor
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = when {
                                                isSelected -> 2.dp
                                                cell.isToday -> 1.5.dp
                                                else -> 0.dp
                                            },
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                cell.isToday -> MaterialTheme.colorScheme.secondary
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSelectDate(cell.dateIso) }
                                        .testTag("calendar_cell_${cell.dateIso}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = cell.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected || primaryMood != null) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                primaryMood != null -> Color(0xFF2C2623)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (primaryMood != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            BotanicalMoodIllustration(
                                                mood = primaryMood,
                                                size = 18.dp,
                                                showSoftBackground = false
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

        // Selected Date Card (clean column layout, button never cut off!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = DateAndPhotoUtils.formatFullRussianDate(selectedDateIso),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedDayEntries.isEmpty())
                            "На этот день пока нет записей"
                        else
                            "Записей за этот день: ${selectedDayEntries.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onOpenNewEntryForDate(selectedDateIso) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_write_for_date_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Записать за этот день")
                    }
                }
            }
        }

        items(selectedDayEntries, key = { it.id.ifBlank { it.dateIso } }) { entry ->
            DiaryEntryCard(
                entry = entry,
                onClick = { onEditEntry(entry) },
                onTagClicked = onTagClicked
            )
        }

        if (sameDayOtherYears.isNotEmpty()) {
            item {
                Text(
                    text = "В этот же день (${selectedMonthDay}) в другие годы:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            items(sameDayOtherYears, key = { "other_year_${it.id}" }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onEditEntry(entry) },
                    onTagClicked = onTagClicked
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchAndTagsTab(
    allEntries: List<DiaryEntry>,
    filteredEntries: List<DiaryEntry>,
    searchQuery: String,
    selectedTag: String?,
    selectedMood: BotanicalMood?,
    onSearchQueryChanged: (String) -> Unit,
    onToggleTag: (String?) -> Unit,
    onToggleMood: (BotanicalMood?) -> Unit,
    onEditEntry: (DiaryEntry) -> Unit
) {
    val allAvailableTags = remember(allEntries) {
        allEntries.flatMap { it.tags }.distinct().sorted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Поиск записей",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty() || selectedTag != null || selectedMood != null) {
                        IconButton(
                            onClick = {
                                onSearchQueryChanged("")
                                onToggleTag(null)
                                onToggleMood(null)
                            },
                            modifier = Modifier.testTag("clear_search_filters_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Сбросить")
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

        // Mood Filter Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Фильтр по настроению",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BotanicalMood.entries.forEach { mood ->
                        val isSelected = selectedMood == mood
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleMood(mood) },
                            label = { Text(mood.titleRu) },
                            leadingIcon = {
                                BotanicalMoodIllustration(
                                    mood = mood,
                                    size = 20.dp,
                                    showSoftBackground = false
                                )
                            },
                            modifier = Modifier.testTag("search_mood_filter_${mood.key}")
                        )
                    }
                }
            }
        }

        // Tags Filter Cloud
        if (allAvailableTags.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Теги (${allAvailableTags.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allAvailableTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTag.equals(tag, ignoreCase = true),
                                onClick = { onToggleTag(tag) },
                                label = { Text("#$tag") },
                                modifier = Modifier.testTag("search_tag_filter_$tag")
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Найдено записей: ${filteredEntries.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (filteredEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BotanicalMoodIllustration(mood = BotanicalMood.MIST_FERN, size = 48.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Попробуйте другое слово или выберите другой тег.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredEntries, key = { it.id.ifBlank { it.dateIso } }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onEditEntry(entry) },
                    onTagClicked = { clickedTag -> onToggleTag(clickedTag) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsAndProfileTab(
    analytics: MoodAnalyticsData,
    userEmail: String?,
    isPinEnabled: Boolean,
    onOpenPinSetup: () -> Unit,
    onLockNow: () -> Unit,
    onClearAllEntries: () -> Unit = {},
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Аналитика и настройки",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Статистика настроения и безопасность дневника",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Всего записей",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${analytics.totalEntries}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Прошлые годы: ${analytics.pastYearsMemoriesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Средний комфорт",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f / 5", analytics.averageHarmony),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = analytics.dominantMood?.let { "Чаще: ${it.titleRu}" } ?: "Нет данных",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Emotional Wave Smooth Canvas Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "График настроения",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Динамика по последним дням (от 1 до 5)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (analytics.recentTrendPoints.size < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Добавьте от 2 записей для построения графика",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val pts = analytics.recentTrendPoints

                            // Horizontal guide lines for 1..5
                            for (level in 1..5) {
                                val y = h - ((level - 1) / 4f) * h
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            val offsets = pts.mapIndexed { idx, (_, value) ->
                                val x = if (pts.size == 1) w / 2f else (idx.toFloat() / (pts.size - 1)) * w
                                val y = h - ((value.coerceIn(1, 5) - 1) / 4f) * h
                                Offset(x, y)
                            }

                            val linePath = Path().apply {
                                moveTo(offsets.first().x, offsets.first().y)
                                for (i in 1 until offsets.size) {
                                    val prev = offsets[i - 1]
                                    val curr = offsets[i]
                                    val midX = (prev.x + curr.x) / 2f
                                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                }
                            }

                            val fillPath = Path().apply {
                                addPath(linePath)
                                lineTo(offsets.last().x, h)
                                lineTo(offsets.first().x, h)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            offsets.forEach { pt ->
                                drawCircle(
                                    color = secondaryColor,
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            analytics.recentTrendPoints.forEach { (dateLabel, _) ->
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mood Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Распределение настроений",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val total = analytics.totalEntries.coerceAtLeast(1)
                    BotanicalMood.entries.forEach { mood ->
                        val count = analytics.moodCounts[mood] ?: 0
                        val fraction = count.toFloat() / total.toFloat()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalMoodIllustration(mood = mood, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mood.titleRu,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$count дн.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = mood.primaryColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = mood.primaryColor,
                                    trackColor = mood.softBgColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Tags
        if (analytics.topResourceTags.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Популярные теги",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            analytics.topResourceTags.forEach { (tag, count) ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "#$tag ($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security & PIN Code Lock Card (Stacked buttons, zero overflow!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPinEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Защита дневника PIN-кодом",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPinEnabled) "PIN-код включен — требуется ввод при запуске"
                                else "Защита не установлена",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenPinSetup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manage_pin_button")
                        ) {
                            Text(
                                text = if (isPinEnabled) "Изменить или отключить PIN" else "Установить PIN-код",
                                maxLines = 1
                            )
                        }
                        if (isPinEnabled) {
                            OutlinedButton(
                                onClick = onLockNow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lock_diary_now_button")
                            ) {
                                Text("Заблокировать сейчас", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Cloud Account & Sign-Out Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Синхронизация с аккаунтом",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail ?: "Google аккаунт подключен",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (analytics.totalEntries > 0) {
                        OutlinedButton(
                            onClick = onClearAllEntries,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_entries_button")
                        ) {
                            Text("Очистить все записи дневника", maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Выйти из аккаунта", maxLines = 1)
                    }
                }
            }
        }
    }
}
