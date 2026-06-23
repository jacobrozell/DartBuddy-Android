package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.service.ForfeitCandidate
import com.jacobrozell.dartbuddy.domain.service.ForfeitWinnerResolution
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.theme.Brand

private enum class ForfeitStep {
    PickPlayer,
    PickWinner,
    Confirm,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForfeitFlowSheets(
    visible: Boolean,
    participantCount: Int,
    humanParticipantIds: List<String>,
    participantNames: List<String>,
    participantIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (forfeitingId: String, winnerId: String?) -> Unit,
    resolveWinner: (forfeitingId: String) -> ForfeitWinnerResolution,
    isPersisting: Boolean = false,
) {
    if (!visible) return

    var step by remember { mutableStateOf(ForfeitStep.PickPlayer) }
    var forfeitingId by remember { mutableStateOf<String?>(null) }
    var winnerId by remember { mutableStateOf<String?>(null) }
    var tiedCandidates by remember { mutableStateOf<List<ForfeitCandidate>>(emptyList()) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        forfeitingId = null
        winnerId = null
        tiedCandidates = emptyList()
        step = if (participantCount >= 3) {
            ForfeitStep.PickPlayer
        } else {
            val human = humanParticipantIds.firstOrNull() ?: return@LaunchedEffect
            forfeitingId = human
            when (val resolution = resolveWinner(human)) {
                is ForfeitWinnerResolution.Automatic -> {
                    winnerId = resolution.winnerPlayerId
                    ForfeitStep.Confirm
                }
                is ForfeitWinnerResolution.ChooseAmongTied -> {
                    tiedCandidates = resolution.candidates
                    ForfeitStep.PickWinner
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nameById = remember(participantIds, participantNames) {
        participantIds.zip(participantNames).toMap()
    }

    fun selectForfeiting(id: String) {
        forfeitingId = id
        when (val resolution = resolveWinner(id)) {
            is ForfeitWinnerResolution.Automatic -> {
                winnerId = resolution.winnerPlayerId
                step = ForfeitStep.Confirm
            }
            is ForfeitWinnerResolution.ChooseAmongTied -> {
                tiedCandidates = resolution.candidates
                step = ForfeitStep.PickWinner
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DS.s4)
                .padding(bottom = DS.s6),
            verticalArrangement = Arrangement.spacedBy(DS.s3),
        ) {
            when (step) {
                ForfeitStep.PickPlayer -> {
                    Text(
                        stringResource(R.string.play_match_forfeit_pickPlayer_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.play_match_forfeit_pickPlayer_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    humanParticipantIds.forEach { id ->
                        TextButton(
                            onClick = { selectForfeiting(id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(nameById[id] ?: id, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
                ForfeitStep.PickWinner -> {
                    Text(
                        stringResource(R.string.play_match_forfeit_pickWinner_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.play_match_forfeit_pickWinner_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    tiedCandidates.forEach { candidate ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    winnerId = candidate.playerId
                                    step = ForfeitStep.Confirm
                                }
                                .padding(vertical = DS.s2),
                        ) {
                            Text(candidate.displayName, fontWeight = FontWeight.SemiBold)
                            Text(candidate.standingSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
                ForfeitStep.Confirm -> {
                    val forfeiter = forfeitingId?.let { nameById[it] }
                    val winner = winnerId?.let { nameById[it] }
                    Text(
                        stringResource(R.string.play_match_forfeit_confirm_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (participantCount <= 1 || forfeiter == null) {
                            stringResource(R.string.play_match_forfeit_confirm_solo_message)
                        } else {
                            stringResource(R.string.play_match_forfeit_confirm_message, forfeiter, winner ?: "—")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryActionButton(
                        title = stringResource(R.string.play_match_forfeit_confirm_action),
                        onClick = {
                            val id = forfeitingId ?: return@PrimaryActionButton
                            onConfirm(id, winnerId)
                        },
                        loading = isPersisting,
                        accent = Brand.Amber,
                    )
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.play_match_forfeit_confirm_cancel))
                    }
                }
            }
        }
    }
}
