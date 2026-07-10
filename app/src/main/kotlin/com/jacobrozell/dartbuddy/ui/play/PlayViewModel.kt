package com.jacobrozell.dartbuddy.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.prefs.RematchStore
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.domain.surface.ProductSurface
import com.jacobrozell.dartbuddy.domain.engine.CricketEngine
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.MatchAnalytics
import com.jacobrozell.dartbuddy.support.logging.AnalyticsUserIdentity
import com.jacobrozell.dartbuddy.support.logging.error
import com.jacobrozell.dartbuddy.support.logging.info
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.L10n
import com.jacobrozell.dartbuddy.support.MatchConfigText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class PlayUiState(
    val players: List<com.jacobrozell.dartbuddy.data.repository.PlayerSummary> = emptyList(),
    val selectedPlayerIds: List<String> = emptyList(),
    val setupMode: MatchType = MatchType.X01,
    val activeMatch: MatchEntity? = null,
    val settings: AppSettings = AppSettings(),
    val x01StartScore: Int = 501,
    val x01LegsToWin: Int = 1,
    val x01SetsEnabled: Boolean = false,
    val x01SetsToWin: Int = 1,
    val x01CheckoutMode: X01CheckoutMode = X01CheckoutMode.DoubleOut,
    val x01CheckInMode: X01CheckInMode = X01CheckInMode.StraightIn,
    val x01LegFormat: X01LegFormat = X01LegFormat.FirstTo,
    val cricketPointsEnabled: Boolean = true,
    val cricketScoringMode: CricketScoringMode = CricketScoringMode.Standard,
    val cricketLegsToWin: Int = 1,
    val cricketSetsEnabled: Boolean = false,
    val cricketSetsToWin: Int = 1,
    val cricketLegFormat: X01LegFormat = X01LegFormat.FirstTo,
    val randomOrder: Boolean = false,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
    val rematchErrorMessage: String? = null,
) {
    val selectedPlayers: List<PlayerSummary>
        get() = selectedPlayerIds.mapNotNull { id -> players.find { it.id == id } }

    val availablePlayers: List<PlayerSummary>
        get() = players.filter { it.id !in selectedPlayerIds }

    val availableHumans: List<PlayerSummary>
        get() = availablePlayers.filter { !it.isBot }.sortedBy { it.name.lowercase() }

    val availableBots: List<PlayerSummary>
        get() = availablePlayers.filter { it.isBot }.sortedBy { it.name.lowercase() }

    val canStart: Boolean
        get() = selectedPlayerIds.isNotEmpty() &&
            !(setupMode == MatchType.Cricket && selectedPlayerIds.size < 2)

    val validationErrors: List<String>
        get() = buildList {
            if (selectedPlayerIds.isEmpty()) add("empty_roster")
            if (setupMode == MatchType.Cricket && selectedPlayerIds.size < 2) add("cricket_min")
        }
}

@HiltViewModel
class PlayViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val settingsRepository: SettingsRepository,
    private val rematchStore: RematchStore,
    private val practiceSetupStore: com.jacobrozell.dartbuddy.data.prefs.PracticeSetupStore,
    private val logger: AppLogger,
    private val firebaseBootstrap: com.jacobrozell.dartbuddy.support.FirebaseBootstrap,
    private val l10n: L10n,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()
    private var pendingStartSource: String? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            _uiState.update { it.applySettingsDefaults(settings).copy(setupMode = settings.defaultMatchType) }
            if (playerRepository.observeActivePlayers().first().isEmpty()) {
                playerRepository.create("Player 1")
            }
        }
        viewModelScope.launch {
            playerRepository.observeActivePlayers().collect { players ->
                _uiState.update { it.copy(players = players) }
            }
        }
        viewModelScope.launch {
            matchRepository.observeActiveMatch().collect { match ->
                _uiState.update { it.copy(activeMatch = match) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observe().collect { settings ->
                _uiState.update { current ->
                    if (current.settings == settings) current else current.copy(settings = settings)
                }
            }
        }
    }

    private fun PlayUiState.applySettingsDefaults(settings: AppSettings) = copy(
        settings = settings,
        x01StartScore = settings.defaultX01StartScore,
        x01LegsToWin = settings.defaultLegsToWin,
        x01SetsEnabled = settings.defaultSetsEnabled,
        x01SetsToWin = settings.defaultSetsToWin,
        x01CheckoutMode = settings.defaultX01CheckoutMode,
        x01CheckInMode = settings.defaultX01CheckInMode,
        x01LegFormat = settings.defaultLegFormat,
        cricketLegsToWin = settings.defaultLegsToWin,
        cricketSetsEnabled = settings.defaultSetsEnabled,
        cricketSetsToWin = settings.defaultSetsToWin,
        cricketLegFormat = settings.defaultLegFormat,
        cricketPointsEnabled = settings.defaultCricketPointsEnabled,
        cricketScoringMode = settings.defaultCricketScoringMode,
    )

    fun setSetupMode(mode: MatchType) = _uiState.update { it.copy(setupMode = mode) }
    fun toggleRandomOrder() = _uiState.update { it.copy(randomOrder = !it.randomOrder) }

    fun removeFromSelection(id: String) = _uiState.update { state ->
        state.copy(selectedPlayerIds = state.selectedPlayerIds.filterNot { it == id })
    }

    fun moveSelectedPlayer(fromIndex: Int, toIndex: Int) = _uiState.update { state ->
        val list = state.selectedPlayerIds.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return@update state
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        state.copy(selectedPlayerIds = list)
    }

    fun togglePlayer(id: String) = _uiState.update { state ->
        state.copy(
            selectedPlayerIds = if (id in state.selectedPlayerIds) {
                state.selectedPlayerIds - id
            } else {
                state.selectedPlayerIds + id
            },
        )
    }

    fun movePlayerUp(id: String) = _uiState.update { state ->
        val list = state.selectedPlayerIds.toMutableList()
        val index = list.indexOf(id)
        if (index > 0) {
            list[index - 1] = list[index].also { list[index] = list[index - 1] }
            state.copy(selectedPlayerIds = list)
        } else state
    }

    fun movePlayerDown(id: String) = _uiState.update { state ->
        val list = state.selectedPlayerIds.toMutableList()
        val index = list.indexOf(id)
        if (index in 0 until list.lastIndex) {
            list[index + 1] = list[index].also { list[index] = list[index + 1] }
            state.copy(selectedPlayerIds = list)
        } else state
    }

    fun updateX01StartScore(score: Int) = _uiState.update { it.copy(x01StartScore = score) }
    fun updateX01LegsToWin(legs: Int) = _uiState.update { it.copy(x01LegsToWin = legs.coerceAtLeast(1)) }
    fun updateX01SetsEnabled(enabled: Boolean) = _uiState.update { it.copy(x01SetsEnabled = enabled) }
    fun updateX01SetsToWin(sets: Int) = _uiState.update { it.copy(x01SetsToWin = sets.coerceAtLeast(1)) }
    fun updateX01CheckoutMode(mode: X01CheckoutMode) = _uiState.update { it.copy(x01CheckoutMode = mode) }
    fun updateX01CheckInMode(mode: X01CheckInMode) = _uiState.update { it.copy(x01CheckInMode = mode) }
    fun updateX01LegFormat(format: X01LegFormat) = _uiState.update { it.copy(x01LegFormat = format) }
    fun updateCricketPointsEnabled(enabled: Boolean) = _uiState.update { it.copy(cricketPointsEnabled = enabled) }
    fun updateCricketScoringMode(mode: CricketScoringMode) = _uiState.update { it.copy(cricketScoringMode = mode) }
    fun updateCricketLegsToWin(legs: Int) = _uiState.update { it.copy(cricketLegsToWin = legs.coerceAtLeast(1)) }
    fun updateCricketSetsEnabled(enabled: Boolean) = _uiState.update { it.copy(cricketSetsEnabled = enabled) }
    fun updateCricketSetsToWin(sets: Int) = _uiState.update { it.copy(cricketSetsToWin = sets.coerceAtLeast(1)) }
    fun updateCricketLegFormat(format: X01LegFormat) = _uiState.update { it.copy(cricketLegFormat = format) }

    fun addPlayer(
        name: String,
        colorToken: String? = null,
        avatarStyleRaw: String? = null,
    ) {
        viewModelScope.launch {
            val created = playerRepository.create(name, colorToken, avatarStyleRaw)
            _uiState.update {
                it.copy(
                    selectedPlayerIds = it.selectedPlayerIds + created.id,
                    rematchErrorMessage = null,
                )
            }
        }
    }

    fun addPresetBot(difficulty: BotDifficulty) {
        viewModelScope.launch {
            val created = playerRepository.createPresetBot(difficulty)
            _uiState.update { it.copy(selectedPlayerIds = it.selectedPlayerIds + created.id) }
        }
    }

    fun addCustomBot(name: String, metrics: CustomBotMetrics) {
        viewModelScope.launch {
            val created = playerRepository.createCustomBot(name, metrics)
            _uiState.update { it.copy(selectedPlayerIds = it.selectedPlayerIds + created.id) }
        }
    }

    fun setupConfigSummary(): String {
        val state = _uiState.value
        return when (state.setupMode) {
            MatchType.X01 -> MatchConfigText.x01InlineConfig(
                MatchConfigX01(
                    startScore = state.x01StartScore,
                    legsToWin = state.x01LegsToWin,
                    setsEnabled = state.x01SetsEnabled,
                    setsToWin = if (state.x01SetsEnabled) state.x01SetsToWin else null,
                    checkoutMode = state.x01CheckoutMode,
                    checkInMode = state.x01CheckInMode,
                    legFormat = state.x01LegFormat,
                ),
                l10n,
            )
            MatchType.Cricket -> MatchConfigText.cricketInlineConfig(
                MatchConfigCricket(
                    pointsEnabled = state.cricketPointsEnabled,
                    scoringMode = state.cricketScoringMode,
                    legsToWin = state.cricketLegsToWin,
                    setsEnabled = state.cricketSetsEnabled,
                    setsToWin = if (state.cricketSetsEnabled) state.cricketSetsToWin else null,
                    legFormat = state.cricketLegFormat,
                ),
                l10n,
            )
        }
    }

    fun startMatch(onStarted: (MatchType, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            try {
                val state = _uiState.value
                var ordered = state.selectedPlayerIds.mapNotNull { id ->
                    state.players.find { it.id == id }
                }
                if (state.randomOrder && ordered.size > 1) {
                    ordered = ordered.shuffled()
                }
                if (ordered.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Select at least one player") }
                    return@launch
                }
                if (state.setupMode == MatchType.Cricket && ordered.size < 2) {
                    _uiState.update { it.copy(errorMessage = "Cricket needs at least 2 players") }
                    return@launch
                }
                val ids = ordered.map { it.id }
                val names = ordered.map { it.name }
                val startSource = pendingStartSource ?: "setup"
                val (entity, configPayload) = when (state.setupMode) {
                    MatchType.X01 -> {
                        val config = MatchConfigX01(
                            startScore = state.x01StartScore,
                            legsToWin = state.x01LegsToWin,
                            setsEnabled = state.x01SetsEnabled,
                            setsToWin = if (state.x01SetsEnabled) state.x01SetsToWin else null,
                            checkoutMode = state.x01CheckoutMode,
                            checkInMode = state.x01CheckInMode,
                            legFormat = state.x01LegFormat,
                        )
                        val payload = MatchConfigPayload.X01(config)
                        matchRepository.startMatch(
                            MatchType.X01,
                            payload,
                            ids,
                            names,
                            json.encodeToString(X01Engine.makeInitialState(config, ids)),
                        ) to payload
                    }
                    MatchType.Cricket -> {
                        val config = MatchConfigCricket(
                            pointsEnabled = state.cricketPointsEnabled,
                            scoringMode = if (state.cricketPointsEnabled) state.cricketScoringMode else CricketScoringMode.Standard,
                            legsToWin = state.cricketLegsToWin,
                            setsEnabled = state.cricketSetsEnabled,
                            setsToWin = if (state.cricketSetsEnabled) state.cricketSetsToWin else null,
                            legFormat = state.cricketLegFormat,
                        )
                        val payload = MatchConfigPayload.Cricket(config)
                        matchRepository.startMatch(
                            MatchType.Cricket,
                            payload,
                            ids,
                            names,
                            json.encodeToString(CricketEngine.makeInitialState(config, ids)),
                        ) to payload
                    }
                }
                val analyticsMetadata = MatchAnalytics.metadata(
                    matchType = state.setupMode,
                    config = configPayload,
                    participantCount = ids.size,
                    players = ordered,
                    startSource = startSource,
                )
                logger.info(
                    LogCategory.UI,
                    eventName = "match_setup_start",
                    message = "User started match setup.",
                    metadata = analyticsMetadata,
                    correlationId = entity.id,
                )
                settingsRepository.save(
                    state.settings.copy(
                        defaultX01StartScore = state.x01StartScore,
                        defaultX01CheckoutMode = state.x01CheckoutMode,
                        defaultX01CheckInMode = state.x01CheckInMode,
                        defaultLegFormat = if (state.setupMode == MatchType.X01) state.x01LegFormat else state.cricketLegFormat,
                        defaultLegsToWin = if (state.setupMode == MatchType.X01) state.x01LegsToWin else state.cricketLegsToWin,
                        defaultSetsEnabled = if (state.setupMode == MatchType.X01) state.x01SetsEnabled else state.cricketSetsEnabled,
                        defaultSetsToWin = if (state.setupMode == MatchType.X01) state.x01SetsToWin else state.cricketSetsToWin,
                        defaultCricketPointsEnabled = state.cricketPointsEnabled,
                        defaultCricketScoringMode = state.cricketScoringMode,
                    ),
                )
                logger.info(
                    LogCategory.SCORING,
                    eventName = "match_started",
                    message = "Match started.",
                    metadata = analyticsMetadata,
                    correlationId = entity.id,
                )
                logger.info(
                    LogCategory.SCORING,
                    eventName = "game_mode_played",
                    message = "Game mode played.",
                    metadata = analyticsMetadata,
                    correlationId = entity.id,
                )
                AnalyticsUserIdentity.syncLastGameMode(firebaseBootstrap, state.setupMode)
                pendingStartSource = null
                onStarted(state.setupMode, entity.id)
            } catch (e: Exception) {
                logger.error(
                    LogCategory.UI,
                    eventName = "match_start_failed",
                    message = e.message ?: "Match start failed.",
                    metadata = mapOf("matchType" to _uiState.value.setupMode.name.lowercase()),
                )
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isStarting = false) }
            }
        }
    }

    /** Applies rematch / training-practice payloads when returning to Play (ViewModel may already exist). */
    fun consumePendingNavigationSetup() {
        rematchStore.pendingPlayerIds?.let { ids ->
            val matchType = rematchStore.pendingMatchType?.let { type ->
                runCatching { MatchType.valueOf(type) }.getOrNull()
            }
            rematchStore.clear()

            val activeIds = ids.filter { id ->
                _uiState.value.players.any { it.id == id && !it.isArchived }
            }
            val rosterValid = activeIds.isNotEmpty() &&
                !(matchType == MatchType.Cricket && activeIds.size < 2)

            if (!rosterValid) {
                logger.error(
                    LogCategory.UI,
                    eventName = "rematch_roster_invalid",
                    message = "Rematch roster could not be restored.",
                    metadata = mapOf(
                        "requestedCount" to ids.size.toString(),
                        "availableCount" to activeIds.size.toString(),
                        "matchType" to (matchType?.name?.lowercase() ?: "unknown"),
                    ),
                )
                _uiState.update {
                    it.copy(rematchErrorMessage = l10n.get(R.string.play_summary_rematch_failed))
                }
                return
            }

            _uiState.update {
                it.copy(
                    selectedPlayerIds = activeIds,
                    setupMode = matchType ?: it.setupMode,
                    rematchErrorMessage = null,
                )
            }
            pendingStartSource = "rematch"
            return
        }
        if (!ProductSurface.showsTrainingBots) return
        practiceSetupStore.consume()?.let { setup ->
            _uiState.update {
                it.copy(
                    selectedPlayerIds = listOf(setup.humanId, setup.trainingBotId),
                    setupMode = setup.mode,
                )
            }
            pendingStartSource = "setup"
        }
    }

    fun clearRematchError() = _uiState.update { it.copy(rematchErrorMessage = null) }

    fun logPlayHomeReady(hasActiveMatch: Boolean) {
        logger.info(
            LogCategory.UI,
            eventName = if (hasActiveMatch) "play_home_active_match" else "play_home_ready",
            message = if (hasActiveMatch) "Play home shows active match." else "Play home ready.",
        )
    }

    fun logMatchResumed(match: MatchEntity, startSource: String = "resume") {
        viewModelScope.launch {
            val ids = json.decodeFromString<List<String>>(match.participantIdsJson)
            val players = ids.mapNotNull { playerRepository.getById(it) }
            MatchAnalytics.logResumed(
                logger = logger,
                firebaseBootstrap = firebaseBootstrap,
                entity = match,
                json = json,
                players = players,
                startSource = startSource,
            )
        }
    }
}
