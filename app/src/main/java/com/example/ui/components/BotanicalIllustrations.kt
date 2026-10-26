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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                            text = mood.titleRu,
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
