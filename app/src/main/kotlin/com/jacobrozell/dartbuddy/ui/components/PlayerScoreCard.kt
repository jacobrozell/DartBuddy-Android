package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun PlayerScoreCard(
    name: String,
    score: Int,
    setsWon: Int,
    legsWon: Int,
    isActive: Boolean,
    accentColor: Color,
    visitDarts: List<DartInput>,
    dartsThrown: Int,
    average: Double,
    modifier: Modifier = Modifier,
    dartSlotAlphas: List<Float> = emptyList(),
    useWideLayout: Boolean = false,
    useStackedLayout: Boolean = false,
    hideInlineStats: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    val dartBox = if (dark) Brand.DartBoxDark else Brand.DartBoxLight
    val secondary = if (dark) Brand.TextSecondaryDark else Brand.TextSecondaryLight

    val summary = stringResource(R.string.play_x01_scoreCard_summaryFormat, name, score)
    val visitLabel = if (visitDarts.isNotEmpty()) {
        stringResource(
            R.string.play_x01_scoreCard_visitDartsFormat,
            visitDarts.joinToString(" ") { it.compactDisplayLabel },
        )
    } else {
        ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .semantics { contentDescription = listOf(summary, visitLabel).filter { it.isNotEmpty() }.joinToString(". ") },
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(
                    when {
                        useStackedLayout -> 120.dp
                        useWideLayout -> 72.dp
                        else -> 88.dp
                    },
                )
                .background(if (isActive) accentColor else Color.Transparent),
        )
        when {
            useStackedLayout -> {
                StackedScoreCardBody(
                    name = name,
                    score = score,
                    setsWon = setsWon,
                    legsWon = legsWon,
                    isActive = isActive,
                    accentColor = accentColor,
                    visitDarts = visitDarts,
                    dartsThrown = dartsThrown,
                    average = average,
                    dartBox = dartBox,
                    secondary = secondary,
                    dartSlotAlphas = dartSlotAlphas,
                    showInlineStats = !hideInlineStats,
                )
            }
            useWideLayout -> {
            WideScoreCardBody(
                name = name,
                score = score,
                setsWon = setsWon,
                legsWon = legsWon,
                isActive = isActive,
                accentColor = accentColor,
                visitDarts = visitDarts,
                dartsThrown = dartsThrown,
                average = average,
                dartBox = dartBox,
                secondary = secondary,
                dartSlotAlphas = dartSlotAlphas,
            )
            }
            else -> {
            StandardScoreCardBody(
                name = name,
                score = score,
                setsWon = setsWon,
                legsWon = legsWon,
                isActive = isActive,
                accentColor = accentColor,
                visitDarts = visitDarts,
                dartsThrown = dartsThrown,
                average = average,
                dartBox = dartBox,
                secondary = secondary,
                dartSlotAlphas = dartSlotAlphas,
            )
            }
        }
    }
}

@Composable
private fun StackedScoreCardBody(
    name: String,
    score: Int,
    setsWon: Int,
    legsWon: Int,
    isActive: Boolean,
    accentColor: Color,
    visitDarts: List<DartInput>,
    dartsThrown: Int,
    average: Double,
    dartBox: Color,
    secondary: Color,
    dartSlotAlphas: List<Float>,
    showInlineStats: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isActive) {
            Text(
                stringResource(R.string.play_x01_turn_active),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
        Text(
            "$score",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) accentColor else secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        SetsLegsLine(setsWon, legsWon, secondary)
        if (isActive) {
            VisitDartRow(visitDarts, dartBox, dartSlotAlphas)
        }
        if (showInlineStats) {
            StatsColumn(average, dartsThrown, secondary, alignStart = true)
        }
    }
}

@Composable
private fun StandardScoreCardBody(
    name: String,
    score: Int,
    setsWon: Int,
    legsWon: Int,
    isActive: Boolean,
    accentColor: Color,
    visitDarts: List<DartInput>,
    dartsThrown: Int,
    average: Double,
    dartBox: Color,
    secondary: Color,
    dartSlotAlphas: List<Float>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isActive) {
                Text(
                    stringResource(R.string.play_x01_turn_active),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }
            Text(
                "$score",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) accentColor else secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SetsLegsLine(setsWon, legsWon, secondary)
        }

        if (isActive) {
            VisitDartRow(visitDarts, dartBox, dartSlotAlphas)
        }

        StatsColumn(average, dartsThrown, secondary)
    }
}

@Composable
private fun WideScoreCardBody(
    name: String,
    score: Int,
    setsWon: Int,
    legsWon: Int,
    isActive: Boolean,
    accentColor: Color,
    visitDarts: List<DartInput>,
    dartsThrown: Int,
    average: Double,
    dartBox: Color,
    secondary: Color,
    dartSlotAlphas: List<Float>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isActive) {
                        Text(
                            stringResource(R.string.play_x01_turn_active),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    }
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SetsLegsLine(setsWon, legsWon, secondary)
                }
                Text(
                    "$score",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (isActive) {
                VisitDartRow(visitDarts, dartBox, dartSlotAlphas)
            }
        }
        StatsColumn(average, dartsThrown, secondary)
    }
}

@Composable
private fun SetsLegsLine(setsWon: Int, legsWon: Int, secondary: Color) {
    if (setsWon > 0 || legsWon > 0) {
        Text(
            buildString {
                if (setsWon > 0) append(stringResource(R.string.play_x01_setsCountFormat, setsWon))
                if (legsWon > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(stringResource(R.string.play_x01_legsCountFormat, legsWon))
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
        )
    }
}

@Composable
private fun VisitDartRow(
    visitDarts: List<DartInput>,
    dartBox: Color,
    dartSlotAlphas: List<Float>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val label = visitDarts.getOrNull(index)?.compactDisplayLabel ?: "—"
            val slotAlpha = dartSlotAlphas.getOrElse(index) { 1f }
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(slotAlpha)
                    .background(dartBox),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatsColumn(
    average: Double,
    dartsThrown: Int,
    secondary: Color,
    alignStart: Boolean = false,
) {
    Column(horizontalAlignment = if (alignStart) Alignment.Start else Alignment.End) {
        Text(
            String.format("%.1f", average),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
        )
        Text("3DA", style = MaterialTheme.typography.labelSmall, color = secondary)
        Text("$dartsThrown", style = MaterialTheme.typography.labelSmall, color = secondary)
    }
}
