package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken

@androidx.compose.runtime.Composable
fun PlayerAvatar(
    name: String,
    colorToken: String?,
    playerId: String,
    size: Dp,
    avatarStyleRaw: String? = null,
    isBot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val token = PlayerColorToken.resolve(colorToken ?: PlayerColorToken.defaultForPlayer(playerId).raw)
    val style = PlayerAvatarStyle.resolve(
        avatarStyleRaw ?: PlayerAvatarStyle.defaultForPlayer(playerId, isBot).raw,
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(token.color),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = name,
            tint = Color.White,
            modifier = Modifier
                .size(size * 0.48f)
                .then(if (style == PlayerAvatarStyle.Dart) Modifier.rotate(135f) else Modifier),
        )
    }
}

fun playerAccentColor(colorToken: String?, playerId: String): Color {
    val token = PlayerColorToken.resolve(colorToken ?: PlayerColorToken.defaultForPlayer(playerId).raw)
    return token.color
}
