package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import com.jacobrozell.dartbuddy.ui.theme.Brand

private val numberRows = listOf(
    listOf(20, 19, 18),
    listOf(17, 16, 15),
)

@Composable
fun CricketTapPad(
    enteredDarts: List<DartInput>,
    selectedMultiplier: DartMultiplier,
    onMultiplierChange: (DartMultiplier) -> Unit,
    onSegmentTap: (Int) -> Unit,
    onMiss: () -> Unit,
    onSubmit: () -> Unit,
    onUndoDart: () -> Unit,
    onUndoTurn: () -> Unit,
    canSubmit: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = isSystemInDarkTheme()
    val dartBoxColor = if (dark) Brand.DartBoxDark else Brand.DartBoxLight
    val padEnabled = enabled

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(3) { index ->
                    val label = enteredDarts.getOrNull(index)?.compactDisplayLabel ?: "—"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = dartBoxColor,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp).heightIn(min = 34.dp),
                    ) {
                        Text(label, modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        numberRows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { value ->
                    ScoringPadKeyWeight(
                        label = value.toString(),
                        onClick = { onSegmentTap(value) },
                        enabled = padEnabled && enteredDarts.size < 3,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_pad_bullLabel),
                onClick = { onSegmentTap(25) },
                style = ScoringPadKeyStyle.Bull,
                enabled = padEnabled && enteredDarts.size < 3,
            )
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_pad_missLabel),
                onClick = onMiss,
                style = ScoringPadKeyStyle.Miss,
                enabled = padEnabled && enteredDarts.size < 3,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_multiplier_double),
                onClick = { onMultiplierChange(DartMultiplier.Double) },
                style = ScoringPadKeyStyle.Modifier,
                selected = selectedMultiplier == DartMultiplier.Double,
                enabled = padEnabled,
            )
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_multiplier_triple),
                onClick = { onMultiplierChange(DartMultiplier.Triple) },
                style = ScoringPadKeyStyle.Modifier,
                selected = selectedMultiplier == DartMultiplier.Triple,
                enabled = padEnabled,
            )
            ScoringPadKeyWeight(
                label = stringResource(R.string.scoring_enter),
                onClick = onSubmit,
                style = ScoringPadKeyStyle.Action,
                enabled = padEnabled && canSubmit,
            )
        }

        Row(Modifier.fillMaxWidth()) {
            ScoringPadKey(
                label = "↩",
                onClick = { if (enteredDarts.isEmpty()) onUndoTurn() else onUndoDart() },
                modifier = Modifier.fillMaxWidth(),
                style = ScoringPadKeyStyle.Action,
                enabled = padEnabled,
                accessibilityLabel = stringResource(R.string.scoring_undoLastTurn),
            )
        }
    }
}

fun cricketSegmentFromPad(value: Int, multiplier: DartMultiplier): DartInput = when (value) {
    0 -> DartInput(multiplier, DartSegment.Miss, isMiss = true)
    25 -> if (multiplier == DartMultiplier.Double) {
        DartInput(DartMultiplier.Single, DartSegment.InnerBull)
    } else {
        DartInput(multiplier, DartSegment.OuterBull)
    }
    else -> DartInput(multiplier, DartSegment.OneToTwenty(value))
}
