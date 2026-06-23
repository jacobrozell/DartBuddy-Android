package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.ui.layout.matchHeaderCutoutPadding
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun MatchGameplayHeader(
    title: String,
    onExit: () -> Unit,
    onUndoTurn: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .matchHeaderCutoutPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit, modifier = Modifier.a11yId(AccessibilityIds.MATCH_EXIT)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.play_match_exit_title))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = RoundedCornerShape(10.dp), color = cardBg) {
            IconButton(onClick = onUndoTurn, modifier = Modifier.size(44.dp).a11yId(AccessibilityIds.MATCH_UNDO)) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.match_undo),
                    tint = Brand.Green,
                )
            }
        }
    }
}
