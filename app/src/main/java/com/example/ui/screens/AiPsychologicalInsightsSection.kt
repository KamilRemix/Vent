package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiAnalysisUiState
import com.example.data.ai.AiPsychologicalReport
import com.example.data.ai.AnalysisPeriodType
import com.example.data.ai.AvailableAnalysisPeriod
import com.example.data.ai.WeekdayMoodSummary
import com.example.data.model.BotanicalMood
import com.example.ui.components.BotanicalMoodIllustration
import com.example.ui.theme.NunitoFontFamily
import com.example.util.LocalAppLanguage
import com.example.util.tr

/**
 * Inviting banner shown on the Home Journal screen at the end of the month,
 * in December for the Year in Review, or when a monthly psychological check-in is available.
 */
@Composable
fun EndOfPeriodAiBanner(
    period: AvailableAnalysisPeriod,
    hasSavedReport: Boolean,
    onOpenPeriodAnalysis: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val isYearly = period.periodType == AnalysisPeriodType.YEARLY_REVIEW

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPeriodAnalysis(period.periodKey) }
            .testTag("home_ai_period_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isYearly) {
                            tr("DECEMBER YEAR IN REVIEW • AI PSYCHOLOGIST", "ИТОГИ ГОДА В ДЕКАБРЕ • ИИ-ПСИХОЛОГ")
                        } else if (period.isEndOfPeriodHighlight) {
                            tr("END OF MONTH • PSYCHOLOGICAL REVIEW", "ИТОГИ МЕСЯЦА • ИИ-ПСИХОЛОГ")
                        } else {
                            tr("PSYCHOLOGICAL STATE ANALYSIS", "ПСИХОЛОГИЧЕСКИЙ РАЗБОР СОСТОЯНИЯ")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = period.localizedTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = tr("${period.entriesCount} entries", "Записей: ${period.entriesCount}"),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = if (isYearly) {
                    tr(
                        "Reflect on your entire year: discover emotional arcs, energy sources, hidden wins, and warm guidance from your AI psychologist.",
                        "Подведите глубокие итоги года: узнайте динамику своего состояния, источники сил, скрытые победы и тёплое письмо-резюме."
                    )
                } else {
                    tr(
                        "Read a mindful psychological portrait of your mood, stress triggers, thinking patterns, and energy anchors for ${period.localizedTitle(lang)}.",
                        "Прочитайте бережный психологический разбор вашего состояния, триггеров усталости, паттернов мышления и точек опоры за ${period.localizedTitle(lang)}."
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onOpenPeriodAnalysis(period.periodKey) },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_ai_banner_action_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasSavedReport) {
                        tr("Read Psychological Report", "Открыть готовый разбор")
                    } else {
                        tr("Analyze My State", "Проанализировать моё состояние")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Full Psychological AI Hub & Report Viewer inside AnalyticsTab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPsychologicalHubSection(
    availablePeriods: List<AvailableAnalysisPeriod>,
    selectedPeriodKey: String,
    selectedCategory: AnalysisPeriodType,
    savedReports: Map<String, AiPsychologicalReport>,
    aiUiState: AiAnalysisUiState,
    onSelectCategory: (AnalysisPeriodType) -> Unit,
    onSelectPeriodKey: (String) -> Unit,
    onGenerateOrRefreshReport: (AvailableAnalysisPeriod) -> Unit
) {
    val lang = LocalAppLanguage.current

    val filteredPeriodsForCategory = remember(availablePeriods, selectedCategory) {
        availablePeriods.filter { it.periodType == selectedCategory }
    }

    val activePeriod = remember(availablePeriods, selectedPeriodKey, filteredPeriodsForCategory) {
        availablePeriods.firstOrNull { it.periodKey == selectedPeriodKey }
            ?: filteredPeriodsForCategory.firstOrNull()
            ?: availablePeriods.firstOrNull()
    }

    val currentSavedReport = activePeriod?.let { period ->
        savedReports[period.periodKey]
            ?: (aiUiState as? AiAnalysisUiState.Success)?.report?.takeIf { it.periodKey == period.periodKey }
    }

    var lastAutoRequestedKey by remember { mutableStateOf<String?>(null) }

    // Auto-generate report once when opening a period that doesn't have a saved report yet
    LaunchedEffect(activePeriod?.periodKey, activePeriod?.entriesCount) {
        val period = activePeriod ?: return@LaunchedEffect
        val existing = savedReports[period.periodKey]
        val requestSignature = "${period.periodKey}_${period.entriesCount}"
        val needsAutoGeneration = existing == null || (existing.entriesCount == 0 && period.entriesCount > 0)
        if (needsAutoGeneration && lastAutoRequestedKey != requestSignature && aiUiState !is AiAnalysisUiState.Generating) {
            lastAutoRequestedKey = requestSignature
            onGenerateOrRefreshReport(period)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Main AI Psychologist Control Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_psychologist_hub_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("AI PSYCHOLOGIST • STATE & MOOD PORTRAIT", "ИИ-ПСИХОЛОГ • РАЗБОР СОСТОЯНИЯ"),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tr(
                                "Deep Analysis of Mood & State",
                                "Глубокий анализ состояния и эмоций"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = tr(
                        "Select a month, the last 30 days, or your Year in Review (December special). Your personal AI psychologist analyzes your emotional rhythm, stress triggers, energy anchors, thinking patterns, and hidden wins.",
                        "Выберите итоги месяца, срез за 30 дней или большой годовой отчёт (особенно в декабре). Умный алгоритм проанализирует ваше настроение, триггеры стресса, источники ресурса, паттерны мышления и скрытые победы."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category Mode Switcher (Monthly | Last 30 Days | Yearly Review)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modes = listOf(
                        AnalysisPeriodType.MONTHLY to tr("Monthly Summary", "Итоги месяца"),
                        AnalysisPeriodType.LAST_30_DAYS to tr("Last 30 Days", "За 30 дней"),
                        AnalysisPeriodType.YEARLY_REVIEW to tr("Year in Review (Dec)", "Итоги года (Декабрь)")
                    )
                    modes.forEach { (mode, label) ->
                        val selected = selectedCategory == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(mode) },
                            label = {
                                Text(
                                    text = label,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("ai_mode_${mode.id}")
                        )
                    }
                }

                // Period Picker Chips for the chosen category
                if (filteredPeriodsForCategory.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredPeriodsForCategory, key = { it.periodKey }) { periodItem ->
                            val isSelected = activePeriod?.periodKey == periodItem.periodKey
                            val isSaved = savedReports.containsKey(periodItem.periodKey)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPeriodKey(periodItem.periodKey) },
                                label = {
                                    Text(
                                        text = "${periodItem.localizedTitle(lang)} (${periodItem.entriesCount})",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                trailingIcon = if (isSaved) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Spa,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                // Selected Period Summary + Generate / Update Button
                if (activePeriod != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activePeriod.localizedTitle(lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activePeriod.localizedSubtitle(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = tr(
                                        "${activePeriod.entriesCount} entries",
                                        "Записей: ${activePeriod.entriesCount}"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    val generatingState = aiUiState as? AiAnalysisUiState.Generating
                    val isGenerating = generatingState != null
                    Button(
                        onClick = { onGenerateOrRefreshReport(activePeriod) },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_ai_report_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (generatingState != null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(
                                    "Analyzing state... ${generatingState.progressPercent}%",
                                    "Идёт психологический разбор... ${generatingState.progressPercent}%"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = if (currentSavedReport != null) Icons.Filled.Refresh else Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    currentSavedReport != null -> tr(
                                        "Refresh Analysis (${activePeriod.localizedTitle(lang)})",
                                        "Обновить разбор (${activePeriod.localizedTitle(lang)})"
                                    )
                                    else -> tr(
                                        "Analyze My State (${activePeriod.localizedTitle(lang)})",
                                        "Проанализировать состояние (${activePeriod.localizedTitle(lang)})"
                                    )
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Error Banner if generation failed
                if (aiUiState is AiAnalysisUiState.Error) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiUiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // 2. Interactive Live Progress Card while AI analyzes entries
        val activeGeneratingState = aiUiState as? AiAnalysisUiState.Generating
        AnimatedVisibility(
            visible = activeGeneratingState != null,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut()
        ) {
            if (activeGeneratingState != null) {
                val targetProgress = (activeGeneratingState.progressPercent / 100f).coerceIn(0.05f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = 320),
                    label = "ai_progress_bar"
                )

                val steps = listOf(
                    16 to tr("Reading journal entries & titles", "Чтение записей, заголовков и контекста"),
                    38 to tr("Computing emotional arc & harmony score", "Оценка эмоциональной дуги и ведущего тона"),
                    64 to tr("Finding energy anchors & stress triggers", "Поиск источников ресурса и триггеров усталости"),
                    84 to tr("Mapping weekday rhythm & #tags", "Анализ ритма по дням недели и #тегам"),
                    96 to tr("Writing personal letter & recommendations", "Формирование тёплого письма и рекомендаций")
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeGeneratingState.stageTitle.ifBlank {
                                        tr("Analyzing your state...", "Анализируем ваше состояние...")
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (activeGeneratingState.stageDetail.isNotBlank()) {
                                    Text(
                                        text = activeGeneratingState.stageDetail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${activeGeneratingState.progressPercent}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            steps.forEach { (threshold, label) ->
                                val isCompleted = activeGeneratingState.progressPercent > threshold
                                val isCurrent = activeGeneratingState.progressPercent == threshold
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted || isCurrent) {
                                            Icons.Filled.CheckCircle
                                        } else {
                                            Icons.Outlined.RadioButtonUnchecked
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isCompleted -> Color(0xFF3E7B52)
                                            isCurrent -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = if (isCurrent || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isCompleted || isCurrent -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Detailed Psychological Report Cards (when available for the selected period)
        if (currentSavedReport != null) {
            PsychologicalReportContent(report = currentSavedReport)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsychologicalReportContent(
    report: AiPsychologicalReport
) {
    val lang = LocalAppLanguage.current
    val dominantMood = remember(report.dominantMoodKey) { report.dominantMood() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_psychological_report_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Report Headline + Core Emotional Arc + Warm Letter from Psychologist
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, dominantMood.primaryColor.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BotanicalMoodIllustration(
                        mood = dominantMood,
                        size = 56.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.periodTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = dominantMood.primaryColor
                        )
                        Text(
                            text = "«${report.emotionalHeadline}»",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr(
                                "${report.entriesCount} entries • Avg intensity ${String.format("%.1f", report.averageIntensity)}/5 • Dominant: ${dominantMood.localizedTitle(lang)}",
                                "Записей: ${report.entriesCount} • Интенсивность: ${String.format("%.1f", report.averageIntensity)}/5 • Фон: ${dominantMood.localizedTitle(lang)}"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Warm Empathetic Letter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tr("Warm Letter & State Reflection", "Тёплое письмо-отклик о твоём состоянии"),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = report.warmLetter,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )

                if (report.emotionalArcSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = dominantMood.softBgColor.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tr("EMOTIONAL DYNAMICS OF THE PERIOD", "ДИНАМИКА НАСТРОЕНИЯ ЗА ПЕРИОД"),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = dominantMood.primaryColor
                            )
                            Text(
                                text = report.emotionalArcSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Energy Sources & Anchors vs. Stress Triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightBulletListCard(
                title = tr("Energy & Anchors", "Источники ресурса и сил"),
                subtitle = tr("What restored your calm and light", "Что наполняло энергией и покоем"),
                icon = Icons.Filled.WbSunny,
                accentColor = Color(0xFF3E7B52),
                softBgColor = Color(0xFFEAF4EC),
                items = report.energySources,
                modifier = Modifier.weight(1f)
            )
        }

        InsightBulletListCard(
            title = tr("Stress Triggers & Energy Drains", "Триггеры усталости и стресса"),
            subtitle = tr("Situations and themes that drained your energy", "Что вызывало напряжение, тревогу или упадок сил"),
            icon = Icons.Outlined.TrendingDown,
            accentColor = Color(0xFFB55D4C),
            softBgColor = Color(0xFFFAECE8),
            items = report.stressTriggers,
            modifier = Modifier.fillMaxWidth()
        )

        // Card 3: Cognitive Patterns & Inner Voice
        InsightBulletListCard(
            title = tr("Thinking Patterns & Inner Voice", "Паттерны мышления и внутренний голос"),
            subtitle = tr("How you talked to yourself and processed emotions", "Как строился ваш внутренний диалог и восприятие событий"),
            icon = Icons.Filled.Psychology,
            accentColor = MaterialTheme.colorScheme.primary,
            softBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            items = report.cognitivePatterns,
            modifier = Modifier.fillMaxWidth()
        )

        // Card 4: Quiet Wins & Personal Resilience
        InsightBulletListCard(
            title = tr("Quiet Wins & Personal Growth", "Скрытые личные победы и точки опоры"),
            subtitle = tr("Important steps and resilience worth celebrating", "Важные преодоления, за которые стоит себя поблагодарить"),
            icon = Icons.Filled.EmojiEvents,
            accentColor = Color(0xFFB8862D),
            softBgColor = Color(0xFFFBF3E4),
            items = report.quietWins,
            modifier = Modifier.fillMaxWidth()
        )

        // Card 5: Detailed Weekday Rhythm & Tag Analysis
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = tr("Weekday Rhythm & Tag Map", "Ритм по дням недели и карта #тегов"),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "How your emotional intensity and frequency shift from Monday to Sunday",
                                "Как менялись активность и интенсивность эмоций с понедельника по воскресенье"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Weekday Breakdown Bars
                if (report.weekdayBreakdown.isNotEmpty()) {
                    val maxDayCount = (report.weekdayBreakdown.maxOfOrNull { it.entriesCount } ?: 1).coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.weekdayBreakdown.forEach { day ->
                            WeekdayStatRow(day = day, maxCount = maxDayCount)
                        }
                    }
                }

                // Top Tags Row
                if (report.topTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.topTags.forEach { tagBadge ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = tagBadge,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                if (report.weekdayAndTagInsights.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.weekdayAndTagInsights,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Card 6: Gentle Advice & Questions for Next Month / Year
        InsightBulletListCard(
            title = tr("Gentle Rituals & Questions Ahead", "Мягкие советы и вопросы на будущее"),
            subtitle = tr("Mindful practices tailored for your next chapter", "Бережные практики и ориентиры на следующий период"),
            icon = Icons.Outlined.Lightbulb,
            accentColor = MaterialTheme.colorScheme.tertiary,
            softBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            items = report.gentleAdviceAndQuestions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WeekdayStatRow(
    day: WeekdayMoodSummary,
    maxCount: Int
) {
    val lang = LocalAppLanguage.current
    val mood = day.dominantMoodKey?.let { BotanicalMood.fromKey(it) }
    val barFraction = if (day.entriesCount == 0) 0.04f else (day.entriesCount.toFloat() / maxCount.toFloat()).coerceIn(0.12f, 1f)
    val barColor = mood?.primaryColor ?: MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = day.shortLabel(lang),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }

        Text(
            text = if (day.entriesCount > 0) {
                "${day.entriesCount} • ${String.format("%.1f", day.averageIntensity)}/5"
            } else {
                "0"
            },
            style = MaterialTheme.typography.labelMedium,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (day.entriesCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(68.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun InsightBulletListCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    softBgColor: Color,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = softBgColor,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { idx, point ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = softBgColor.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
