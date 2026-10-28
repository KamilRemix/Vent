package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.ui.components.BotanicalMoodIllustration
import com.example.util.DateAndPhotoUtils
import com.example.util.LocalAppLanguage
import com.example.util.tr
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureLettersTab(
    futureLetters: List<DiaryEntry>,
    todayIso: String,
    onComposeLetter: () -> Unit,
    onOpenUnlockedLetter: (DiaryEntry) -> Unit,
    onDeleteLetter: (String) -> Unit
) {
    val lang = LocalAppLanguage.current

    // Refresh current time tick every 15 seconds so minute-level letters unlock live on screen
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowTick = System.currentTimeMillis()
        }
    }

    val unlockedLetters = remember(futureLetters, todayIso, nowTick) {
        futureLetters.filter { it.isUnlocked(todayIso) }
            .sortedByDescending { it.dateIso }
    }
    val sealedLetters = remember(futureLetters, todayIso, nowTick) {
        futureLetters.filter { !it.isUnlocked(todayIso) }
            .sortedBy { it.unlockTimestampMillis() }
    }

    var selectedSealedLetter by remember { mutableStateOf<DiaryEntry?>(null) }

    // Sealed Letter Dialog: Strictly NO early unlock or content spoilers!
    selectedSealedLetter?.let { sealed ->
        val writtenDate = DateAndPhotoUtils.formatFullDate(sealed.writtenDateIso(), lang)
        val unlockDate = DateAndPhotoUtils.formatFullDate(sealed.dateIso, lang)
        val unlockTime = sealed.unlockTimeHhMm()
        val exactCountdown = DateAndPhotoUtils.formatExactCountdown(
            unlockDateIso = sealed.dateIso,
            unlockHourOfDay = sealed.unlockHour(),
            unlockMinute = sealed.unlockMinute(),
            lang = lang
        )

        AlertDialog(
            onDismissRequest = { selectedSealedLetter = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = tr("Sealed Letter", "Письмо запечатано"),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    tr("Time Capsule Sealed", "Письмо запечатано"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            "This letter is securely sealed until its scheduled unlock date and time. To preserve the surprise for your future self, its title and contents remain hidden.",
                            "Это письмо надежно запечатано до назначенной даты и времени открытия. Чтобы сохранить интригу для вас в будущем, заголовок и текст письма скрыты."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr("Sent on: $writtenDate", "Отправлено: $writtenDate"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = tr(
                                    "Unlocks on: $unlockDate at $unlockTime",
                                    "Откроется: $unlockDate в $unlockTime"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = exactCountdown,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedSealedLetter = null },
                    modifier = Modifier.testTag("dismiss_sealed_dialog_button")
                ) {
                    Text(tr("Got It", "Понятно"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteLetter(sealed.id)
                        selectedSealedLetter = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("Delete", "Удалить"))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("future_letters_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Time Capsule Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.HourglassTop,
                                        contentDescription = tr("Time Capsule", "Капсула времени"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("TIME CAPSULE", "КАПСУЛА ВРЕМЕНИ"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr("Letters to Your Future Self", "Письма себе в будущее"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = tr(
                                "Send a message, wish, or reminder into the future. Pick the exact day and time — your letter stays sealed until that moment, and Android will send you a push notification even if the app is closed.",
                                "Отправьте послание, мечту или напоминание в будущее. Выберите точный день и время — письмо будет запечатано до этой минуты, а Android пришлет уведомление, даже если приложение закрыто."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onComposeLetter,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("compose_future_letter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    tr("Write Future Letter", "Написать письмо в будущее"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Stats Pill Row
        if (futureLetters.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "${unlockedLetters.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr("Ready to Read", "Открыто"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "${sealedLetters.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr("Sealed", "Запечатано"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Empty State with Botanical Wax-Sealed Envelope Vector Illustration & Soft Gradient
        if (futureLetters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SealedLetterBotanicalIllustration(
                                modifier = Modifier.size(116.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tr("No Time Capsule Letters Yet", "Пока нет писем в будущее"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tr(
                                    "Write a letter to yourself 1 month, 6 months, or 1 year from now. It will stay sealed with a botanical wax stamp until the exact day and minute you choose.",
                                    "Напишите письмо себе через месяц, полгода или год. Оно останется надежно запечатанным до выбранного дня и минуты."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            FilledTonalButton(
                                onClick = onComposeLetter,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditCalendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    tr("Create First Time Capsule", "Создать первое письмо"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Unlocked Letters Section
        if (unlockedLetters.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tr(
                            "Unlocked Letters (${unlockedLetters.size})",
                            "Открытые письма (${unlockedLetters.size})"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(unlockedLetters, key = { it.id }) { letter ->
                UnlockedFutureLetterCard(
                    letter = letter,
                    onClick = { onOpenUnlockedLetter(letter) }
                )
            }
        }

        // 5. Sealed Letters Section (Strictly NO title/content spoilers!)
        if (sealedLetters.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tr(
                            "Sealed in Time Capsule (${sealedLetters.size})",
                            "Запечатаны в капсуле времени (${sealedLetters.size})"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(sealedLetters, key = { it.id }) { letter ->
                SealedFutureLetterCard(
                    letter = letter,
                    onClick = { selectedSealedLetter = letter }
                )
            }
        }
    }
}

@Composable
fun UnlockedFutureLetterCard(
    letter: DiaryEntry,
    onClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val mood = letter.botanicalMood()
    val writtenDate = DateAndPhotoUtils.formatShortDate(letter.writtenDateIso(), lang)
    val unlockedDate = DateAndPhotoUtils.formatShortDate(letter.dateIso, lang)
    val unlockTime = letter.unlockTimeHhMm()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("unlocked_letter_card_${letter.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = tr(
                                "Unlocked • $unlockedDate $unlockTime",
                                "Открыто • $unlockedDate $unlockTime"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = tr("Sent on $writtenDate", "Отправлено $writtenDate"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotanicalMoodIllustration(
                    mood = mood,
                    size = 44.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = letter.title.ifBlank { tr("Letter from the Past", "Письмо из прошлого") },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = letter.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Open Letter →", "Прочитать письмо →"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * SealedFutureLetterCard:
 * Strictly displays ONLY:
 * - Status "Sealed" / "Запечатано"
 * - Sent date ("Sent on [date]" / "Отправлено [date]")
 * - Unlock date & time + exact countdown
 * NEVER shows `letter.title` or `letter.content` preview!
 */
@Composable
fun SealedFutureLetterCard(
    letter: DiaryEntry,
    onClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val writtenDate = DateAndPhotoUtils.formatShortDate(letter.writtenDateIso(), lang)
    val unlockDate = DateAndPhotoUtils.formatShortDate(letter.dateIso, lang)
    val unlockTime = letter.unlockTimeHhMm()
    val exactCountdown = DateAndPhotoUtils.formatExactCountdown(
        unlockDateIso = letter.dateIso,
        unlockHourOfDay = letter.unlockHour(),
        unlockMinute = letter.unlockMinute(),
        lang = lang
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("sealed_letter_card_${letter.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = tr("Sealed", "Запечатано"),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tr("Sealed", "Запечатано"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = tr("Sent on $writtenDate", "Отправлено $writtenDate"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = tr(
                        "Unlocks $unlockDate at $unlockTime",
                        "Откроется: $unlockDate в $unlockTime"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = exactCountdown,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FutureLetterComposerScreen(
    todayIso: String,
    isUploadingPhoto: Boolean,
    onUploadPhoto: (Uri, (String) -> Unit) -> Unit,
    onSaveLetter: (
        unlockDateIso: String,
        unlockHour: Int,
        unlockMinute: Int,
        title: String,
        content: String,
        mood: BotanicalMood,
        photoUrls: List<String>
    ) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    val lang = LocalAppLanguage.current

    var unlockDateIso by remember {
        mutableStateOf(DateAndPhotoUtils.shiftMonthsIso(todayIso, 1))
    }
    var unlockHour by remember { mutableStateOf(9) }
    var unlockMinute by remember { mutableStateOf(0) }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf(BotanicalMood.WARM_ROOT) }
    var photoUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && photoUrls.size < 4) {
            onUploadPhoto(uri) { uploadedUrl ->
                if (photoUrls.size < 4) {
                    photoUrls = photoUrls + uploadedUrl
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(unlockDateIso) {
            runCatching {
                LocalDate.parse(unlockDateIso)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            }.getOrDefault(System.currentTimeMillis() + 86_400_000L)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val minDate = LocalDate.parse(todayIso)
                            unlockDateIso = if (picked.isBefore(minDate)) {
                                minDate.toString()
                            } else {
                                picked.toString()
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(tr("Select Date", "Выбрать дату"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(tr("Cancel", "Отмена"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = unlockHour,
            initialMinute = unlockMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    tr("Select Unlock Time", "Выберите время открытия"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        unlockHour = timePickerState.hour
                        unlockMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text(tr("Save Time", "Сохранить время"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(tr("Cancel", "Отмена"))
                }
            }
        )
    }

    val formattedTime = DateAndPhotoUtils.formatTimeHhMm(unlockHour, unlockMinute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tr("Letter to the Future", "Письмо в будущее"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val shortUnlock = DateAndPhotoUtils.formatShortDate(unlockDateIso, lang)
                        Text(
                            text = tr(
                                "Unlocks $shortUnlock at $formattedTime",
                                "Откроется $shortUnlock в $formattedTime"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("cancel_future_letter_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "Назад")
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveLetter(
                                unlockDateIso,
                                unlockHour,
                                unlockMinute,
                                title,
                                content,
                                selectedMood,
                                photoUrls
                            )
                        },
                        enabled = !isUploadingPhoto && (title.isNotBlank() || content.isNotBlank()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("seal_future_letter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Seal Letter", "Запечатать"), fontWeight = FontWeight.SemiBold)
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
            // 1. Unlock Date & Time Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = tr(
                                    "When should this letter unlock?",
                                    "Когда должно открыться письмо?"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            // Line 1: Full date (with day of week) and time
                            Text(
                                text = "${DateAndPhotoUtils.formatFullDate(unlockDateIso, lang)} • $formattedTime",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Line 2: Remaining time in muted color
                            Text(
                                text = DateAndPhotoUtils.formatExactCountdown(
                                    unlockDateIso = unlockDateIso,
                                    unlockHourOfDay = unlockHour,
                                    unlockMinute = unlockMinute,
                                    lang = lang
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val datePresets = listOf(
                        tr("In 1 Week", "Через 1 нед.") to DateAndPhotoUtils.shiftDateIso(todayIso, daysOffset = 7),
                        tr("In 1 Month", "Через 1 мес.") to DateAndPhotoUtils.shiftMonthsIso(todayIso, 1),
                        tr("In 3 Months", "Через 3 мес.") to DateAndPhotoUtils.shiftMonthsIso(todayIso, 3),
                        tr("In 6 Months", "Через 6 мес.") to DateAndPhotoUtils.shiftMonthsIso(todayIso, 6),
                        tr("In 1 Year", "Через 1 год") to DateAndPhotoUtils.shiftDateIso(todayIso, yearsOffset = 1)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick 2-minute test preset so the user can test background push notifications right away!
                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.MINUTE, 2)
                                }
                                unlockDateIso = DateAndPhotoUtils.calendarToIso(cal)
                                unlockHour = cal.get(Calendar.HOUR_OF_DAY)
                                unlockMinute = cal.get(Calendar.MINUTE)
                            },
                            label = { Text(tr("In 2 min", "Через 2 мин.")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        datePresets.forEach { (label, dateStr) ->
                            val selected = unlockDateIso == dateStr
                            FilterChip(
                                selected = selected,
                                onClick = { unlockDateIso = dateStr },
                                label = { Text(label) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }

                        AssistChip(
                            onClick = { showDatePicker = true },
                            label = { Text(tr("Pick Date...", "Выбрать дату...")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("pick_custom_unlock_date_button")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Time Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = tr("Unlock Time", "Время открытия"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Push notification arrives at $formattedTime",
                                        "Уведомление придет ровно в $formattedTime"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pick_custom_unlock_time_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(formattedTime, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Mood Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("How are you feeling as you write this?", "С каким настроением вы пишете письмо?"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BotanicalMood.entries) { mood ->
                        val isSelected = mood == selectedMood
                        val bgColor by animateColorAsState(
                            if (isSelected) mood.softBgColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            label = "letterMoodBg"
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) mood.primaryColor
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clickable { selectedMood = mood }
                                .testTag("letter_mood_${mood.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BotanicalMoodIllustration(
                                    mood = mood,
                                    size = 26.dp,
                                    showSoftBackground = false
                                )
                                Text(
                                    text = mood.localizedTitle(lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Letter Subject & Content
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(tr("Letter Subject", "Тема письма")) },
                placeholder = {
                    Text(
                        tr(
                            "e.g., Read this on a quiet autumn morning...",
                            "Например: Прочитай это в тихий осенний вечер..."
                        )
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("future_letter_title_input")
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(tr("Your Message to the Future", "Ваше послание в будущее")) },
                placeholder = {
                    Text(
                        tr(
                            "Dear Future Me,\n\nRight now I am thinking about... Here is what I hope you remember when you open this...",
                            "Привет, я из будущего!\n\nСейчас я думаю о том, что... Надеюсь, когда ты откроешь это письмо, ты вспомнишь..."
                        )
                    )
                },
                minLines = 7,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("future_letter_content_input")
            )

            // 4. Optional Photos (Uploaded to Firebase Storage as WebP)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Attach Photos (${photoUrls.size}/4)", "Фотографии (${photoUrls.size}/4)"),
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr("Uploading...", "Загрузка..."))
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
                            val url = photoUrls[index]
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = url,
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureLetterReaderDialog(
    letter: DiaryEntry,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val mood = letter.botanicalMood()
    val writtenDate = DateAndPhotoUtils.formatFullDate(letter.writtenDateIso(), lang)
    val unlockedDate = DateAndPhotoUtils.formatFullDate(letter.dateIso, lang)
    val unlockTime = letter.unlockTimeHhMm()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotanicalMoodIllustration(
                    mood = mood,
                    size = 44.dp
                )
                Column {
                    Text(
                        text = letter.title.ifBlank { tr("Letter from the Past", "Письмо из прошлого") },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Sent on $writtenDate", "Отправлено $writtenDate"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        val moodLabel = mood.localizedTitle(lang)
                        Text(
                            text = tr(
                                "Unlocked $unlockedDate at $unlockTime • $moodLabel",
                                "Открыто $unlockedDate в $unlockTime • $moodLabel"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = letter.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (letter.photoUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(letter.photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = tr("Photo from the past", "Фото из прошлого"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(tr("Close", "Закрыть"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete(letter.id)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(tr("Delete", "Удалить"))
            }
        }
    )
}

@Composable
fun SealedLetterBotanicalIllustration(
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val envelopeFill = MaterialTheme.colorScheme.surface
    val haloStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    val haloEnd = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Soft radial gradient aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(haloStart, haloEnd),
                center = Offset(cx, cy),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(cx, cy)
        )

        // 2. Decorative botanical leaves emerging behind the envelope
        val leftLeaf = Path().apply {
            moveTo(cx - w * 0.18f, cy - h * 0.08f)
            cubicTo(
                cx - w * 0.38f, cy - h * 0.28f,
                cx - w * 0.22f, cy - h * 0.40f,
                cx - w * 0.06f, cy - h * 0.24f
            )
            close()
        }
        drawPath(path = leftLeaf, color = primaryColor.copy(alpha = 0.28f))
        drawPath(
            path = leftLeaf,
            color = primaryColor,
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val rightLeaf = Path().apply {
            moveTo(cx + w * 0.18f, cy - h * 0.08f)
            cubicTo(
                cx + w * 0.38f, cy - h * 0.28f,
                cx + w * 0.22f, cy - h * 0.40f,
                cx + w * 0.06f, cy - h * 0.24f
            )
            close()
        }
        drawPath(path = rightLeaf, color = secondaryColor.copy(alpha = 0.28f))
        drawPath(
            path = rightLeaf,
            color = secondaryColor,
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Envelope body
        val envWidth = w * 0.62f
        val envHeight = h * 0.44f
        val envLeft = cx - envWidth / 2f
        val envTop = cy - envHeight / 2f + h * 0.05f

        drawRoundRect(
            color = envelopeFill,
            topLeft = Offset(envLeft, envTop),
            size = Size(envWidth, envHeight),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(envLeft, envTop),
            size = Size(envWidth, envHeight),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.026f)
        )

        // 4. Envelope top flap lines
        val flapPath = Path().apply {
            moveTo(envLeft + w * 0.02f, envTop + h * 0.02f)
            lineTo(cx, cy + h * 0.07f)
            lineTo(envLeft + envWidth - w * 0.02f, envTop + h * 0.02f)
        }
        drawPath(
            path = flapPath,
            color = primaryColor,
            style = Stroke(width = w * 0.024f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Wax seal stamp in center of envelope
        val sealCenter = Offset(cx, cy + h * 0.07f)
        drawCircle(
            color = tertiaryColor,
            radius = w * 0.095f,
            center = sealCenter
        )
        drawCircle(
            color = envelopeFill,
            radius = w * 0.072f,
            center = sealCenter,
            style = Stroke(width = w * 0.014f)
        )
    }
}

