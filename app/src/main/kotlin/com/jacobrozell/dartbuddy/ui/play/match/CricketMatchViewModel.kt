package com.jacobrozell.dartbuddy.ui.play.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.DartBotEngine
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.service.CricketSession
import com.jacobrozell.dartbuddy.domain.service.ForfeitWinnerResolution
import com.jacobrozell.dartbuddy.domain.service.MatchForfeitResolver
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.ui.components.CricketBoardColumn
import com.jacobrozell.dartbuddy.ui.theme.PlayerColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class CricketMatchUiState(
    val loading: Boolean = true,
    val session: CricketSession? = null,
    val participantNames: List<String> = emptyList(),
    val participantIds: List<String> = emptyList(),
    val currentDarts: List<DartInput> = emptyList(),
    val armedMultiplier: DartMultiplier = DartMultiplier.Single,
    val errorMessage: String? = null,
    val showExitDialog: Boolean = false,
    val showForfeitDialog: Boolean = false,
    val isForfeitPersisting: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val turnTotalCallerEnabled: Boolean = false,
    val isBotTurnActive: Boolean = false,
    val participantIsBot: Map<String, Boolean> = emptyMap(),
    val botStaggerEnabled: Boolean = true,
    val botDartHapticsEnabled: Boolean = true,
)

@HiltViewModel
class CricketMatchViewModel @Inject constructor(
    private val matchSessionService: MatchSessionService,
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val feedback: FeedbackServices,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CricketMatchUiState())
    val uiState: StateFlow<CricketMatchUiState> = _uiState.asStateFlow()
    private var botTurnJob: Job? = null

    fun load(matchId: String) {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            val session = matchSessionService.loadCricketSession(matchId) ?: run {
                _uiState.value = CricketMatchUiState(loading = false, errorMessage = "Match not found")
                return@launch
            }
            val names = json.decodeFromString<List<String>>(session.entity.participantNamesJson)
            val ids = json.decodeFromString<List<String>>(session.entity.participantIdsJson)
            val botFlags = ids.associateWith { id -> playerRepository.getById(id)?.isBot == true }
            _uiState.value = CricketMatchUiState(
                loading = false,
                session = session,
                participantNames = names,
                participantIds = ids,
                participantIsBot = botFlags,
                hapticsEnabled = settings.hapticsEnabled,
                soundEnabled = settings.soundEnabled,
                turnTotalCallerEnabled = settings.turnTotalCallerEnabled,
                botStaggerEnabled = settings.botStaggerEnabled,
                botDartHapticsEnabled = settings.botDartHapticsEnabled,
            )
            scheduleBotTurnIfNeeded()
        }
    }

    fun boardColumns(): List<CricketBoardColumn> {
        val state = _uiState.value
        val session = state.session ?: return emptyList()
        return session.state.players.mapIndexed { index, player ->
            CricketBoardColumn(
                name = state.participantNames.getOrElse(index) { "Player ${index + 1}" },
                score = player.score,
                marks = player.marks,
                isActive = index == session.state.currentPlayerIndex,
                accentColor = PlayerColors.accent(index),
            )
        }
    }

    fun setMultiplier(multiplier: DartMultiplier) = _uiState.update { it.copy(armedMultiplier = multiplier) }

    fun addDart(dart: DartInput) {
        val state = _uiState.value
        if (state.isBotTurnActive || isCurrentPlayerBot(state)) return
        if (state.currentDarts.size >= 3) return
        feedback.playDartFeedback(dart, state.hapticsEnabled, state.soundEnabled)
        val next = state.currentDarts + dart
        _uiState.update { it.copy(currentDarts = next, armedMultiplier = DartMultiplier.Single) }
        if (next.size == 3) submitTurnInternal()
    }

    fun undoDart() = _uiState.update { it.copy(currentDarts = it.currentDarts.dropLast(1)) }

    fun submitTurn(onComplete: (String) -> Unit) = submitTurnInternal(onComplete)

    private fun submitTurnInternal(onComplete: ((String) -> Unit)? = null) {
        val current = _uiState.value
        val session = current.session ?: return
        if (current.currentDarts.isEmpty()) return
        viewModelScope.launch {
            try {
                val updated = matchSessionService.submitCricketTurn(session, current.currentDarts)
                val matchComplete = updated.state.isComplete
                val visitTotal = updated.events.lastOrNull()?.totalPointsAdded
                if (matchComplete) {
                    feedback.matchFinished(current.hapticsEnabled, current.soundEnabled)
                }
                visitTotal?.let {
                    feedback.announceTurnTotal(it, current.soundEnabled, current.turnTotalCallerEnabled)
                }
                _uiState.update { it.copy(session = updated, currentDarts = emptyList()) }
                if (updated.state.isComplete) {
                    onComplete?.invoke(updated.entity.id)
                } else {
                    scheduleBotTurnIfNeeded(onComplete)
                }
            } catch (e: Exception) {
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
            matchSessionService.undoLastCricketTurn(session)?.let { updated ->
                _uiState.update { it.copy(session = updated, currentDarts = emptyList()) }
                scheduleBotTurnIfNeeded()
            }
        }
    }

    private fun isCurrentPlayerBot(state: CricketMatchUiState): Boolean {
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
                val profile = BotTurnCoordinator.skillProfileFor(player, MatchType.Cricket) ?: return@launch

                _uiState.update { it.copy(isBotTurnActive = true) }
                BotTurnCoordinator.thinkDelay()

                val darts = DartBotEngine.generateCricketTurn(
                    state = session.state,
                    playerIndex = playerIndex,
                    profile = profile,
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
                    val updated = matchSessionService.submitCricketTurn(session, darts)
                    val matchComplete = updated.state.isComplete
                    val visitTotal = updated.events.lastOrNull()?.totalPointsAdded
                    if (matchComplete) {
                        feedback.matchFinished(state.hapticsEnabled, state.soundEnabled)
                    }
                    visitTotal?.let {
                        feedback.announceTurnTotal(it, state.soundEnabled, state.turnTotalCallerEnabled)
                    }
                    _uiState.update { it.copy(session = updated, currentDarts = emptyList(), isBotTurnActive = false) }
                    if (updated.state.isComplete) {
                        onComplete?.invoke(updated.entity.id)
                        return@launch
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
        return MatchForfeitResolver.resolveCricket(
            session.state,
            _uiState.value.participantNames,
            _uiState.value.participantIds,
            forfeitingPlayerId,
        )
    }
}
