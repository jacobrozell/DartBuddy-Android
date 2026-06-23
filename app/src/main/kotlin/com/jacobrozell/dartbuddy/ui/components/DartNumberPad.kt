package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import com.jacobrozell.dartbuddy.ui.theme.Brand

internal fun scoringPadRows(columnCount: Int): List<List<Int>> = when (columnCount) {
    4 -> listOf(
        listOf(1, 2, 3, 4),
        listOf(5, 6, 7, 8),
        listOf(9, 10, 11, 12),
        listOf(13, 14, 15, 16),
        listOf(17, 18, 19, 20),
        listOf(25),
    )
    else -> listOf(
        listOf(1, 2, 3, 4, 5, 6, 7),
        listOf(8, 9, 10, 11, 12, 13, 14),
        listOf(15, 16, 17, 18, 19, 20, 25),
    )
}

@Composable
fun DartNumberPad(
    enteredDarts: List<DartInput>,
    selectedMultiplier: DartMultiplier,
    onMultiplierChange: (DartMultiplier) -> Unit,
    onSegmentTap: (Int) -> Unit,
    onMiss: () -> Unit,
    onUndoDart: () -> Unit,
    onUndoTurn: () -> Unit,
    modifier: Modifier = Modifier,
    showsVisitPreview: Boolean = true,
    maxDarts: Int = 3,
    enabled: Boolean = true,
    dartSlotAlphas: List<Float> = emptyList(),
    columnCount: Int = 7,
) {
    val dark = isSystemInDarkTheme()
    val dartBoxColor = if (dark) Brand.DartBoxDark else Brand.DartBoxLight
    val interactionModifier = if (enabled) Modifier else Modifier.alpha(0.45f)

    Column(modifier.then(interactionModifier).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showsVisitPreview) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .a11yId(AccessibilityIds.DART_VISIT_PREVIEW),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(maxDarts) { index ->
                        val label = enteredDarts.getOrNull(index)?.compactDisplayLabel ?: "—"
                        val slotAlpha = dartSlotAlphas.getOrElse(index) { 1f }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = dartBoxColor,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .heightIn(min = 34.dp)
                                .alpha(slotAlpha),
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }

        scoringPadRows(columnCount).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { value ->
                    val label = if (value == 25) stringResource(R.string.scoring_pad_bullLabel) else value.toString()
                    val style = if (value == 25) ScoringPadKeyStyle.Bull else ScoringPadKeyStyle.Number
                    val testTag = when (value) {
                        25 -> AccessibilityIds.PAD_BULL
                        else -> AccessibilityIds.padSegment(value)
                    }
                    ScoringPadKeyWeight(
                        label = label,
                        onClick = { onSegmentTap(value) },
                        style = style,
                        enabled = enabled && enteredDarts.size < maxDarts,
                        testTag = testTag,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_multiplier_double),
                onClick = { onMultiplierChange(DartMultiplier.Double) },
                style = ScoringPadKeyStyle.Modifier,
                selected = selectedMultiplier == DartMultiplier.Double,
                enabled = enabled,
                accessibilityLabel = stringResource(R.string.scoring_multiplier_double_accessibility),
                testTag = AccessibilityIds.PAD_DOUBLE,
            )
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_multiplier_triple),
                onClick = { onMultiplierChange(DartMultiplier.Triple) },
                style = ScoringPadKeyStyle.Modifier,
                selected = selectedMultiplier == DartMultiplier.Triple,
                enabled = enabled,
                accessibilityLabel = stringResource(R.string.scoring_multiplier_triple_accessibility),
                testTag = AccessibilityIds.PAD_TRIPLE,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_pad_missLabel),
                onClick = onMiss,
                style = ScoringPadKeyStyle.Miss,
                enabled = enteredDarts.size < maxDarts,
                accessibilityLabel = stringResource(R.string.scoring_segment_miss_accessibility),
                testTag = AccessibilityIds.PAD_MISS,
            )
            OutlinedUndoKey(
                onClick = { if (enteredDarts.isEmpty()) onUndoTurn() else onUndoDart() },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                accessibilityLabel = stringResource(R.string.scoring_undoLastTurn),
                testTag = AccessibilityIds.PAD_UNDO,
            )
        }
    }
}

@Composable
private fun OutlinedUndoKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityLabel: String,
    testTag: String,
) {
    ScoringPadKey(
        label = "↩",
        onClick = onClick,
        modifier = modifier,
        style = ScoringPadKeyStyle.Action,
        enabled = enabled,
        accessibilityLabel = accessibilityLabel,
        testTag = testTag,
    )
}

fun segmentFromPadValue(value: Int, multiplier: DartMultiplier): DartInput = when (value) {
    0 -> DartInput(multiplier, DartSegment.Miss, isMiss = true)
    25 -> if (multiplier == DartMultiplier.Triple) {
        DartInput(DartMultiplier.Single, DartSegment.InnerBull)
    } else {
        DartInput(multiplier, DartSegment.OuterBull)
    }
    else -> DartInput(multiplier, DartSegment.OneToTwenty(value))
}
