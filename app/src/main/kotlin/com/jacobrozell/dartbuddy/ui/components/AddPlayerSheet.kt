package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.jacobrozell.dartbuddy.ui.layout.formImeInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.players.PlayerCreateFields
import com.jacobrozell.dartbuddy.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerSheet(
    name: String,
    onNameChange: (String) -> Unit,
    avatarStyle: PlayerAvatarStyle,
    onAvatarStyleChange: (PlayerAvatarStyle) -> Unit,
    colorToken: PlayerColorToken,
    onColorChange: (PlayerColorToken) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(DS.s4).formImeInsets(),
            verticalArrangement = Arrangement.spacedBy(DS.s3),
        ) {
            Text(
                stringResource(R.string.players_add),
                fontWeight = FontWeight.Bold,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            PlayerCreateFields(
                name = name,
                onNameChange = onNameChange,
                avatarStyle = avatarStyle,
                onAvatarStyleChange = onAvatarStyleChange,
                colorToken = colorToken,
                onColorChange = onColorChange,
            )
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_confirm), color = Brand.Green)
            }
            Spacer(Modifier.height(DS.s4))
        }
    }
}
