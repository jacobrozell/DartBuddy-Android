package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchExitDialog(
    onDismiss: () -> Unit,
    onSaveAndExit: () -> Unit,
    onSaveAndForfeit: () -> Unit,
    onAbandon: () -> Unit,
    canForfeit: Boolean = true,
    isBotTurnBlocking: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            Text(
                stringResource(R.string.play_match_exit_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.play_match_exit_message),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onSaveAndExit,
                enabled = !isBotTurnBlocking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.play_match_exit_saveAndExit), color = Brand.Green)
            }
            if (canForfeit) {
                TextButton(
                    onClick = onSaveAndForfeit,
                    enabled = !isBotTurnBlocking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.play_match_exit_saveAndForfeit), color = Brand.RedAccent)
                }
            }
            TextButton(
                onClick = onAbandon,
                enabled = !isBotTurnBlocking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.play_match_exit_abandon), color = Brand.RedAccent)
            }
            if (isBotTurnBlocking) {
                Text(
                    stringResource(R.string.play_match_exit_disabledWhileBot),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_stay))
            }
        }
    }
}
