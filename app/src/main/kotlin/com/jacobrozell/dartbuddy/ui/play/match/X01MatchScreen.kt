package com.jacobrozell.dartbuddy.ui.play.match

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.ui.components.BotTurnBanner
import com.jacobrozell.dartbuddy.ui.components.CheckoutSuggestionBanner
import com.jacobrozell.dartbuddy.ui.components.DartNumberPad
import com.jacobrozell.dartbuddy.ui.components.ForfeitFlowSheets
import com.jacobrozell.dartbuddy.ui.components.MatchExitDialog
import com.jacobrozell.dartbuddy.ui.components.MatchFeedbackBanner
import com.jacobrozell.dartbuddy.ui.components.MatchFeedbackBannerStyle
import com.jacobrozell.dartbuddy.ui.components.MatchGameplayHeader
import com.jacobrozell.dartbuddy.ui.components.MatchScoringBody
import com.jacobrozell.dartbuddy.ui.components.PlayerScoreCard
import com.jacobrozell.dartbuddy.ui.components.segmentFromPadValue
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.scoringPadColumnCount
import com.jacobrozell.dartbuddy.ui.layout.usesIPadPortraitTwoPlayerScoreRow
import com.jacobrozell.dartbuddy.ui.layout.usesPinnedActiveX01PlayerCard
import com.jacobrozell.dartbuddy.ui.layout.usesAccessibilityMatchScoringLayout
import com.jacobrozell.dartbuddy.ui.layout.usesStackedPlayerScoreCardLayout
import com.jacobrozell.dartbuddy.ui.layout.usesWidePlayerScoreCardLayout
import com.jacobrozell.dartbuddy.ui.theme.PlayerColors
import kotlinx.coroutines.delay

@Composable
fun X01MatchScreen(
    matchId: String,
    onComplete: (String) -> Unit,
    onExit: () -> Unit,
    viewModel: X01MatchViewModel = hiltViewModel(),
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

    val participantIds = remember(session.entity.participantIdsJson) {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(session.entity.participantIdsJson)
    }

    if (session.state.isComplete) {
        LaunchedEffect(session.entity.id) { onComplete(session.entity.id) }
        return
    }

    val gameState = session.state
    val currentIndex = gameState.currentPlayerIndex
    val routes = viewModel.checkoutRoutes()
    val cards = viewModel.playerCards()
    val layout = rememberLayoutContext(playerCount = cards.size)
    val landscape = layout.isLandscape
    val wideCards = usesWidePlayerScoreCardLayout(layout) && !usesStackedPlayerScoreCardLayout(layout)
    val stackedCards = usesStackedPlayerScoreCardLayout(layout)
    val hideInlineStats = usesAccessibilityMatchScoringLayout(layout)
    val padColumns = scoringPadColumnCount(layout)
    val visitDartAlphas = viewModel.visitDartAlphas()
    val checkInBanner = viewModel.checkInBannerText()
    val activeCard = cards.getOrNull(currentIndex)
    val inactiveCards = if (activeCard != null) cards.filterIndexed { index, _ -> index != currentIndex } else cards
    val pinsActive = usesPinnedActiveX01PlayerCard(layout)

    LaunchedEffect(state.showLegWinBanner) {
        if (state.showLegWinBanner) {
            delay(1_200)
            viewModel.dismissLegWinBanner()
        }
    }

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        MatchGameplayHeader(
            title = stringResource(R.string.play_x01_title),
            subtitle = state.configSummary,
            onExit = { viewModel.showExitDialog(true) },
            onUndoTurn = viewModel::undoTurn,
        )

        MatchScoringBody(
            playerCount = cards.size,
            activeBand = {
                if (pinsActive && activeCard != null) {
                    X01PlayerScoreCard(
                        card = activeCard,
                        isActive = true,
                        visitDarts = state.currentDarts,
                        dartSlotAlphas = visitDartAlphas,
                        useWideLayout = wideCards,
                        useStackedLayout = stackedCards,
                        hideInlineStats = hideInlineStats,
                    )
                }
            },
            scoreboard = {
                val scoreCards = if (pinsActive) inactiveCards else cards
                if (usesIPadPortraitTwoPlayerScoreRow(layout, scoreCards.size)) {
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        scoreCards.forEach { card ->
                            X01PlayerScoreCard(
                                card = card,
                                isActive = !pinsActive && card.playerId == activeCard?.playerId,
                                visitDarts = if (!pinsActive && card.playerId == activeCard?.playerId) state.currentDarts else emptyList(),
                                dartSlotAlphas = if (!pinsActive && card.playerId == activeCard?.playerId) visitDartAlphas else emptyList(),
                                useWideLayout = wideCards,
                                useStackedLayout = stackedCards,
                                hideInlineStats = hideInlineStats,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    scoreCards.forEach { card ->
                        X01PlayerScoreCard(
                            card = card,
                            isActive = !pinsActive && card.playerId == activeCard?.playerId,
                            visitDarts = if (!pinsActive && card.playerId == activeCard?.playerId) state.currentDarts else emptyList(),
                            dartSlotAlphas = if (!pinsActive && card.playerId == activeCard?.playerId) visitDartAlphas else emptyList(),
                            useWideLayout = wideCards,
                            useStackedLayout = stackedCards,
                            hideInlineStats = hideInlineStats,
                        )
                    }
                }
            },
            padChrome = {
                if (routes.isNotEmpty()) {
                    CheckoutSuggestionBanner(
                        routes = routes,
                        selectedIndex = state.checkoutRouteIndex.coerceIn(0, routes.lastIndex.coerceAtLeast(0)),
                        displayLabel = viewModel::checkoutDisplayLabel,
                        onCycle = viewModel::cycleCheckoutRoute,
                    )
                }
                if (state.showLegWinBanner) {
                    MatchFeedbackBanner(
                        message = stringResource(R.string.play_x01_legWon_banner),
                        style = MatchFeedbackBannerStyle.LegWin,
                    )
                } else {
                    state.bustMessage?.let { MatchFeedbackBanner(it) }
                        ?: checkInBanner?.let {
                            MatchFeedbackBanner(
                                message = it,
                                style = MatchFeedbackBannerStyle.CheckIn,
                            )
                        }
                }
                BotTurnBanner(visible = state.isBotTurnActive)
                state.errorMessage?.let { MatchFeedbackBanner(it) }
            },
            pad = {
                DartNumberPad(
                    enteredDarts = state.currentDarts,
                    selectedMultiplier = state.armedMultiplier,
                    onMultiplierChange = viewModel::setMultiplier,
                    onSegmentTap = { value -> viewModel.addDart(segmentFromPadValue(value, state.armedMultiplier)) },
                    onMiss = { viewModel.addDart(segmentFromPadValue(0, state.armedMultiplier)) },
                    onUndoDart = viewModel::undoDart,
                    onUndoTurn = viewModel::undoTurn,
                    showsVisitPreview = !landscape,
                    enabled = !state.isBotTurnActive,
                    dartSlotAlphas = visitDartAlphas,
                    columnCount = padColumns,
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
            participantCount = participantIds.size,
            humanParticipantIds = viewModel.humanParticipantIds(),
            participantNames = state.participantNames,
            participantIds = participantIds,
            onDismiss = { viewModel.showForfeitDialog(false) },
            onConfirm = { forfeiter, winner -> viewModel.forfeit(forfeiter, winner, onComplete) },
            resolveWinner = viewModel::resolveForfeit,
            isPersisting = state.isForfeitPersisting,
        )
    }
}

@Composable
private fun X01PlayerScoreCard(
    card: X01PlayerCard,
    isActive: Boolean,
    visitDarts: List<DartInput>,
    dartSlotAlphas: List<Float> = emptyList(),
    useWideLayout: Boolean = false,
    useStackedLayout: Boolean = false,
    hideInlineStats: Boolean = false,
    modifier: Modifier = Modifier,
) {
    PlayerScoreCard(
        modifier = modifier,
        name = card.name,
        score = card.remainingScore,
        setsWon = card.setsWon,
        legsWon = card.legsWon,
        isActive = isActive,
        accentColor = PlayerColors.fromToken(card.colorToken, card.playerId),
        visitDarts = visitDarts,
        dartsThrown = card.dartsThrown,
        average = card.average,
        dartSlotAlphas = dartSlotAlphas,
        useWideLayout = useWideLayout,
        useStackedLayout = useStackedLayout,
        hideInlineStats = hideInlineStats,
    )
}
