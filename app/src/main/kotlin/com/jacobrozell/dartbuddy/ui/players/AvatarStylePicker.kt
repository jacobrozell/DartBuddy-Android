package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.theme.Brand

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarStylePicker(
    selected: PlayerAvatarStyle,
    onSelect: (PlayerAvatarStyle) -> Unit,
    modifier: Modifier = Modifier,
    showsTitle: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        if (showsTitle) {
            Text(
                stringResource(com.jacobrozell.dartbuddy.R.string.players_edit_avatar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DS.s2),
            verticalArrangement = Arrangement.spacedBy(DS.s2),
        ) {
            PlayerAvatarStyle.all.forEach { style ->
                val label = stringResource(style.labelRes)
                val isSelected = style == selected
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Brand.Green.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Brand.Green else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(style) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    com.jacobrozell.dartbuddy.ui.components.PlayerAvatar(
                        name = label,
                        colorToken = com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.Green.raw,
                        playerId = style.raw,
                        size = 32.dp,
                        avatarStyleRaw = style.raw,
                    )
                }
            }
        }
    }
}
