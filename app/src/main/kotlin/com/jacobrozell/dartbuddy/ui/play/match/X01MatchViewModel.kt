package com.jacobrozell.dartbuddy.ui.play.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.DartBotEngine
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.engine.CheckoutSuggester
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.service.ForfeitWinnerResolution
import com.jacobrozell.dartbuddy.domain.service.MatchForfeitResolver
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.domain.service.X01Session
import com.jacobrozell.dartbuddy.domain.service.MatchTurnSupport
import com.jacobrozell.dartbuddy.domain.service.MatchVisitPreview
import com.jacobrozell.dartbuddy.domain.service.StatsService
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.info
import com.jacobrozell.dartbuddy.support.L10n
import com.jacobrozell.dartbuddy.support.MatchConfigText
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class X01PlayerCard(
    val playerId: String,
    val name: String,
    val remainingScore: Int,
    val legsWon: Int,
    val setsWon: Int,
    val dartsThrown: Int,
    val average: Double,
    val accentIndex: Int,
    val colorToken: String?,
)

data class X01MatchUiState(
    val loading: Boolean = true,
    val session: X01Session? = null,
    val participantNames: List<String> = emptyList(),
    val participantIds: List<String> = emptyList(),
    val participantColorTokens: Map<String, String?> = emptyMap(),
    val currentDarts: List<DartInput> = emptyList(),
    val armedMultiplier: DartMultiplier = DartMultiplier.Single,
    val bustMessage: String? = null,
    val errorMessage: String? = null,
    val showExitDialog: Boolean = false,
    val showForfeitDialog: Boolean = false,
    val isForfeitPersisting: Boolean = false,
    val showLegWinBanner: Boolean = false,
    val checkoutRouteIndex: Int = 0,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val turnTotalCallerEnabled: Boolean = false,
    val configSummary: String? = null,
    val isBotTurnActive: Boolean = false,
    val participantIsBot: Map<String, Boolean> = emptyMap(),
    val botStaggerEnabled: Boolean = true,
    val botDartHapticsEnabled: Boolean = true,
)

@HiltViewModel
class X01MatchViewModel @Inject constructor(
    private val matchSessionService: MatchSessionService,
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val feedback: FeedbackServices,
    private val logger: AppLogger,
    private val l10n: L10n,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(X01MatchUiState())
    val uiState: StateFlow<X01MatchUiState> = _uiState.asStateFlow()
    private var botTurnJob: Job? = null

    fun load(matchId: String) {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            val session = matchSessionService.loadX01Session(matchId)
            if (session == null) {
                _uiState.value = X01MatchUiState(loading = false, errorMessage = "Match not found")
                return@launch
            }
            val names = json.decodeFromString<List<String>>(session.entity.participantNamesJson)
            val ids = json.decodeFromString<List<String>>(session.entity.participantIdsJson)
            val colorTokens = ids.associateWith { id -> playerRepository.getById(id)?.colorToken }
            val botFlags = ids.associateWith { id -> playerRepository.getById(id)?.isBot == true }
            _uiState.value = X01MatchUiState(
                loading = false,
                session = session,
                participantNames = names,
                participantIds = ids,
                participantColorTokens = colorTokens,
                participantIsBot = botFlags,
                hapticsEnabled = settings.hapticsEnabled,
                soundEnabled = settings.soundEnabled,
                turnTotalCallerEnabled = settings.turnTotalCallerEnabled,
                botStaggerEnabled = settings.botStaggerEnabled,
                botDartHapticsEnabled = settings.botDartHapticsEnabled,
                configSummary = buildConfigSummary(session),
            )
            logger.info(
                LogCategory.UI,
                eventName = "match_screen_appeared",
                message = "Match screen appeared.",
                metadata = mapOf("matchType" to MatchType.X01.name.lowercase()),
                correlationId = matchId,
            )
            scheduleBotTurnIfNeeded()
        }
    }

    fun playerCards(): List<X01PlayerCard> {
        val state = _uiState.value
        val session = state.session ?: return emptyList()
        val events = session.events
        val currentIndex = session.state.currentPlayerIndex
        val canHumanInput = !state.isBotTurnActive && !isCurrentPlayerBot(state)
        return session.state.players.mapIndexed { index, player ->
            val isActive = index == currentIndex
            val stats = statsForPlayer(
                playerId = player.playerId,
                events = events,
                isActive = isActive,
                canHumanInput = canHumanInput,
                isBotPlaying = state.isBotTurnActive,
                isCurrentPlayerBot = state.participantIsBot[player.playerId] == true,
                currentDarts = if (isActive) state.currentDarts else emptyList(),
                checkInMode = session.config.checkInMode,
                isCheckedIn = player.isCheckedIn,
            )
            X01PlayerCard(
                playerId = player.playerId,
                name = state.participantNames.getOrElse(index) { "Player ${index + 1}" },
                remainingScore = player.remainingScore,
                legsWon = player.legsWon,
                setsWon = player.setsWon,
                dartsThrown = stats.first,
                average = stats.second,
                accentIndex = index,
                colorToken = state.participantColorTokens[player.playerId],
            )
        }
    }

    private fun statsForPlayer(
        playerId: String,
        events: List<X01TurnEvent>,
        isActive: Boolean,
        canHumanInput: Boolean,
        isBotPlaying: Boolean,
        isCurrentPlayerBot: Boolean,
        currentDarts: List<DartInput>,
        checkInMode: X01CheckInMode,
        isCheckedIn: Boolean,
    ): Pair<Int, Double> {
        val playerEvents = events.filter { it.playerId == playerId }
        val committedDarts = StatsService.x01CommittedDarts(playerEvents)
        val committedPoints = StatsService.x01CommittedPoints(playerEvents)
        val includesPreview = MatchVisitPreview.includesActiveVisit(
            isActive = isActive,
            canHumanInput = canHumanInput,
            isBotPlaying = isBotPlaying,
            isCurrentPlayerBot = isCurrentPlayerBot,
        )
        val previewDarts = if (includesPreview) currentDarts.size else 0
        val previewPoints = if (includesPreview) {
            X01Engine.scoreVisit(currentDarts, checkInMode, isCheckedIn).appliedTotal
        } else {
            0
        }
        val darts = committedDarts + previewDarts
        val average = StatsService.x01LiveScorecardAverage(
            committedPoints = committedPoints,
            committedDarts = committedDarts,
            previewPoints = previewPoints,
            previewDarts = previewDarts,
        )
        return darts to average
    }

    fun setMultiplier(multiplier: DartMultiplier) = _uiState.update { it.copy(armedMultiplier = multiplier) }

    fun addDart(dart: DartInput) {
        val state = _uiState.value
        if (state.isBotTurnActive || isCurrentPlayerBot(state)) return
        if (state.currentDarts.size >= 3) return
        feedback.playDartFeedback(dart, state.hapticsEnabled, state.soundEnabled)
        val next = state.currentDarts + dart
        _uiState.update { it.copy(currentDarts = next, armedMultiplier = DartMultiplier.Single, bustMessage = null) }
        val session = state.session ?: return
        val player = session.state.players[session.state.currentPlayerIndex]
        val visitScore = X01Engine.scoreVisit(next, session.config.checkInMode, player.isCheckedIn)
        val shouldAutoSubmit = next.size == 3 || visitScore.appliedTotal >= player.remainingScore
        if (shouldAutoSubmit) {
            submitTurnInternal()
        }
    }

    fun checkInBannerText(): String? {
        val state = _uiState.value
        val session = state.session ?: return null
        if (session.config.checkInMode == X01CheckInMode.StraightIn) return null
        if (state.isBotTurnActive || isCurrentPlayerBot(state)) return null
        if (state.bustMessage != null || state.showLegWinBanner) return null
        val player = session.state.players[session.state.currentPlayerIndex]
        if (player.isCheckedIn) return null
        return when (session.config.checkInMode) {
            X01CheckInMode.DoubleIn -> l10n.get(R.string.play_x01_checkIn_banner_doubleIn)
            X01CheckInMode.MasterIn -> l10n.get(R.string.play_x01_checkIn_banner_masterIn)
            X01CheckInMode.StraightIn -> null
        }
    }

    fun visitDartAlphas(): List<Float> {
        val state = _uiState.value
        val session = state.session ?: return emptyList()
        val player = session.state.players[session.state.currentPlayerIndex]
        val scored = X01Engine.scoreVisit(state.currentDarts, session.config.checkInMode, player.isCheckedIn)
        return scored.dartCounts.map { if (it) 1f else 0.38f }
    }

    fun showsCheckoutSuggestions(): Boolean {
        val state = _uiState.value
        val session = state.session ?: return false
        if (session.config.checkInMode == X01CheckInMode.StraightIn) return true
        val player = session.state.players[session.state.currentPlayerIndex]
        if (player.isCheckedIn) return true
        val visitScore = X01Engine.scoreVisit(state.currentDarts, session.config.checkInMode, player.isCheckedIn)
        return visitScore.checkedInAfter
    }

    fun undoDart() {
        val session = _uiState.value.session ?: return
        if (_uiState.value.currentDarts.isEmpty()) return
        _uiState.update { it.copy(currentDarts = it.currentDarts.dropLast(1)) }
        MatchTurnSupport.logDartUndone(
            logger,
            session.entity.id,
            MatchType.X01,
            MatchTurnSupport.progressMetadata(
                MatchType.X01,
                session.events.size,
                session.state.legIndex,
                session.state.setIndex,
                session.entity.status,
            ),
        )
    }

    fun checkoutRoutes(): List<List<String>> {
        val state = _uiState.value
        val session = state.session ?: return emptyList()
        if (session.state.isComplete || !showsCheckoutSuggestions()) return emptyList()
        val player = session.state.players[session.state.currentPlayerIndex]
        val dartsLeft = maxOf(1, 3 - state.currentDarts.size)
        val visitScore = X01Engine.scoreVisit(state.currentDarts, session.config.checkInMode, player.isCheckedIn)
        val previewRemaining = player.remainingScore - visitScore.appliedTotal
        return CheckoutSuggester.allSuggestions(previewRemaining, session.config.checkoutMode, dartsLeft)
    }

    fun cycleCheckoutRoute() = _uiState.update { state ->
        val routes = checkoutRoutes()
        if (routes.isEmpty()) state else state.copy(checkoutRouteIndex = (state.checkoutRouteIndex + 1) % routes.size)
    }

    fun checkoutDisplayLabel(token: String): String =
        com.jacobrozell.dartbuddy.domain.scoring.DartAccessibility.checkoutDisplayLabel(
            token,
            { res, args -> l10n.format(res, *args) },
            { res -> l10n.get(res) },
        )

    fun submitTurn(onComplete: (String) -> Unit) = submitTurnInternal(onComplete)

    private fun submitTurnInternal(onComplete: ((String) -> Unit)? = null) {
        val current = _uiState.value
        val session = current.session ?: return
        if (current.currentDarts.isEmpty()) return
        viewModelScope.launch {
            try {
                val updated = matchSessionService.submitX01Turn(session, null, current.currentDarts)
                MatchTurnSupport.logTurnSubmitted(logger, session.entity.id, updated)
                val busted = updated.events.lastOrNull()?.isBust == true
                val checkedOut = updated.events.lastOrNull()?.didCheckout == true
                val legWon = checkedOut && !updated.state.isComplete
                val matchComplete = updated.state.isComplete
                val appliedTotal = updated.events.lastOrNull()?.appliedTotal
                if (busted) {
                    feedback.bust(current.hapticsEnabled)
                } else if (matchComplete) {
                    feedback.matchFinished(current.hapticsEnabled, current.soundEnabled)
                } else if (legWon) {
                    feedback.legFinished(current.hapticsEnabled, current.soundEnabled)
                }
                appliedTotal?.let {
                    feedback.announceTurnTotal(it, current.soundEnabled, current.turnTotalCallerEnabled)
                }
                _uiState.update {
                    it.copy(
                        session = updated,
                        currentDarts = emptyList(),
                        checkoutRouteIndex = 0,
                        bustMessage = if (busted) l10n.get(R.string.play_x01_bustFeedback) else null,
                        showLegWinBanner = legWon,
                    )
                }
                if (updated.state.isComplete || updated.entity.status == "Forfeited") {
                    onComplete?.invoke(updated.entity.id)
                } else {
                    scheduleBotTurnIfNeeded(onComplete)
                }
            } catch (e: Exception) {
                MatchTurnSupport.logTurnPersistFailed(
                    logger,
                    session.entity.id,
                    MatchType.X01,
                    e.message ?: "Turn persist failed.",
                )
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun undoTurn() {
        val state = _uiState.value
        if (state.isBotTurnActive) return
        val session = state.session ?: return
        botTurnJob?.cancel()
        viewModelScope.launch {
            try {
                matchSessionService.undoLastX01Turn(session)?.let { updated ->
                    MatchTurnSupport.logTurnUndone(
                        logger,
                        session.entity.id,
                        MatchType.X01,
                        MatchTurnSupport.progressMetadata(
                            MatchType.X01,
                            updated.events.size,
                            updated.state.legIndex,
                            updated.state.setIndex,
                            updated.entity.status,
                        ),
                    )
                    _uiState.update { it.copy(session = updated, currentDarts = emptyList(), bustMessage = null) }
                    scheduleBotTurnIfNeeded()
                }
            } catch (e: Exception) {
                MatchTurnSupport.logTurnUndoFailed(
                    logger,
                    session.entity.id,
                    MatchType.X01,
                    e.message ?: "Undo failed.",
                )
            }
        }
    }

    private fun isCurrentPlayerBot(state: X01MatchUiState): Boolean {
        val session = state.session ?: return false
        val playerId = session.state.players[session.state.currentPlayerIndex].playerId
        return state.participantIsBot[playerId] == true
    }

    private fun scheduleBotTurnIfNeeded(onComplete: ((String) -> Unit)? = null) {
        botTurnJob?.cancel()
        botTurnJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                val session = state.session ?: return@launch
                if (session.state.isComplete || state.currentDarts.isNotEmpty()) return@launch

                val playerIndex = session.state.currentPlayerIndex
                val playerId = session.state.players[playerIndex].playerId
                if (state.participantIsBot[playerId] != true) return@launch

                val player = playerRepository.getById(playerId) ?: return@launch
                val profile = playerRepository.resolveBotSkillProfile(playerId, MatchType.X01) ?: return@launch

                _uiState.update { it.copy(isBotTurnActive = true) }
                BotTurnCoordinator.thinkDelay()

                val x01Player = session.state.players[playerIndex]
                val darts = DartBotEngine.generateX01Turn(
                    remaining = x01Player.remainingScore,
                    profile = profile,
                    checkoutMode = session.config.checkoutMode,
                    checkInMode = session.config.checkInMode,
                    isCheckedIn = x01Player.isCheckedIn,
                )

                if (state.botStaggerEnabled) {
                    BotTurnCoordinator.playStaggeredDarts(
                        darts = darts,
                        soundEnabled = state.soundEnabled,
                        botDartHapticsEnabled = state.botDartHapticsEnabled,
                        feedback = feedback,
                        onDart = { partial -> _uiState.update { it.copy(currentDarts = partial) } },
                    )
                }

                try {
                    val updated = matchSessionService.submitX01Turn(session, null, darts)
                    MatchTurnSupport.logTurnSubmitted(logger, session.entity.id, updated)
                    val busted = updated.events.lastOrNull()?.isBust == true
                    val checkedOut = updated.events.lastOrNull()?.didCheckout == true
                    val legWon = checkedOut && !updated.state.isComplete
                    val matchComplete = updated.state.isComplete
                    val appliedTotal = updated.events.lastOrNull()?.appliedTotal
                    if (busted) {
                        feedback.bust(state.hapticsEnabled)
                    } else if (matchComplete) {
                        feedback.matchFinished(state.hapticsEnabled, state.soundEnabled)
                    } else if (legWon) {
                        feedback.legFinished(state.hapticsEnabled, state.soundEnabled)
                    }
                    appliedTotal?.let {
                        feedback.announceTurnTotal(it, state.soundEnabled, state.turnTotalCallerEnabled)
                    }
                    _uiState.update {
                        it.copy(
                            session = updated,
                            currentDarts = emptyList(),
                            checkoutRouteIndex = 0,
                            bustMessage = if (busted) l10n.get(R.string.play_x01_bustFeedback) else null,
                            showLegWinBanner = legWon,
                            isBotTurnActive = false,
                        )
                    }
                    if (updated.state.isComplete || updated.entity.status == "Forfeited") {
                        onComplete?.invoke(updated.entity.id)
                        return@launch
                    }
                    if (legWon) {
                        kotlinx.coroutines.delay(1_200)
                        _uiState.update { it.copy(showLegWinBanner = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message, isBotTurnActive = false) }
                    return@launch
                }
            }
        }
    }

    fun showExitDialog(show: Boolean) = _uiState.update { it.copy(showExitDialog = show) }
    fun showForfeitDialog(show: Boolean) = _uiState.update { it.copy(showForfeitDialog = show, showExitDialog = false) }
    fun dismissLegWinBanner() = _uiState.update { it.copy(showLegWinBanner = false) }

    fun abandonMatch(onDone: () -> Unit) {
        val id = _uiState.value.session?.entity?.id ?: return
        viewModelScope.launch {
            matchRepository.deleteMatch(id)
            onDone()
        }
    }

    fun forfeit(forfeitingPlayerId: String, winnerPlayerId: String?, onComplete: (String) -> Unit) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isForfeitPersisting = true) }
            try {
                val updated = matchSessionService.forfeitMatch(session, forfeitingPlayerId, winnerPlayerId)
                _uiState.update { it.copy(session = updated, showForfeitDialog = false, isForfeitPersisting = false) }
                onComplete(updated.entity.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, showForfeitDialog = false, isForfeitPersisting = false) }
            }
        }
    }

    fun humanParticipantIds(): List<String> = MatchForfeitResolver.humanParticipantIds(
        _uiState.value.participantIds,
        _uiState.value.participantIsBot,
    )

    fun canForfeit(): Boolean = _uiState.value.session?.events?.isNotEmpty() == true

    fun resolveForfeit(forfeitingPlayerId: String): ForfeitWinnerResolution {
        val session = _uiState.value.session ?: return ForfeitWinnerResolution.Automatic(null)
        return MatchForfeitResolver.resolveX01(
            session.state,
            _uiState.value.participantNames,
            _uiState.value.participantIds,
            forfeitingPlayerId,
        )
    }

    private fun buildConfigSummary(session: X01Session): String =
        MatchConfigText.x01InlineConfig(session.config, l10n)
}
