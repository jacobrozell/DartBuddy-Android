package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.jacobrozell.dartbuddy.domain.engine.CricketTarget
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun CricketMarksBoard(
    marks: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CricketTarget.allCases.forEach { target ->
            val count = marks[target.raw] ?: 0
            val closed = count >= 3
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (closed) Brand.ProBot.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (target == CricketTarget.Bull) "B" else target.raw,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(markDisplay(count), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun markDisplay(marks: Int): String = when {
    marks >= 3 -> "●●●"
    marks == 2 -> "●●"
    marks == 1 -> "●"
    else -> "—"
}
