package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.data.model.ReflectionPrompts
import com.example.ui.components.BotanicalHeroArtCard
import com.example.ui.components.BotanicalMoodIllustration
import com.example.ui.components.BotanicalMoodSelectorGrid
import com.example.util.DateAndPhotoUtils
import java.util.Calendar
import java.util.TimeZone

@Composable
fun JournalHomeTab(
    entries: List<DiaryEntry>,
    selectedDateIso: String,
    onSelectDate: (String) -> Unit,
    onOpenNewEntryForDate: (String) -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onTagClicked: (String) -> Unit,
    onJumpToCalendar: () -> Unit,
    onDeleteEntry: ((String) -> Unit)? = null,
    futureLetters: List<DiaryEntry> = emptyList(),
    onOpenFutureLetters: () -> Unit = {}
) {
    val selectedMonthDay = if (selectedDateIso.length >= 10) selectedDateIso.substring(5, 10) else ""
    val selectedYearPrefix = selectedDateIso.take(4)

    val entriesForSelectedDate = remember(entries, selectedDateIso) {
        entries.filter { it.dateIso == selectedDateIso }
    }

    val onThisDayPastYears = remember(entries, selectedMonthDay, selectedYearPrefix) {
        entries.filter {
            it.monthDay == selectedMonthDay && !it.dateIso.startsWith(selectedYearPrefix)
        }.sortedByDescending { it.dateIso }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("journal_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                BotanicalHeroArtCard(height = 140.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFF2C2623)
                            )
                            Text(
                                text = "Уютное место для ваших мыслей",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5A514B)
                            )
                        }
                        BotanicalMoodIllustration(
                            mood = entriesForSelectedDate.firstOrNull()?.botanicalMood()
                                ?: BotanicalMood.CALM_LEAF,
                            size = 50.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenNewEntryForDate(selectedDateIso) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("hero_write_day_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Записать день", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onJumpToCalendar,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_open_calendar_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Календарь", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Date navigation bar with flexible center text
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onSelectDate(DateAndPhotoUtils.shiftDateIso(selectedDateIso, daysOffset = -1))
                        },
                        modifier = Modifier.testTag("prev_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Предыдущий день"
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onJumpToCalendar() }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedDateIso == DateAndPhotoUtils.todayIso()) "Сегодня"
                            else "Выбранная дата",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            text = DateAndPhotoUtils.formatFullRussianDate(selectedDateIso),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            onSelectDate(DateAndPhotoUtils.shiftDateIso(selectedDateIso, daysOffset = 1))
                        },
                        modifier = Modifier.testTag("next_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Следующий день"
                        )
                    }
                }
            }
        }

        // Entries for the currently selected date
        item {
            if (entriesForSelectedDate.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BotanicalMoodIllustration(mood = BotanicalMood.BLOOM, size = 52.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Страница за ${DateAndPhotoUtils.formatShortRussianDate(selectedDateIso)} свободна",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Отметьте настроение и сохраните важные мысли этого дня.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onOpenNewEntryForDate(selectedDateIso) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_day_create_entry_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Заполнить день")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Записи за выбранный день (${entriesForSelectedDate.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    entriesForSelectedDate.forEach { entry ->
                        DiaryEntryCard(
                            entry = entry,
                            onClick = { onEditEntry(entry) },
                            onTagClicked = onTagClicked,
                            onDelete = if (entry.id.isNotBlank() && onDeleteEntry != null) {
                                { onDeleteEntry(entry.id) }
                            } else null
                        )
                    }
                }
            }
        }

        // Future Letters / Time Capsule Banner Card on Home Screen
        item {
            val todayIso = DateAndPhotoUtils.todayIso()
            val readyCount = futureLetters.count { it.isUnlocked(todayIso) }
            val sealedCount = futureLetters.size - readyCount
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFutureLetters() }
                    .testTag("home_future_letters_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SealedEnvelopeIllustration(
                        isUnlocked = readyCount > 0,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Письмо себе в будущее",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                readyCount > 0 -> "Готово к открытию: $readyCount • Нажмите, чтобы прочитать"
                                sealedCount > 0 -> "Запечатано писем в пути: $sealedCount"
                                else -> "Запечатайте послание себе на месяц или год вперед"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onOpenFutureLetters,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (readyCount > 0) "Открыть" else "Письма", maxLines = 1)
                    }
                }
            }
        }

        // "On This Day in Past Years" Time Capsule Section
        if (onThisDayPastYears.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("on_this_day_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "В этот день в прошлые годы",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Воспоминания за ${DateAndPhotoUtils.formatShortRussianDate(selectedDateIso).substringBeforeLast(" ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        onThisDayPastYears.forEach { memory ->
                            val yearsAgoLabel = DateAndPhotoUtils.getYearsAgoLabel(
                                entryDateIso = memory.dateIso,
                                targetDateIso = selectedDateIso
                            )
                            PastYearMemoryItem(
                                entry = memory,
                                yearsAgoBadge = yearsAgoLabel,
                                onClick = { onEditEntry(memory) }
                            )
                        }
                    }
                }
            }
        }

        // All recent journal pages
        if (entries.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Все ваши записи (${entries.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(entries, key = { it.id.ifBlank { it.dateIso } }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onEditEntry(entry) },
                    onTagClicked = onTagClicked,
                    onDelete = if (entry.id.isNotBlank() && onDeleteEntry != null) {
                        { onDeleteEntry(entry.id) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun PastYearMemoryItem(
    entry: DiaryEntry,
    yearsAgoBadge: String,
    onClick: () -> Unit
) {
    val mood = entry.botanicalMood()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotanicalMoodIllustration(mood = mood, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = mood.softBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = yearsAgoBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = mood.primaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = DateAndPhotoUtils.formatShortRussianDate(entry.dateIso),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.title.ifBlank { mood.titleRu },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onTagClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val mood = entry.botanicalMood()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("diary_entry_card_${entry.dateIso}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    BotanicalMoodIllustration(mood = mood, size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateAndPhotoUtils.formatFullRussianDate(entry.dateIso),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.title.ifBlank { mood.titleRu },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = mood.softBgColor,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = mood.titleRu,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = mood.primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Удалить запись",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (!entry.reflectionPrompt.isNullOrBlank() && !entry.reflectionAnswer.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = entry.reflectionPrompt,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.reflectionAnswer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (entry.photosBase64.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entry.photosBase64.forEach { base64 ->
                        val bitmap = remember(base64) {
                            DateAndPhotoUtils.decodeBase64ToImageBitmap(base64)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Фотография",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 96.dp, height = 76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                            )
                        }
                    }
                }
            }

            if (entry.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onTagClicked(tag) }
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryEntryEditorScreen(
    initialDateIso: String,
    existingEntry: DiaryEntry?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        dateIso: String,
        mood: BotanicalMood,
        moodIntensity: Int,
        title: String,
        content: String,
        reflectionPrompt: String?,
        reflectionAnswer: String?,
        tags: List<String>,
        photosBase64: List<String>
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var dateIso by remember(existingEntry, initialDateIso) {
        mutableStateOf(existingEntry?.dateIso ?: initialDateIso)
    }
    var selectedMood by remember(existingEntry) {
        mutableStateOf(existingEntry?.botanicalMood() ?: BotanicalMood.CALM_LEAF)
    }
    var moodIntensity by remember(existingEntry) {
        mutableIntStateOf(existingEntry?.moodIntensity ?: selectedMood.defaultIntensity)
    }
    var title by remember(existingEntry) {
        mutableStateOf(existingEntry?.title.orEmpty())
    }
    var content by remember(existingEntry) {
        mutableStateOf(existingEntry?.content.orEmpty())
    }
    var promptIndex by remember { mutableIntStateOf(0) }
    var reflectionPrompt by remember(existingEntry) {
        mutableStateOf(
            existingEntry?.reflectionPrompt ?: ReflectionPrompts.allPrompts.first()
        )
    }
    var reflectionAnswer by remember(existingEntry) {
        mutableStateOf(existingEntry?.reflectionAnswer.orEmpty())
    }
    val tags = remember(existingEntry) {
        mutableStateListOf<String>().apply {
            addAll(existingEntry?.tags.orEmpty())
        }
    }
    var customTagInput by remember { mutableStateOf("") }
    val photosBase64 = remember(existingEntry) {
        mutableStateListOf<String>().apply {
            addAll(existingEntry?.photosBase64.orEmpty())
        }
    }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        uris.forEach { uri ->
            if (photosBase64.size < 4) {
                val encoded = DateAndPhotoUtils.compressUriToBase64(context, uri)
                if (encoded != null) {
                    photosBase64.add(encoded)
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val cal = DateAndPhotoUtils.parseIsoToCalendar(dateIso)
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                12,
                0,
                0
            )
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcCal.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            dateIso = DateAndPhotoUtils.calendarToIso(pickedUtc)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Unbreakable, non-overflowing Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("editor_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Text(
                    text = if (existingEntry == null) "Новая запись" else "Редактирование",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )
                if (existingEntry != null && onDelete != null && existingEntry.id.isNotBlank()) {
                    IconButton(
                        onClick = { onDelete(existingEntry.id) },
                        modifier = Modifier.testTag("editor_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Удалить запись",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Button(
                    onClick = {
                        if (content.isBlank() && title.isBlank() && reflectionAnswer.isBlank()) {
                            validationError = "Напишите хотя бы несколько слов о том, как прошел день"
                        } else {
                            onSave(
                                existingEntry?.id,
                                dateIso,
                                selectedMood,
                                moodIntensity,
                                title.ifBlank { selectedMood.titleRu },
                                content.ifBlank { reflectionAnswer.ifBlank { selectedMood.subtitleRu } },
                                reflectionPrompt,
                                reflectionAnswer.takeIf { it.isNotBlank() },
                                tags.toList(),
                                photosBase64.toList()
                            )
                        }
                    },
                    enabled = !isSaving,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("editor_save_button")
                ) {
                    Text(if (isSaving) "..." else "Сохранить", maxLines = 1)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 640.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Date selector card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true }
                        .testTag("editor_date_picker_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Дата записи",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateAndPhotoUtils.formatFullRussianDate(dateIso),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AssistChip(
                            onClick = { showDatePickerDialog = true },
                            label = { Text("Выбрать") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                // 2. Clean mood selector (No weird biology!)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Настроение дня",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedMood.titleRu} — ${selectedMood.subtitleRu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = selectedMood.primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    BotanicalMoodSelectorGrid(
                        selectedMood = selectedMood,
                        onMoodSelected = { mood ->
                            selectedMood = mood
                            moodIntensity = mood.defaultIntensity
                        }
                    )
                }

                // 3. Emotional energy slider
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Уровень энергии и комфорта",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "$moodIntensity из 5",
                                style = MaterialTheme.typography.titleSmall,
                                color = selectedMood.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = moodIntensity.toFloat(),
                            onValueChange = { moodIntensity = it.toInt().coerceIn(1, 5) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.testTag("mood_intensity_slider")
                        )
                    }
                }

                // 4. Title & Free-form journal text
                AnimatedVisibility(visible = validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Главный момент или заголовок") },
                    placeholder = { Text("Например: Тихий вечер, прогулка в парке") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_title_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        validationError = null
                    },
                    label = { Text("Как прошел ваш день?") },
                    placeholder = { Text("Напишите свои мысли, события, впечатления…") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_content_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // 5. Psychological Reflection Prompt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Вопрос для размышления",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = {
                                    promptIndex = (promptIndex + 1) % ReflectionPrompts.allPrompts.size
                                    reflectionPrompt = ReflectionPrompts.allPrompts[promptIndex]
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.testTag("next_reflection_prompt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Другой", maxLines = 1)
                            }
                        }
                        Text(
                            text = reflectionPrompt,
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = reflectionAnswer,
                            onValueChange = {
                                reflectionAnswer = it
                                validationError = null
                            },
                            label = { Text("Ваш ответ") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reflection_answer_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 6. Photo Memories
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Фотографии дня (${photosBase64.size}/4)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = photosBase64.size < 4,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить", maxLines = 1)
                            }
                        }

                        if (photosBase64.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                photosBase64.forEachIndexed { idx, b64 ->
                                    val bmp = remember(b64) {
                                        DateAndPhotoUtils.decodeBase64ToImageBitmap(b64)
                                    }
                                    Box(modifier = Modifier.size(96.dp)) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp,
                                                contentDescription = "Фотография",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                        IconButton(
                                            onClick = { photosBase64.removeAt(idx) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Удалить фото",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Tags Manager
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Теги дня",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customTagInput,
                                onValueChange = { customTagInput = it },
                                label = { Text("Новый тег") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_tag_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilledTonalButton(
                                onClick = {
                                    val clean = customTagInput.trim().removePrefix("#").lowercase()
                                    if (clean.isNotBlank() && !tags.contains(clean) && tags.size < 15) {
                                        tags.add(clean)
                                        customTagInput = ""
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.testTag("add_custom_tag_button")
                            ) {
                                Text("Добавить", maxLines = 1)
                            }
                        }

                        if (tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tags.forEach { tag ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { tags.remove(tag) },
                                        label = { Text("#$tag  ✕") },
                                        modifier = Modifier.testTag("editor_tag_chip_$tag")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
