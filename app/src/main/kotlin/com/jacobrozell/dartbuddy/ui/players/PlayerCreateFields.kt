package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips

@Composable
fun PlayerCreateFields(
    name: String,
    onNameChange: (String) -> Unit,
    avatarStyle: PlayerAvatarStyle,
    onAvatarStyleChange: (PlayerAvatarStyle) -> Unit,
    colorToken: PlayerColorToken,
    onColorChange: (PlayerColorToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        PlayerAvatar(
            name = name.ifBlank { "?" },
            colorToken = colorToken.raw,
            playerId = "new-player",
            avatarStyleRaw = avatarStyle.raw,
            size = 48.dp,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.players_new_name)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
    Spacer(Modifier.height(DS.s2))
    AvatarStylePicker(selected = avatarStyle, onSelect = onAvatarStyleChange)
    HorizontalOptionChips(
        label = stringResource(R.string.players_edit_color),
        options = PlayerColorToken.all.take(8),
        selected = colorToken,
        onSelect = onColorChange,
        labelFor = { it.raw },
    )
    Text(
        stringResource(R.string.players_create_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
