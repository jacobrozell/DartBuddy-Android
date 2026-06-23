package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.domain.engine.CricketTarget
import com.jacobrozell.dartbuddy.ui.theme.Brand

data class CricketBoardColumn(
    val name: String,
    val score: Int,
    val marks: Map<String, Int>,
    val isActive: Boolean,
    val accentColor: Color,
)

@Composable
fun CricketBoardView(
    columns: List<CricketBoardColumn>,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val activeFill = if (dark) Brand.CardElevatedDark.copy(alpha = 0.5f) else Brand.CardElevatedLight.copy(alpha = 0.6f)
    val scroll = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.width(36.dp)) {
            Box(Modifier.height(52.dp))
            CricketTarget.allCases.forEach { target ->
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (target == CricketTarget.Bull) "B" else target.raw,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Box(Modifier.height(48.dp))
        }

        columns.forEach { column ->
            Column(
                modifier = Modifier
                    .width(88.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (column.isActive) activeFill else MaterialTheme.colorScheme.surfaceVariant)
                    .semantics {
                        contentDescription = "${column.name}, score ${column.score}"
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        column.name,
                        fontWeight = if (column.isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (column.isActive) column.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                CricketTarget.allCases.forEach { target ->
                    val count = column.marks[target.raw] ?: 0
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            markGlyphs(count),
                            fontWeight = FontWeight.Bold,
                            color = if (count >= 3) column.accentColor else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${column.score}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (column.isActive) column.accentColor else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun markGlyphs(marks: Int): String = when {
    marks >= 3 -> "●●●"
    marks == 2 -> "●●"
    marks == 1 -> "●"
    else -> "—"
}
