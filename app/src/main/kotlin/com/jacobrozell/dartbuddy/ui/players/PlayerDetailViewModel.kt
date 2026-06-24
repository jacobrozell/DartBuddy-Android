package com.jacobrozell.dartbuddy.ui.players

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.bots.CustomBotConfiguration
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotEligibility
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotEligibilityService
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotException
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotSkillResolver
import com.jacobrozell.dartbuddy.domain.export.PlayerExportService
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.surface.ProductSurface
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.domain.service.MatchStatsCalculator
import com.jacobrozell.dartbuddy.domain.service.PlayerModeStats
import com.jacobrozell.dartbuddy.domain.service.PlayerStatsAggregator
import com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats
import com.jacobrozell.dartbuddy.domain.service.RecentPlayerMatch
import com.jacobrozell.dartbuddy.domain.service.StatsTrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class PlayerDetailUiState(
    val player: PlayerSummary? = null,
    val isLoadingStats: Boolean = true,
    val x01: PlayerModeStats? = null,
    val cricket: PlayerModeStats? = null,
    val x01TrendPoints: List<StatsTrendPoint> = emptyList(),
    val recentMatches: List<RecentPlayerMatch> = emptyList(),
    val lastPlayedLabel: String? = null,
    val trainingBot: PlayerSummary? = null,
    val x01Eligibility: TrainingBotEligibility = TrainingBotEligibility(false, 0, mode = MatchType.X01),
    val cricketEligibility: TrainingBotEligibility = TrainingBotEligibility(false, 0, mode = MatchType.Cricket),
    val isCreatingTrainingBot: Boolean = false,
    val trainingBotErrorRes: Int? = null,
    val isExporting: Boolean = false,
    val exportErrorRes: Int? = null,
)

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val matchSessionService: MatchSessionService,
    private val json: Json,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(playerId: String) {
        viewModelScope.launch {
            val player = playerRepository.getById(playerId)
            _uiState.value = PlayerDetailUiState(player = player, isLoadingStats = player != null)
            if (player == null) return@launch
            loadStats(playerId, player.name)
            if (!player.isBot && ProductSurface.showsTrainingBots) {
                val bot = playerRepository.fetchTrainingBot(playerId)
                _uiState.update {
                    it.copy(
                        trainingBot = bot,
                        x01Eligibility = TrainingBotEligibilityService.eligibility(it.x01, MatchType.X01),
                        cricketEligibility = TrainingBotEligibilityService.eligibility(it.cricket, MatchType.Cricket),
                    )
                }
            }
        }
    }

    private suspend fun loadStats(playerId: String, playerName: String) {
        val history = matchRepository.observeHistory().first()
            .filter { it.participantIds.contains(playerId) }

        val completed = history.filter {
            it.status == com.jacobrozell.dartbuddy.domain.model.MatchStatus.Completed ||
                it.status == com.jacobrozell.dartbuddy.domain.model.MatchStatus.Forfeited
        }
        val x01 = PlayerStatsAggregator.aggregate(completed, playerId, playerName, MatchType.X01, ::matchPlayerStats)
        val cricket = PlayerStatsAggregator.aggregate(completed, playerId, playerName, MatchType.Cricket, ::matchPlayerStats)
        val recent = PlayerStatsAggregator.recentMatches(completed, playerId)
        val trend = PlayerStatsAggregator.x01TrendPoints(completed, playerId) { summary ->
            matchSessionService.loadX01Session(summary.id)?.events
        }
        val lastPlayed = recent.firstOrNull()?.playedAt?.let(::formatLastPlayed)

        _uiState.update {
            it.copy(
                isLoadingStats = false,
                x01 = x01,
                cricket = cricket,
                x01TrendPoints = trend,
                recentMatches = recent,
                lastPlayedLabel = lastPlayed,
                x01Eligibility = TrainingBotEligibilityService.eligibility(x01, MatchType.X01),
                cricketEligibility = TrainingBotEligibilityService.eligibility(cricket, MatchType.Cricket),
            )
        }
    }

    private suspend fun matchPlayerStats(summary: MatchSummary): List<PlayerMatchStats>? =
        when (summary.matchType) {
            MatchType.X01 -> {
                val session = matchSessionService.loadX01Session(summary.id) ?: return null
                MatchStatsCalculator.x01Stats(
                    summary.participantIds,
                    summary.participantNames,
                    session.events,
                    session.state.players.associate { it.playerId to it.legsWon },
                    session.state.players.associate { it.playerId to it.setsWon },
                    summary.winnerPlayerId,
                )
            }
            MatchType.Cricket -> {
                val session = matchSessionService.loadCricketSession(summary.id) ?: return null
                MatchStatsCalculator.cricketStats(
                    summary.participantIds,
                    summary.participantNames,
                    session.events,
                    session.state.players.associate { it.playerId to it.score },
                    session.state.players.associate { it.playerId to it.legsWon },
                    session.state.players.associate { it.playerId to it.setsWon },
                    summary.winnerPlayerId,
                )
            }
        }

    fun calibratedSummary(mode: MatchType): String? {
        val state = _uiState.value
        val modeStats = if (mode == MatchType.X01) state.x01 else state.cricket
        val profile = TrainingBotSkillResolver.resolve(modeStats, mode)
        return if (mode == MatchType.X01) {
            "%.0f".format(TrainingBotSkillResolver.calibratedX01Summary(profile))
        } else {
            "%.2f".format(TrainingBotSkillResolver.calibratedCricketSummary(profile))
        }
    }

    fun createTrainingBot(humanPlayerId: String) {
        if (!ProductSurface.showsTrainingBots) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTrainingBot = true, trainingBotErrorRes = null) }
            try {
                val bot = playerRepository.createTrainingBot(humanPlayerId)
                _uiState.update { it.copy(trainingBot = bot, isCreatingTrainingBot = false) }
            } catch (e: TrainingBotException) {
                val res = when (e) {
                    TrainingBotException.Duplicate -> R.string.training_bot_error_duplicate
                    TrainingBotException.InvalidPlayer -> R.string.training_bot_error_invalid_player
                    else -> R.string.training_bot_error_ineligible
                }
                _uiState.update { it.copy(isCreatingTrainingBot = false, trainingBotErrorRes = res) }
            }
        }
    }

    fun exportPlayer(context: Context, playerId: String) {
        if (!ProductSurface.showsPlayerExport) return
        viewModelScope.launch {
            val player = playerRepository.getById(playerId) ?: return@launch
            if (player.isBot) return@launch
            _uiState.update { it.copy(isExporting = true, exportErrorRes = null) }
            try {
                val allPlayers = playerRepository.observeAllPlayers().first()
                val matches = matchRepository.matchesForPlayer(playerId)
                val bundle = PlayerExportService.buildBundle(player, allPlayers, matches)
                val file = PlayerExportService.writeExportFile(context, bundle, player.name, json)
                PlayerExportService.shareExport(context, file, player.name)
                _uiState.update { it.copy(isExporting = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isExporting = false, exportErrorRes = R.string.players_detail_export_error) }
            }
        }
    }

    fun save(id: String, name: String, colorToken: String?, avatarStyleRaw: String?, notes: String?, isPrimary: Boolean) {
        viewModelScope.launch {
            playerRepository.update(id, name, colorToken, avatarStyleRaw, notes, isPrimary)
            val player = playerRepository.getById(id)
            _uiState.update { it.copy(player = player) }
        }
    }

    fun updateCustomBotConfiguration(id: String, configuration: CustomBotConfiguration) {
        viewModelScope.launch {
            playerRepository.updateCustomBotConfiguration(id, configuration)
            _uiState.update { it.copy(player = playerRepository.getById(id)) }
        }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            playerRepository.delete(id)
            onDone()
        }
    }

    fun archive(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            playerRepository.archive(id)
            onDone()
        }
    }

    fun unarchive(id: String) {
        viewModelScope.launch {
            playerRepository.unarchive(id)
            _uiState.update { it.copy(player = playerRepository.getById(id)) }
        }
    }

    private fun formatLastPlayed(instant: Instant): String {
        val now = Instant.now()
        val days = ChronoUnit.DAYS.between(
            instant.atZone(ZoneId.systemDefault()).toLocalDate(),
            now.atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        return when {
            days == 0L -> "today"
            days == 1L -> "yesterday"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    }
}
