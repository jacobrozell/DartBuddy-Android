package com.jacobrozell.dartbuddy.ui.play.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.prefs.RematchStore
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.engine.CricketEngine
import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.domain.service.MatchStatsCalculator
import com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.logging.AnalyticsUserIdentity
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.MatchAnalytics
import com.jacobrozell.dartbuddy.support.logging.info
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class MatchSummaryUiState(
    val loading: Boolean = true,
    val entity: MatchEntity? = null,
    val matchType: MatchType? = null,
    val participantNames: List<String> = emptyList(),
    val participantIds: List<String> = emptyList(),
    val winnerName: String? = null,
    val forfeiterName: String? = null,
    val isForfeited: Boolean = false,
    val playerStats: List<PlayerMatchStats> = emptyList(),
    val setsEnabled: Boolean = false,
    val isUndoing: Boolean = false,
    val undoError: String? = null,
)

@HiltViewModel
class MatchSummaryViewModel @Inject constructor(
    private val matchSessionService: MatchSessionService,
    private val rematchStore: RematchStore,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val feedback: FeedbackServices,
    private val logger: AppLogger,
    private val firebaseBootstrap: FirebaseBootstrap,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchSummaryUiState())
    val uiState: StateFlow<MatchSummaryUiState> = _uiState.asStateFlow()
    private var analyticsLoggedForMatchId: String? = null

    fun load(matchId: String) {
        viewModelScope.launch {
            val entity = matchSessionService.getMatchEntity(matchId) ?: run {
                _uiState.value = MatchSummaryUiState(loading = false)
                return@launch
            }
            val names = json.decodeFromString<List<String>>(entity.participantNamesJson)
            val ids = json.decodeFromString<List<String>>(entity.participantIdsJson)
            val type = MatchType.valueOf(entity.matchType)
            val config = json.decodeFromString<MatchConfigPayload>(entity.configJson)
            val setsEnabled = when (type) {
                MatchType.X01 -> (config as MatchConfigPayload.X01).config.setsEnabled
                MatchType.Cricket -> (config as MatchConfigPayload.Cricket).config.setsEnabled
            }
            val stats = when (type) {
                MatchType.X01 -> {
                    val x01Config = (config as MatchConfigPayload.X01).config
                    val events: List<X01TurnEvent> = if (entity.eventsJson.isBlank() || entity.eventsJson == "[]") {
                        emptyList()
                    } else {
                        json.decodeFromString(entity.eventsJson)
                    }
                    val state = entity.snapshotJson?.let { json.decodeFromString<com.jacobrozell.dartbuddy.domain.engine.X01State>(it) }
                        ?: X01Engine.makeInitialState(x01Config, ids)
                    MatchStatsCalculator.x01Stats(
                        ids,
                        names,
                        events,
                        state.players.associate { it.playerId to it.legsWon },
                        state.players.associate { it.playerId to it.setsWon },
                        entity.winnerPlayerId,
                    )
                }
                MatchType.Cricket -> {
                    val cricketConfig = (config as MatchConfigPayload.Cricket).config
                    val events: List<CricketTurnEvent> = if (entity.eventsJson.isBlank() || entity.eventsJson == "[]") {
                        emptyList()
                    } else {
                        json.decodeFromString(entity.eventsJson)
                    }
                    val state = entity.snapshotJson?.let { json.decodeFromString<com.jacobrozell.dartbuddy.domain.engine.CricketState>(it) }
                        ?: CricketEngine.makeInitialState(cricketConfig, ids)
                    MatchStatsCalculator.cricketStats(
                        ids,
                        names,
                        events,
                        state.players.associate { it.playerId to it.score },
                        state.players.associate { it.playerId to it.legsWon },
                        state.players.associate { it.playerId to it.setsWon },
                        entity.winnerPlayerId,
                    )
                }
            }
            val winnerIndex = ids.indexOf(entity.winnerPlayerId)
            val forfeiterIndex = ids.indexOf(entity.forfeitedByPlayerId)
            val isForfeited = entity.status == MatchStatus.Forfeited.name
            val winnerName = if (winnerIndex >= 0) names[winnerIndex] else null
            _uiState.value = MatchSummaryUiState(
                loading = false,
                entity = entity,
                matchType = type,
                participantNames = names,
                participantIds = ids,
                winnerName = winnerName,
                forfeiterName = if (forfeiterIndex >= 0) names[forfeiterIndex] else null,
                isForfeited = isForfeited,
                playerStats = stats,
                setsEnabled = setsEnabled,
            )
            if (winnerName != null && !isForfeited) {
                val settings = settingsRepository.get()
                feedback.summaryCelebration(settings.hapticsEnabled)
            }
            if (analyticsLoggedForMatchId != entity.id) {
                analyticsLoggedForMatchId = entity.id
                val config = runCatching {
                    json.decodeFromString<MatchConfigPayload>(entity.configJson)
                }.getOrNull()
                if (config != null) {
                    val players = ids.mapNotNull { playerRepository.getById(it) }
                    val metadata = MatchAnalytics.metadata(
                        matchType = type,
                        config = config,
                        participantCount = ids.size,
                        players = players,
                        forfeited = isForfeited,
                    )
                    logger.info(
                        LogCategory.SCORING,
                        eventName = "match_completed",
                        message = "Match completed.",
                        metadata = metadata,
                        correlationId = entity.id,
                    )
                    logger.info(
                        LogCategory.SCORING,
                        eventName = "game_mode_completed",
                        message = "Game mode completed.",
                        metadata = metadata,
                        correlationId = entity.id,
                    )
                    AnalyticsUserIdentity.syncLastGameMode(firebaseBootstrap, type)
                }
            }
        }
    }

    fun prepareRematch() {
        val state = _uiState.value
        rematchStore.pendingPlayerIds = state.participantIds
        rematchStore.pendingMatchType = state.matchType?.name
    }

    fun canUndoLastThrow(): Boolean {
        val entity = _uiState.value.entity ?: return false
        return entity.status == MatchStatus.Completed.name &&
            entity.eventsJson.isNotBlank() &&
            entity.eventsJson != "[]"
    }

    fun undoLastThrow(onResumed: (MatchType, String) -> Unit) {
        val state = _uiState.value
        val entity = state.entity ?: return
        val type = state.matchType ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUndoing = true, undoError = null) }
            try {
                when (type) {
                    MatchType.X01 -> {
                        val session = matchSessionService.loadX01Session(entity.id)
                            ?: error("Match not found")
                        matchSessionService.undoLastX01Turn(session) ?: error("Nothing to undo")
                    }
                    MatchType.Cricket -> {
                        val session = matchSessionService.loadCricketSession(entity.id)
                            ?: error("Match not found")
                        matchSessionService.undoLastCricketTurn(session) ?: error("Nothing to undo")
                    }
                }
                _uiState.update { it.copy(isUndoing = false) }
                onResumed(type, entity.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(isUndoing = false, undoError = e.message) }
            }
        }
    }
}
