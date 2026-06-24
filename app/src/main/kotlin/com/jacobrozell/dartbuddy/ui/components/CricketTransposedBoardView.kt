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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.engine.CricketTarget
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun CricketTransposedBoardView(
    column: CricketBoardColumn,
    allColumns: List<CricketBoardColumn>,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val activeFill = if (dark) Brand.CardElevatedDark.copy(alpha = 0.5f) else Brand.CardElevatedLight.copy(alpha = 0.6f)
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(activeFill)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                column.name,
                fontWeight = FontWeight.SemiBold,
                color = column.accentColor,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${column.score}",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(column.accentColor),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            CricketTarget.allCases.forEach { target ->
                val marks = column.marks[target.raw] ?: 0
                val knockedOut = isTargetKnockedOut(allColumns, target)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (target == CricketTarget.Bull) "B" else target.raw,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        markGlyphs(marks),
                        fontWeight = FontWeight.Bold,
                        color = when {
                            knockedOut -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            marks >= 3 -> column.accentColor
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(activeFill.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.play_cricket_column_footer_darts, column.dartsThrown),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                stringResource(R.string.play_cricket_column_footer_mpr, column.marksPerRound),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun isTargetKnockedOut(columns: List<CricketBoardColumn>, target: CricketTarget): Boolean {
    val key = target.raw
    return columns.all { (it.marks[key] ?: 0) >= 3 }
}

private fun markGlyphs(marks: Int): String = when {
    marks >= 3 -> "●●●"
    marks == 2 -> "●●"
    marks == 1 -> "●"
    else -> "—"
}
