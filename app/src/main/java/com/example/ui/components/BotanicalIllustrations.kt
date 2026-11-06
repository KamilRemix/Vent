package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.BotanicalMood
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage

@Composable
fun BotanicalMoodIllustration(
    mood: BotanicalMood,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showSoftBackground: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showSoftBackground) {
                    Modifier
                        .clip(CircleShape)
                        .background(mood.softBgColor)
                        .border(1.dp, mood.primaryColor.copy(alpha = 0.25f), CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
            when (mood) {
                BotanicalMood.BLOOM -> drawJoySun(mood.primaryColor, mood.accentColor)
                BotanicalMood.CALM_LEAF -> drawCalmWave(mood.primaryColor, mood.accentColor)
                BotanicalMood.WARM_ROOT -> drawWarmHeart(mood.primaryColor, mood.accentColor)
                BotanicalMood.MIST_FERN -> drawMelancholyCloud(mood.primaryColor, mood.accentColor)
                BotanicalMood.AUTUMN_WIND -> drawAnxietyWind(mood.primaryColor, mood.accentColor)
                BotanicalMood.WINTER_SEED -> drawTiredMoon(mood.primaryColor, mood.accentColor)
                BotanicalMood.WILDFIRE -> drawWildfireFlame(mood.primaryColor, mood.accentColor)
                BotanicalMood.CHAOS_WIND -> drawChaosVortex(mood.primaryColor, mood.accentColor)
                BotanicalMood.ASH_MIST -> drawAshMist(mood.primaryColor, mood.accentColor)
            }
        }
    }
}

// 1. Радость — мягкое теплое солнце с мягкими лучами
private fun DrawScope.drawJoySun(primary: Color, accent: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.28f

    // 6 gentle sun rays
    val rayLen = r * 0.6f
    val rayOffset = r * 1.35f
    val angles = listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)
    for (deg in angles) {
        val rad = Math.toRadians(deg)
        val startX = cx + (rayOffset * Math.cos(rad)).toFloat()
        val startY = cy + (rayOffset * Math.sin(rad)).toFloat()
        val endX = cx + ((rayOffset + rayLen) * Math.cos(rad)).toFloat()
        val endY = cy + ((rayOffset + rayLen) * Math.sin(rad)).toFloat()
        drawLine(
            color = accent,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    drawCircle(color = primary, radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFFFF9EE), radius = r * 0.45f, center = Offset(cx - r * 0.2f, cy - r * 0.2f))
}

// 2. Спокойствие — плавная мягкая волна баланса
private fun DrawScope.drawCalmWave(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val wave1 = Path().apply {
        moveTo(w * 0.12f, h * 0.44f)
        cubicTo(w * 0.32f, h * 0.22f, w * 0.68f, h * 0.66f, w * 0.88f, h * 0.44f)
    }
    drawPath(wave1, color = primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

    val wave2 = Path().apply {
        moveTo(w * 0.20f, h * 0.66f)
        cubicTo(w * 0.40f, h * 0.48f, w * 0.60f, h * 0.84f, w * 0.80f, h * 0.66f)
    }
    drawPath(wave2, color = accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

    drawCircle(color = primary, radius = 3.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.22f))
}

// 3. Тепло — уютное сердце
private fun DrawScope.drawWarmHeart(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val heart = Path().apply {
        moveTo(w * 0.5f, h * 0.82f)
        cubicTo(w * 0.10f, h * 0.52f, w * 0.14f, h * 0.18f, w * 0.40f, h * 0.26f)
        cubicTo(w * 0.47f, h * 0.28f, w * 0.5f, h * 0.38f, w * 0.5f, h * 0.38f)
        cubicTo(w * 0.5f, h * 0.38f, w * 0.53f, h * 0.28f, w * 0.60f, h * 0.26f)
        cubicTo(w * 0.86f, h * 0.18f, w * 0.90f, h * 0.52f, w * 0.5f, h * 0.82f)
        close()
    }
    drawPath(heart, color = primary)
    drawCircle(color = accent, radius = 2.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.46f))
}

// 4. Грусть — мягкое облачко с каплей
private fun DrawScope.drawMelancholyCloud(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val cloud = Path().apply {
        moveTo(w * 0.26f, h * 0.58f)
        cubicTo(w * 0.10f, h * 0.58f, w * 0.10f, h * 0.38f, w * 0.26f, h * 0.36f)
        cubicTo(w * 0.28f, h * 0.18f, w * 0.54f, h * 0.14f, w * 0.64f, h * 0.28f)
        cubicTo(w * 0.74f, h * 0.20f, w * 0.90f, h * 0.32f, w * 0.86f, h * 0.46f)
        cubicTo(w * 0.96f, h * 0.52f, w * 0.90f, h * 0.58f, w * 0.78f, h * 0.58f)
        close()
    }
    drawPath(cloud, color = primary)

    // Gentle falling drop
    val drop = Path().apply {
        moveTo(w * 0.50f, h * 0.68f)
        lineTo(w * 0.54f, h * 0.78f)
        quadraticTo(w * 0.50f, h * 0.84f, w * 0.46f, h * 0.78f)
        close()
    }
    drawPath(drop, color = accent)
}

// 5. Тревога — легкий вихрь движения
private fun DrawScope.drawAnxietyWind(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val spiral = Path().apply {
        moveTo(w * 0.15f, h * 0.62f)
        cubicTo(w * 0.35f, h * 0.82f, w * 0.75f, h * 0.62f, w * 0.82f, h * 0.38f)
        cubicTo(w * 0.86f, h * 0.22f, w * 0.62f, h * 0.18f, w * 0.45f, h * 0.30f)
        cubicTo(w * 0.36f, h * 0.40f, w * 0.42f, h * 0.52f, w * 0.54f, h * 0.50f)
    }
    drawPath(spiral, color = primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    drawCircle(color = accent, radius = 3.dp.toPx(), center = Offset(w * 0.72f, h * 0.74f))
}

// 6. Усталость — мягкий полумесяц
private fun DrawScope.drawTiredMoon(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val moon = Path().apply {
        moveTo(w * 0.55f, h * 0.18f)
        cubicTo(w * 0.20f, h * 0.28f, w * 0.20f, h * 0.72f, w * 0.55f, h * 0.82f)
        cubicTo(w * 0.38f, h * 0.68f, w * 0.38f, h * 0.32f, w * 0.55f, h * 0.18f)
        close()
    }
    drawPath(moon, color = primary)

    // Tiny resting star dot
    drawCircle(color = accent, radius = 3.dp.toPx(), center = Offset(w * 0.74f, h * 0.42f))
}

// 7. Гнев / Злость — выразительный огненный лепесток с теплым ядром
private fun DrawScope.drawWildfireFlame(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val flame = Path().apply {
        moveTo(w * 0.50f, h * 0.14f)
        cubicTo(w * 0.68f, h * 0.30f, w * 0.86f, h * 0.52f, w * 0.76f, h * 0.76f)
        cubicTo(w * 0.68f, h * 0.88f, w * 0.32f, h * 0.88f, w * 0.24f, h * 0.76f)
        cubicTo(w * 0.14f, h * 0.52f, w * 0.32f, h * 0.30f, w * 0.50f, h * 0.14f)
        close()
    }
    drawPath(flame, color = primary)

    // Внутреннее светящееся ядро
    val inner = Path().apply {
        moveTo(w * 0.50f, h * 0.38f)
        cubicTo(w * 0.60f, h * 0.50f, w * 0.64f, h * 0.68f, w * 0.50f, h * 0.78f)
        cubicTo(w * 0.36f, h * 0.68f, w * 0.40f, h * 0.50f, w * 0.50f, h * 0.38f)
        close()
    }
    drawPath(inner, color = accent)
}

// 8. Перегруз / Хаос — вихревые потоки и искры мыслей
private fun DrawScope.drawChaosVortex(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val swirl1 = Path().apply {
        moveTo(w * 0.16f, h * 0.46f)
        cubicTo(w * 0.22f, h * 0.18f, w * 0.78f, h * 0.18f, w * 0.84f, h * 0.46f)
        cubicTo(w * 0.88f, h * 0.76f, w * 0.36f, h * 0.84f, w * 0.32f, h * 0.58f)
        cubicTo(w * 0.30f, h * 0.42f, w * 0.62f, h * 0.40f, w * 0.64f, h * 0.54f)
    }
    drawPath(swirl1, color = primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

    drawCircle(color = accent, radius = 3.dp.toPx(), center = Offset(w * 0.24f, h * 0.28f))
    drawCircle(color = accent, radius = 2.dp.toPx(), center = Offset(w * 0.76f, h * 0.70f))
}

// 9. Опустошение / Онемение — гладкий тихий камень в полосах тумана
private fun DrawScope.drawAshMist(primary: Color, accent: Color) {
    val w = size.width
    val h = size.height

    val mist1 = Path().apply {
        moveTo(w * 0.14f, h * 0.34f)
        cubicTo(w * 0.36f, h * 0.26f, w * 0.64f, h * 0.40f, w * 0.86f, h * 0.34f)
    }
    drawPath(mist1, color = accent, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))

    val stone = Path().apply {
        moveTo(w * 0.30f, h * 0.52f)
        cubicTo(w * 0.30f, h * 0.36f, w * 0.70f, h * 0.36f, w * 0.70f, h * 0.52f)
        cubicTo(w * 0.70f, h * 0.68f, w * 0.30f, h * 0.68f, w * 0.30f, h * 0.52f)
        close()
    }
    drawPath(stone, color = primary)

    val mist2 = Path().apply {
        moveTo(w * 0.20f, h * 0.70f)
        cubicTo(w * 0.42f, h * 0.64f, w * 0.68f, h * 0.76f, w * 0.86f, h * 0.70f)
    }
    drawPath(mist2, color = accent.copy(alpha = 0.75f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotanicalMoodBottomSheet(
    selectedMoods: Set<BotanicalMood>,
    onMoodToggle: (BotanicalMood) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (lang == AppLanguage.RU) "Что на душе?" else "How are you feeling?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lang == AppLanguage.RU) "Выберите одно или несколько состояний" else "Select one or multiple states that resonate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedMoods.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (lang == AppLanguage.RU) "Выбрано: ${selectedMoods.size}" else "${selectedMoods.size} selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3x3 Grid of 9 Emotions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BotanicalMood.entries.chunked(3).forEach { rowMoods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMoods.forEach { mood ->
                            val isSelected = selectedMoods.contains(mood)
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.03f else 1f,
                                animationSpec = spring(dampingRatio = 0.7f),
                                label = "moodSheetScale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSelected) mood.softBgColor
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) mood.primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable { onMoodToggle(mood) }
                                    .padding(vertical = 12.dp, horizontal = 6.dp)
                                    .testTag("sheet_mood_${mood.key}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(mood.primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    BotanicalMoodIllustration(
                                        mood = mood,
                                        size = 38.dp,
                                        showSoftBackground = !isSelected
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mood.localizedTitle(lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF2C2623) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = mood.localizedSubtitle(lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color(0xFF5A4D46) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Clear & Done
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (lang == AppLanguage.RU) "Сбросить" else "Reset to Calm",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.RU) "Готово" else "Done",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BotanicalMoodSelectorGrid(
    selectedMood: BotanicalMood,
    onMoodSelected: (BotanicalMood) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = BotanicalMood.entries
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        moods.chunked(3).forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMoods.forEach { mood ->
                    val isSelected = mood == selectedMood
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f),
                        label = "moodScale"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) mood.softBgColor
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) mood.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onMoodSelected(mood) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("mood_card_${mood.key}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BotanicalMoodIllustration(
                            mood = mood,
                            size = 38.dp,
                            showSoftBackground = !isSelected
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mood.localizedTitle(com.example.util.LocalAppLanguage.current),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF2C2623) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BotanicalHeroArtCard(
    modifier: Modifier = Modifier,
    height: Dp = 130.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Warm, soft soothing gradient background
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF3ECE1),
                        Color(0xFFEAF1EB),
                        Color(0xFFF7EDE3)
                    ),
                    start = Offset.Zero,
                    end = Offset(w, h)
                ),
                cornerRadius = CornerRadius(22.dp.toPx())
            )

            // Warm calm halos
            drawCircle(
                color = Color(0xFFD4A359).copy(alpha = 0.16f),
                radius = h * 0.65f,
                center = Offset(w * 0.88f, h * 0.30f)
            )
            drawCircle(
                color = Color(0xFF4A6B53).copy(alpha = 0.10f),
                radius = h * 0.45f,
                center = Offset(w * 0.15f, h * 0.70f)
            )
        }
    }
}

@Composable
fun MoodDistributionBar(
    moodCounts: Map<BotanicalMood, Int>,
    modifier: Modifier = Modifier
) {
    val total = moodCounts.values.sum().coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (moodCounts.values.sum() == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            BotanicalMood.entries.forEach { mood ->
                val count = moodCounts[mood] ?: 0
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat() / total.toFloat())
                            .fillMaxSize()
                            .background(mood.primaryColor)
                    )
                }
            }
        }
    }
}

