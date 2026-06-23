package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun BotTurnBanner(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    val dark = isSystemInDarkTheme()
    val bgAlpha = if (dark) 0.32f else 0.22f
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = DS.s1)
            .background(Brand.Amber.copy(alpha = bgAlpha), RoundedCornerShape(DS.radiusSm))
            .padding(horizontal = DS.s4, vertical = DS.s2),
        horizontalArrangement = Arrangement.spacedBy(DS.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.play_match_bot_throwing),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
