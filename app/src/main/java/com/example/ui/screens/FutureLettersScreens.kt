package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
fun SealedEnvelopeIllustration(
    isUnlocked: Boolean,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isUnlocked) Color(0xFFF7EFE0) else Color(0xFFF5E5E0)
    val envelopeStroke = if (isUnlocked) Color(0xFFB8863B) else Color(0xFFA95C47)
    val sealColor = if (isUnlocked) Color(0xFF4A6B53) else Color(0xFFC87A5B)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.64f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.075f)

            // Envelope body
            val envTop = h * 0.22f
            val envHeight = h * 0.62f
            drawRoundRect(
                color = Color(0xFFFFFBF7),
                topLeft = Offset(w * 0.08f, envTop),
                size = Size(w * 0.84f, envHeight),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
            drawRoundRect(
                color = envelopeStroke,
                topLeft = Offset(w * 0.08f, envTop),
                size = Size(w * 0.84f, envHeight),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                style = stroke
            )

            // Envelope flap
            val flapPath = Path().apply {
                moveTo(w * 0.1f, envTop + h * 0.03f)
                lineTo(w * 0.5f, envTop + envHeight * 0.56f)
                lineTo(w * 0.9f, envTop + h * 0.03f)
            }
            drawPath(path = flapPath, color = envelopeStroke, style = stroke)

            // Wax seal circle in center
            val sealCenter = Offset(w * 0.5f, envTop + envHeight * 0.54f)
            drawCircle(
                color = sealColor,
                radius = w * 0.15f,
                center = sealCenter
            )
            drawCircle(
                color = Color(0xFFFFFBF7),
                radius = w * 0.05f,
                center = sealCenter
            )
        }
    }
}

@Composable
fun FutureLettersTab(
    letters: List<DiaryEntry>,
    onOpenComposer: () -> Unit,
    onUnlockLetterEarly: (DiaryEntry) -> Unit,
    onDeleteLetter: (String) -> Unit
) {
    val todayIso = remember { DateAndPhotoUtils.todayIso() }
    val unlockedLetters = remember(letters, todayIso) {
        letters.filter { it.isUnlocked(todayIso) }.sortedByDescending { it.dateIso }
    }
    val sealedLetters = remember(letters, todayIso) {
        letters.filter { !it.isUnlocked(todayIso) }.sortedBy { it.dateIso }
    }

    var openedLetterForReading by remember { mutableStateOf<DiaryEntry?>(null) }
    var clickedSealedLetter by remember { mutableStateOf<DiaryEntry?>(null) }

    if (openedLetterForReading != null) {
        val letter = openedLetterForReading!!
        FutureLetterReaderDialog(
            letter = letter,
            onDismiss = { openedLetterForReading = null },
            onDelete = {
                if (letter.id.isNotBlank()) {
                    onDeleteLetter(letter.id)
                }
                openedLetterForReading = null
            }
        )
    }

    if (clickedSealedLetter != null) {
        val sealed = clickedSealedLetter!!
        val daysLeft = DateAndPhotoUtils.daysBetween(todayIso, sealed.dateIso).coerceAtLeast(1)
        AlertDialog(
            onDismissRequest = { clickedSealedLetter = null },
            icon = {
                SealedEnvelopeIllustration(isUnlocked = false, size = 56.dp)
            },
            title = {
                Text(
                    text = "Письмо запечатано",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "«${sealed.title}»",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Написано: ${DateAndPhotoUtils.formatShortRussianDate(sealed.writtenDateIso())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Дата открытия: ${DateAndPhotoUtils.formatShortRussianDate(sealed.dateIso)} (${DateAndPhotoUtils.formatDaysRemainingLabel(daysLeft).lowercase()})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Смысл капсулы времени — прочитать свои мысли именно тогда, когда наступит выбранный день. Но при необходимости вы можете снять печать досрочно.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { clickedSealedLetter = null }) {
                    Text("Подождать даты")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onUnlockLetterEarly(sealed)
                            val unlockedCopy = sealed.copy(dateIso = todayIso)
                            clickedSealedLetter = null
                            openedLetterForReading = unlockedCopy
                        }
                    ) {
                        Text("Открыть досрочно")
                    }
                    if (sealed.id.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onDeleteLetter(sealed.id)
                                clickedSealedLetter = null
                            }
                        ) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("future_letters_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                BotanicalHeroArtCard(height = 148.dp)
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
                                text = "Письма в будущее",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF2C2623)
                            )
                            Text(
                                text = "Послания самому себе сквозь время",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5A514B)
                            )
                        }
                        SealedEnvelopeIllustration(
                            isUnlocked = unlockedLetters.isNotEmpty(),
                            size = 52.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenComposer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("write_future_letter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mail,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Написать письмо себе в будущее", maxLines = 1)
                    }
                }
            }
        }

        // Ready to open section
        if (unlockedLetters.isNotEmpty()) {
            item {
                Text(
                    text = "Готовы к открытию (${unlockedLetters.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(unlockedLetters, key = { it.id.ifBlank { it.dateIso } }) { letter ->
                UnlockedFutureLetterCard(
                    letter = letter,
                    onClick = { openedLetterForReading = letter }
                )
            }
        }

        // Sealed letters waiting for their date
        if (sealedLetters.isNotEmpty()) {
            item {
                Text(
                    text = "Запечатанные в пути (${sealedLetters.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(sealedLetters, key = { it.id.ifBlank { it.dateIso } }) { letter ->
                val daysLeft = DateAndPhotoUtils.daysBetween(todayIso, letter.dateIso).coerceAtLeast(1)
                SealedFutureLetterCard(
                    letter = letter,
                    daysRemaining = daysLeft,
                    onClick = { clickedSealedLetter = letter }
                )
            }
        }

        // Clean empty state when user has no letters yet
        if (letters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SealedEnvelopeIllustration(isUnlocked = false, size = 68.dp)
                        Text(
                            text = "У вас пока нет писем в будущее",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Напишите себе слова поддержки, сохраните сегодняшние мечты или фотографии и выберите дату — через месяц, полгода или год. Письмо останется запечатанным до этого дня.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FilledTonalButton(
                            onClick = onOpenComposer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Создать первое письмо", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlockedFutureLetterCard(
    letter: DiaryEntry,
    onClick: () -> Unit
) {
    val mood = letter.botanicalMood()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("unlocked_letter_card_${letter.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SealedEnvelopeIllustration(isUnlocked = true, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Печать снята • Можно прочитать",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = letter.title.ifBlank { "Письмо себе в будущее" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Написано ${DateAndPhotoUtils.formatShortRussianDate(letter.writtenDateIso())} • Настроение: ${mood.titleRu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.MarkEmailRead,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Открыть и прочитать письмо", maxLines = 1)
            }
        }
    }
}

@Composable
private fun SealedFutureLetterCard(
    letter: DiaryEntry,
    daysRemaining: Int,
    onClick: () -> Unit
) {
    val mood = letter.botanicalMood()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sealed_letter_card_${letter.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SealedEnvelopeIllustration(isUnlocked = false, size = 50.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = DateAndPhotoUtils.formatDaysRemainingLabel(daysRemaining),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = letter.title.ifBlank { "Письмо себе в будущее" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Написано: ${DateAndPhotoUtils.formatShortRussianDate(letter.writtenDateIso())} (${mood.titleRu})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Откроется: ${DateAndPhotoUtils.formatShortRussianDate(letter.dateIso)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FutureLetterReaderDialog(
    letter: DiaryEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val mood = letter.botanicalMood()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            BotanicalMoodIllustration(mood = mood, size = 56.dp)
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = letter.title.ifBlank { "Письмо из прошлого" },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Написано ${DateAndPhotoUtils.formatShortRussianDate(letter.writtenDateIso())} • Настроение: ${mood.titleRu}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = letter.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (!letter.reflectionAnswer.isNullOrBlank()) {
                    Surface(
                        color = mood.softBgColor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Моя надежда и цель к этому дню:",
                                style = MaterialTheme.typography.labelSmall,
                                color = mood.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = letter.reflectionAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (letter.photosBase64.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        letter.photosBase64.forEach { b64 ->
                            val bmp = remember(b64) {
                                DateAndPhotoUtils.decodeBase64ToImageBitmap(b64)
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Фото из письма",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Удалить письмо", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FutureLetterComposerScreen(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveLetter: (
        unlockDateIso: String,
        mood: BotanicalMood,
        title: String,
        content: String,
        hopeOrGoal: String?,
        photosBase64: List<String>
    ) -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val todayIso = remember { DateAndPhotoUtils.todayIso() }
    var unlockDateIso by remember {
        mutableStateOf(DateAndPhotoUtils.shiftMonthsIso(todayIso, 1))
    }
    var selectedPreset by remember { mutableIntStateOf(1) } // 1m, 3m, 6m, 12m, 0=custom
    var selectedMood by remember { mutableStateOf(BotanicalMood.WARM_ROOT) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var hopeOrGoal by remember { mutableStateOf("") }
    var promptIndex by remember { mutableIntStateOf(0) }
    val photosBase64 = remember { mutableStateListOf<String>() }
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
        val cal = DateAndPhotoUtils.parseIsoToCalendar(unlockDateIso)
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
                            unlockDateIso = DateAndPhotoUtils.calendarToIso(pickedUtc)
                            selectedPreset = 0
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

    val daysFromToday = DateAndPhotoUtils.daysBetween(todayIso, unlockDateIso).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Top bar with zero button overflow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("future_letter_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Text(
                    text = "Письмо в будущее",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )
                Button(
                    onClick = {
                        if (content.isBlank()) {
                            validationError = "Напишите текст послания себе в будущее"
                        } else {
                            onSaveLetter(
                                unlockDateIso,
                                selectedMood,
                                title.ifBlank { "Письмо себе из ${DateAndPhotoUtils.formatShortRussianDate(todayIso)}" },
                                content,
                                hopeOrGoal.takeIf { it.isNotBlank() },
                                photosBase64.toList()
                            )
                        }
                    },
                    enabled = !isSaving,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("future_letter_seal_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSaving) "..." else "Запечатать", maxLines = 1)
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
                // 1. Unlock Date & Time Capsule Presets
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Когда открыть письмо?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${DateAndPhotoUtils.formatFullRussianDate(unlockDateIso)} (${
                                        if (daysFromToday <= 0) "откроется сразу"
                                        else "через $daysFromToday дн."
                                    })",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                1 to "Через 1 мес.",
                                3 to "Через 3 мес.",
                                6 to "Через полгода",
                                12 to "Через 1 год"
                            )
                            presets.forEach { (months, label) ->
                                FilterChip(
                                    selected = selectedPreset == months,
                                    onClick = {
                                        selectedPreset = months
                                        unlockDateIso = DateAndPhotoUtils.shiftMonthsIso(todayIso, months)
                                    },
                                    label = { Text(label) }
                                )
                            }
                            FilterChip(
                                selected = selectedPreset == 0,
                                onClick = { showDatePickerDialog = true },
                                label = { Text("Выбрать дату") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // 2. Inspiration prompt card
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                    text = "Идея для письма",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = {
                                    promptIndex = (promptIndex + 1) % ReflectionPrompts.futureLetterPrompts.size
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Другая", maxLines = 1)
                            }
                        }
                        Text(
                            text = ReflectionPrompts.futureLetterPrompts[promptIndex],
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 3. Letter Title & Body
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
                    label = { Text("Обращение или тема письма") },
                    placeholder = { Text("Например: Дорогой я из будущего…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("future_letter_title_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        validationError = null
                    },
                    label = { Text("Текст вашего письма") },
                    placeholder = { Text("Напишите то, о чем важно помнить, поддержите себя, расскажите о сегодняшнем дне…") },
                    minLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("future_letter_content_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = hopeOrGoal,
                    onValueChange = { hopeOrGoal = it },
                    label = { Text("Моя главная надежда или цель к этой дате (по желанию)") },
                    placeholder = { Text("Например: Обрести больше спокойствия и побывать у моря") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("future_letter_goal_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // 4. Mood when writing the letter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "С каким чувством вы отправляете письмо?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    BotanicalMoodSelectorGrid(
                        selectedMood = selectedMood,
                        onMoodSelected = { selectedMood = it }
                    )
                }

                // 5. Photos for the time capsule
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
                                text = "Вложить фото в конверт (${photosBase64.size}/4)",
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
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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
                                                contentDescription = "Фото в письмо",
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
