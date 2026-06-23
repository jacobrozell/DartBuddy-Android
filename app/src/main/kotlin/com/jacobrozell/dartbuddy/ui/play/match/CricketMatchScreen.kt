package com.jacobrozell.dartbuddy.ui.play.match

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.components.BotTurnBanner
import com.jacobrozell.dartbuddy.ui.components.CricketBoardView
import com.jacobrozell.dartbuddy.ui.components.CricketTransposedBoardView
import com.jacobrozell.dartbuddy.ui.components.CricketTapPad
import com.jacobrozell.dartbuddy.ui.components.ForfeitFlowSheets
import com.jacobrozell.dartbuddy.ui.components.MatchExitDialog
import com.jacobrozell.dartbuddy.ui.components.MatchFeedbackBanner
import com.jacobrozell.dartbuddy.ui.components.MatchGameplayHeader
import com.jacobrozell.dartbuddy.ui.components.MatchScoringBody
import com.jacobrozell.dartbuddy.ui.components.cricketSegmentFromPad
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.usesTransposedCricketBoardLayout

@Composable
fun CricketMatchScreen(
    matchId: String,
    onComplete: (String) -> Unit,
    onExit: () -> Unit,
    viewModel: CricketMatchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(matchId) { viewModel.load(matchId) }

    BackHandler { viewModel.showExitDialog(true) }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val session = state.session
    if (session == null) {
        Text(state.errorMessage ?: "Unable to load match", modifier = Modifier.padding(16.dp))
        return
    }

    if (session.state.isComplete) {
        LaunchedEffect(session.entity.id) { onComplete(session.entity.id) }
        return
    }

    val roundLabel = stringResource(
        R.string.play_cricket_roundTurn,
        session.state.roundIndex + 1,
        session.state.currentPlayerIndex + 1,
    )
    val layout = rememberLayoutContext(playerCount = state.participantNames.size)
    val transposedBoard = usesTransposedCricketBoardLayout(layout)
    val boardColumns = viewModel.boardColumns()
    val activeColumn = boardColumns.firstOrNull { it.isActive }

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        MatchGameplayHeader(
            title = stringResource(R.string.play_cricket_title),
            subtitle = roundLabel,
            onExit = { viewModel.showExitDialog(true) },
            onUndoTurn = viewModel::undoTurn,
        )

        MatchScoringBody(
            playerCount = state.participantNames.size,
            scoreboard = {
                if (transposedBoard && activeColumn != null) {
                    CricketTransposedBoardView(
                        column = activeColumn,
                        allColumns = boardColumns,
                    )
                } else {
                    CricketBoardView(columns = boardColumns)
                }
                state.errorMessage?.let { MatchFeedbackBanner(it) }
            },
            pad = {
                BotTurnBanner(visible = state.isBotTurnActive)
                CricketTapPad(
                    enteredDarts = state.currentDarts,
                    selectedMultiplier = state.armedMultiplier,
                    onMultiplierChange = viewModel::setMultiplier,
                    onSegmentTap = { value ->
                        viewModel.addDart(cricketSegmentFromPad(value, state.armedMultiplier))
                    },
                    onMiss = { viewModel.addDart(cricketSegmentFromPad(0, state.armedMultiplier)) },
                    onSubmit = { viewModel.submitTurn(onComplete) },
                    onUndoDart = viewModel::undoDart,
                    onUndoTurn = viewModel::undoTurn,
                    canSubmit = state.currentDarts.isNotEmpty(),
                    enabled = !state.isBotTurnActive,
                )
            },
        )
    }

    if (state.showExitDialog) {
        MatchExitDialog(
            onDismiss = { viewModel.showExitDialog(false) },
            onSaveAndExit = { viewModel.showExitDialog(false); onExit() },
            onSaveAndForfeit = { viewModel.showForfeitDialog(true) },
            onAbandon = { viewModel.abandonMatch(onExit) },
            canForfeit = viewModel.canForfeit(),
            isBotTurnBlocking = state.isBotTurnActive,
        )
    }

    if (state.showForfeitDialog) {
        ForfeitFlowSheets(
            visible = true,
            participantCount = state.participantIds.size,
            humanParticipantIds = viewModel.humanParticipantIds(),
            participantNames = state.participantNames,
            participantIds = state.participantIds,
            onDismiss = { viewModel.showForfeitDialog(false) },
            onConfirm = { forfeiter, winner -> viewModel.forfeit(forfeiter, winner, onComplete) },
            resolveWinner = viewModel::resolveForfeit,
            isPersisting = state.isForfeitPersisting,
        )
    }
}
