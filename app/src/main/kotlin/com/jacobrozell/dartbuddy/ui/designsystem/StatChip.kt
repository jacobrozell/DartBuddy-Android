package com.jacobrozell.dartbuddy.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val secondary = if (isSystemInDarkTheme()) Brand.TextSecondaryDark else Brand.TextSecondaryLight
    val tileA11y = stringResource(R.string.stats_stat_tile_accessibility_format, label, value)
    Column(
        modifier = modifier
            .semantics {
                contentDescription = tileA11y
            },
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = if (compact) 14.sp else 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
