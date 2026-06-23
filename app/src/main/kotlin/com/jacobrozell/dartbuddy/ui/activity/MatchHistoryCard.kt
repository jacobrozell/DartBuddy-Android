package com.jacobrozell.dartbuddy.ui.activity

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.support.HistoryListRow
import com.jacobrozell.dartbuddy.support.HistoryStanding
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.theme.Brand
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MatchHistoryCard(
    row: HistoryListRow,
    modifier: Modifier = Modifier,
    dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        .withZone(ZoneId.systemDefault()),
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    val dateText = dateFormatter.format(Instant.ofEpochMilli(row.updatedAtEpochMillis))

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DS.radiusMd),
        color = cardBg,
    ) {
        Column(
            modifier = Modifier.padding(DS.s4),
            verticalArrangement = Arrangement.spacedBy(DS.s2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DS.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameModeBadge(row.matchType.name)
                Text(
                    dateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                HistoryStatusBadge(
                    isForfeited = row.isForfeited,
                    isFinished = row.isFinished,
                )
            }
            Text(
                row.configText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            row.standings.forEachIndexed { index, standing ->
                HistoryStandingRow(
                    index = index,
                    standing = standing,
                    matchType = row.matchType,
                )
            }
        }
    }
}

@Composable
private fun HistoryStatusBadge(isForfeited: Boolean, isFinished: Boolean) {
    when {
        isForfeited -> StatusBadge(stringResource(R.string.history_status_forfeit), Brand.Amber)
        isFinished -> StatusBadge(stringResource(R.string.history_status_finished), Brand.Green)
    }
}

@Composable
private fun StatusBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(DS.radiusXs),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun HistoryStandingRow(
    index: Int,
    standing: HistoryStanding,
    matchType: MatchType,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}. ${standing.name}",
            modifier = Modifier.weight(1f),
            fontWeight = if (standing.isWinner) FontWeight.SemiBold else FontWeight.Normal,
            color = if (standing.isWinner) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 2,
        )
        Column(horizontalAlignment = Alignment.End) {
            if (matchType == MatchType.X01 && (standing.sets > 0 || standing.legs > 0)) {
                Text(
                    stringResource(R.string.history_standing_sets_legs_format, standing.sets, standing.legs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${standing.score}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (standing.isWinner) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
