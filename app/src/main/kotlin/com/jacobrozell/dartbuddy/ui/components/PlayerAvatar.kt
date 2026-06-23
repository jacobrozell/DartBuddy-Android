package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken

@Composable
fun PlayerAvatar(
    name: String,
    colorToken: String?,
    playerId: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val token = PlayerColorToken.resolve(colorToken ?: PlayerColorToken.defaultForPlayer(playerId).raw)
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(token.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

fun playerAccentColor(colorToken: String?, playerId: String): Color {
    val token = PlayerColorToken.resolve(colorToken ?: PlayerColorToken.defaultForPlayer(playerId).raw)
    return token.color
}
