package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ai.AvailableAnalysisPeriod
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.data.model.ReflectionPrompts
import com.example.ui.components.BotanicalHeroArtCard
import com.example.ui.components.BotanicalMoodIllustration
import com.example.util.AppLanguage
import com.example.util.DateAndPhotoUtils
import com.example.util.LocalAppLanguage
import com.example.util.tr

@Composable
fun JournalHomeTab(
    entries: List<DiaryEntry>,
    selectedDateIso: String,
    entriesForSelectedDate: List<DiaryEntry>,
    onThisDayEntries: List<DiaryEntry>,
    recommendedAiPeriod: AvailableAnalysisPeriod? = null,
    hasSavedAiReport: Boolean = false,
    onOpenAiAnalysis: (String) -> Unit = {},
    onCreateNewForDate: (String) -> Unit,
    onViewEntry: (DiaryEntry) -> Unit,
    onOpenCalendar: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val latestMood = entriesForSelectedDate.firstOrNull()?.botanicalMood()
        ?: entries.firstOrNull()?.botanicalMood()
        ?: BotanicalMood.CALM_LEAF

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("journal_home_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Clean Hero Summary Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                ) {
                    BotanicalHeroArtCard(
                        modifier = Modifier.fillMaxSize(),
                        height = 168.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = tr("✨ Mindful Journal", "✨ Дневник эмоций"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = DateAndPhotoUtils.formatFullDate(selectedDateIso, lang),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            val moodTitle = latestMood.localizedTitle(lang)
                            val moodSub = latestMood.localizedSubtitle(lang)
                            Text(
                                text = tr(
                                    "Mood: $moodTitle — $moodSub",
                                    "Настроение: $moodTitle — $moodSub"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedButton(
                                onClick = onOpenCalendar,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("hero_calendar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = tr("Change Date", "Выбрать дату"),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr("Change Date", "Выбрать дату"),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        BotanicalMoodIllustration(
                            mood = latestMood,
                            size = 64.dp
                        )
                    }
                }
            }
        }

        // 1b. End-of-Month / December Year-in-Review AI Psychological Banner
        if (recommendedAiPeriod != null) {
            item {
                EndOfPeriodAiBanner(
                    period = recommendedAiPeriod,
                    hasSavedReport = hasSavedAiReport,
                    onOpenPeriodAnalysis = onOpenAiAnalysis
                )
            }
        }

        // 2. "On This Day" Memory Flashback Card (if available)
        if (onThisDayEntries.isNotEmpty()) {
            item {
                OnThisDayFlashbackCard(
                    memories = onThisDayEntries,
                    todayIso = selectedDateIso,
                    onOpenMemory = onViewEntry
                )
            }
        }

        // 3. Selected Date Section Header + Single Primary Entry Action
        item {
            val shortDate = DateAndPhotoUtils.formatShortDate(selectedDateIso, lang)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Entries for $shortDate", "Записи за $shortDate"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (entriesForSelectedDate.isEmpty()) {
                            tr("No notes recorded for this day yet", "За этот день пока нет записей")
                        } else {
                            tr(
                                "${entriesForSelectedDate.size} ${if (entriesForSelectedDate.size == 1) "entry" else "entries"} — tap to read",
                                "Записей: ${entriesForSelectedDate.size} — нажмите, чтобы прочитать"
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Only show top-right action button when entries already exist for the selected date
                if (entriesForSelectedDate.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { onCreateNewForDate(selectedDateIso) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_another_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("New Entry", "Новая запись"))
                    }
                }
            }
        }

        if (entriesForSelectedDate.isEmpty()) {
            item {
                EmptyDayEntryPromptCard(
                    dateIso = selectedDateIso,
                    onWriteClick = { onCreateNewForDate(selectedDateIso) }
                )
            }
        } else {
            items(entriesForSelectedDate, key = { it.id }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onViewEntry(entry) }
                )
            }
        }

        // 4. Recent Timeline Feed (other dates)
        val otherEntries = entries.filter { it.dateIso != selectedDateIso }.take(20)
        if (otherEntries.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr("Recent Timeline", "Недавние записи"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(otherEntries, key = { "recent_${it.id}" }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onViewEntry(entry) }
                )
            }
        }
    }
}

@Composable
fun OnThisDayFlashbackCard(
    memories: List<DiaryEntry>,
    todayIso: String,
    onOpenMemory: (DiaryEntry) -> Unit
) {
    val lang = LocalAppLanguage.current
    val memory = memories.first()
    val yearsAgoLabel = DateAndPhotoUtils.getYearsAgoLabel(memory.dateIso, todayIso, lang)
    val mood = memory.botanicalMood()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMemory(memory) }
            .testTag("on_this_day_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BotanicalMoodIllustration(
                mood = mood,
                size = 52.dp
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tr(
                            "ON THIS DAY • ${yearsAgoLabel.uppercase()}",
                            "В ЭТОТ ДЕНЬ • ${yearsAgoLabel.uppercase()}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = memory.title.ifBlank { DateAndPhotoUtils.formatFullDate(memory.dateIso, lang) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyDayEntryPromptCard(
    dateIso: String,
    onWriteClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val prompt = remember(dateIso, lang) { ReflectionPrompts.getPromptForDate(dateIso, lang) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_day_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = tr("Reflection Prompt", "Вопрос для размышления"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "“$prompt”",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onWriteClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("write_entry_empty_day_button")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Write Entry", "Написать запись"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val mood = entry.botanicalMood()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("diary_entry_card_${entry.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Mood icon + Date & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotanicalMoodIllustration(
                    mood = mood,
                    size = 46.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DateAndPhotoUtils.formatShortDate(entry.dateIso, lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = mood.softBgColor
                        ) {
                            Text(
                                text = mood.localizedTitle(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = mood.primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (entry.title.isNotBlank()) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = tr("Read entry", "Прочитать запись"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content Preview
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Reflection Answer / Gratitude snippet
            if (!entry.reflectionAnswer.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.reflectionAnswer}",
                            style = MaterialTheme.typography.labelMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Photo strip (rendered from Firebase Storage downloadUrls via Coil AsyncImage)
            if (entry.photoUrls.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(entry.photoUrls) { photoUrl ->
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = tr("Attached photo", "Прикрепленное фото"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            // Tags Footer
            if (entry.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Reader Screen (`DiaryEntryReaderScreen`):
 * When the user taps an entry in Calendar, Journal, or Search, they open this clean,
 * distraction-free reading view to revisit their memory — and only enter Edit mode
 * if they explicitly tap the "Edit / Изменить" button!
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryEntryReaderScreen(
    entry: DiaryEntry,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = LocalAppLanguage.current
    val mood = entry.botanicalMood()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("Delete Entry?", "Удалить запись?")) },
            text = {
                Text(
                    tr(
                        "Are you sure you want to delete this journal entry? This action cannot be undone.",
                        "Вы уверены, что хотите удалить эту запись из дневника? Это действие нельзя отменить."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("Delete", "Удалить"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("Cancel", "Отмена"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = DateAndPhotoUtils.formatFullDate(entry.dateIso, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Memory Reader", "Просмотр записи"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "Назад")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("reader_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = tr("Delete", "Удалить"),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    FilledTonalButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("reader_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Edit", "Изменить"), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Mood Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = mood.softBgColor),
                border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BotanicalMoodIllustration(
                        mood = mood,
                        size = 52.dp,
                        showSoftBackground = false
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("MOOD OF THE DAY", "НАСТРОЕНИЕ ДНЯ"),
                            style = MaterialTheme.typography.labelSmall,
                            color = mood.primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mood.localizedTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mood.localizedSubtitle(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Main Story Page Card
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
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (entry.title.isNotBlank()) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3. Reflection Prompt & Answer (if filled)
            if (!entry.reflectionAnswer.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = entry.reflectionPrompt?.takeIf { !it.startsWith("WRITTEN_AT:") }
                                    ?: tr("Reflection & Gratitude", "Мысль и благодарность"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = entry.reflectionAnswer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 4. Photos Gallery
            if (entry.photoUrls.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr("Photos from this day", "Фотографии этого дня"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(entry.photoUrls) { photoUrl ->
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = tr("Memory photo", "Фото воспоминания"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                    }
                }
            }

            // 5. Tags
            if (entry.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entry.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryEntryEditorScreen(
    initialEntry: DiaryEntry?,
    targetDateIso: String,
    isUploadingPhoto: Boolean,
    onUploadPhoto: (Uri, (String) -> Unit) -> Unit,
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
        photoUrls: List<String>
    ) -> Unit,
    onDelete: ((String) -> Unit)?,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    val lang = LocalAppLanguage.current

    val effectiveDateIso = initialEntry?.dateIso ?: targetDateIso
    var title by remember(initialEntry) { mutableStateOf(initialEntry?.title ?: "") }
    var content by remember(initialEntry) { mutableStateOf(initialEntry?.content ?: "") }
    var selectedMood by remember(initialEntry) {
        mutableStateOf(initialEntry?.botanicalMood() ?: BotanicalMood.CALM_LEAF)
    }
    var moodIntensity by remember(initialEntry) {
        mutableStateOf(initialEntry?.moodIntensity ?: 4)
    }
    var reflectionPrompt by remember(initialEntry, effectiveDateIso, lang) {
        mutableStateOf(
            initialEntry?.reflectionPrompt ?: ReflectionPrompts.getPromptForDate(effectiveDateIso, lang)
        )
    }
    var reflectionAnswer by remember(initialEntry) {
        mutableStateOf(initialEntry?.reflectionAnswer ?: "")
    }
    var tags by remember(initialEntry) { mutableStateOf(initialEntry?.tags ?: emptyList()) }
    var newTagInput by remember { mutableStateOf("") }
    var photoUrls by remember(initialEntry) { mutableStateOf(initialEntry?.photoUrls ?: emptyList()) }
    var showPromptSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && photoUrls.size < 4) {
            onUploadPhoto(uri) { downloadUrl ->
                if (photoUrls.size < 4) {
                    photoUrls = photoUrls + downloadUrl
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (initialEntry == null) {
                                tr("New Entry", "Новая запись")
                            } else {
                                tr("Edit Entry", "Редактирование записи")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateAndPhotoUtils.formatFullDate(effectiveDateIso, lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "Назад")
                        )
                    }
                },
                actions = {
                    if (initialEntry != null && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(initialEntry.id) },
                            modifier = Modifier.testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = tr("Delete entry", "Удалить запись"),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onSave(
                                initialEntry?.id,
                                effectiveDateIso,
                                selectedMood,
                                moodIntensity,
                                title,
                                content,
                                reflectionPrompt,
                                reflectionAnswer.takeIf { it.isNotBlank() },
                                tags,
                                photoUrls
                            )
                        },
                        enabled = !isUploadingPhoto,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_save_button")
                    ) {
                        Text(tr("Save", "Сохранить"), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Mood Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("How are you feeling?", "Как вы себя чувствуете?"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BotanicalMood.entries) { mood ->
                        val isSelected = mood == selectedMood
                        val cardBg by animateColorAsState(
                            if (isSelected) mood.softBgColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            label = "moodBg"
                        )
                        Surface(
                            modifier = Modifier
                                .width(148.dp)
                                .clickable {
                                    selectedMood = mood
                                    moodIntensity = mood.defaultIntensity
                                }
                                .testTag("mood_option_${mood.key}"),
                            shape = RoundedCornerShape(18.dp),
                            color = cardBg,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) mood.primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BotanicalMoodIllustration(
                                    mood = mood,
                                    size = 42.dp,
                                    showSoftBackground = !isSelected
                                )
                                Text(
                                    text = mood.localizedTitle(lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = mood.localizedSubtitle(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Reflection Prompt Helper
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tr("Writing Prompts", "Вопросы для размышления"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { showPromptSheet = !showPromptSheet }) {
                            Text(
                                if (showPromptSheet) tr("Hide", "Скрыть")
                                else tr("Browse Prompts", "Выбрать другой")
                            )
                        }
                    }

                    AnimatedVisibility(visible = showPromptSheet) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            for (promptItem in ReflectionPrompts.getPrompts(lang).take(5)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            reflectionPrompt = promptItem
                                            showPromptSheet = false
                                        }
                                ) {
                                    Text(
                                        text = "• $promptItem",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Title & Main Story Text Fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(tr("Entry Title", "Заголовок записи")) },
                placeholder = {
                    Text(
                        tr(
                            "e.g., Quiet morning coffee & fresh air",
                            "Например: Утренний кофе и спокойная прогулка"
                        )
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_title_input")
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(tr("Your Thoughts & Story", "Ваши мысли и события дня")) },
                placeholder = {
                    Text(
                        tr(
                            "Write freely without judgment...",
                            "Запишите все, что важно сохранить в памяти..."
                        )
                    )
                },
                minLines = 6,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_content_input")
            )

            // 4. Reflection / Gratitude Note
            OutlinedTextField(
                value = reflectionAnswer,
                onValueChange = { reflectionAnswer = it },
                label = { Text(reflectionPrompt) },
                placeholder = {
                    Text(
                        tr(
                            "Warm tea, a good conversation, a moment of peace...",
                            "Теплый чай, хороший разговор, минута тишины..."
                        )
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_gratitude_input")
            )

            // 5. Photos Section (WebP compressed & uploaded to Firebase Storage)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Photos (${photoUrls.size}/4)", "Фотографии (${photoUrls.size}/4)"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (photoUrls.size < 4) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isUploadingPhoto,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("editor_add_photo_button")
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr("Uploading WebP...", "Загрузка WebP..."))
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Add Photo", "Добавить фото"))
                            }
                        }
                    }
                }

                if (photoUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(photoUrls.size) { index ->
                            val photoUrl = photoUrls[index]
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = tr("Attached photo", "Прикрепленное фото"),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        photoUrls = photoUrls.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = tr("Remove photo", "Удалить фото"),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Tags Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Tags", "Теги"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = {
                            Text(
                                tr(
                                    "Add tag (e.g., mindfulness, work, travel)",
                                    "Добавить тег (например: уют, прогулка, работа)"
                                )
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_tag_input")
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val formatted = newTagInput.trim().lowercase().removePrefix("#")
                            if (formatted.isNotEmpty() && !tags.contains(formatted) && tags.size < 12) {
                                tags = tags + formatted
                                newTagInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("editor_add_tag_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = tr("Add tag", "Добавить тег")
                        )
                    }
                }

                val suggestedTags = if (lang == AppLanguage.RU) {
                    listOf("благодарность", "семья", "прогулка", "книги", "работа", "мечты")
                } else {
                    listOf("gratitude", "family", "walk", "books", "work", "dreams")
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { tags = tags - tag },
                            label = { Text("#$tag") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = tr("Remove tag", "Удалить тег"),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                    suggestedTags.filter { !tags.contains(it) }.forEach { suggestion ->
                        SuggestionChip(
                            onClick = {
                                if (tags.size < 12) tags = tags + suggestion
                            },
                            label = { Text("+$suggestion") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
