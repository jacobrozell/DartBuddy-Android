package com.jacobrozell.dartbuddy.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.domain.service.MatchStatsCalculator
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.components.ResumeBanner
import com.jacobrozell.dartbuddy.ui.designsystem.ErrorBanner
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.TabRootScaffold
import com.jacobrozell.dartbuddy.ui.theme.Brand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class ActivityFilter { All, X01, Cricket }

data class PlayerAggregateStats(
    val playerId: String,
    val name: String,
    val matchCount: Int,
    val x01Average: Double?,
    val cricketMpr: Double?,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
) : ViewModel() {
    val history = matchRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val players = playerRepository.observeActivePlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeMatch = matchRepository.observeActiveMatch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val matchSessionService: MatchSessionService,
    private val json: Json,
) : ViewModel() {
    private val _rows = kotlinx.coroutines.flow.MutableStateFlow<List<PlayerAggregateStats>>(emptyList())
    val rows = _rows.asStateFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(filter: ActivityFilter, period: ActivityPeriod, playerId: String?) {
        viewModelScope.launch {
            val history = matchRepository.observeHistory().first()
            val completed = ActivityFilters.filterMatches(
                matches = history.filter {
                    it.status == MatchStatus.Completed || it.status == MatchStatus.Forfeited
                },
                mode = filter,
                period = period,
                playerId = playerId,
            )

            val aggregates = mutableMapOf<String, MutableAggregate>()
            completed.forEach { summary ->
                summary.participantIds.zip(summary.participantNames).forEach { (id, name) ->
                    val bucket = aggregates.getOrPut(id) { MutableAggregate(id, name) }
                    bucket.matchCount++
                }
                when (summary.matchType) {
                    MatchType.X01 -> {
                        val session = matchSessionService.loadX01Session(summary.id) ?: return@forEach
                        val stats = MatchStatsCalculator.x01Stats(
                            summary.participantIds,
                            summary.participantNames,
                            session.events,
                            session.state.players.associate { it.playerId to it.legsWon },
                            session.state.players.associate { it.playerId to it.setsWon },
                            summary.winnerPlayerId,
                        )
                        stats.forEach { row ->
                            aggregates[row.playerId]?.addX01(row.average3Dart)
                        }
                    }
                    MatchType.Cricket -> {
                        val session = matchSessionService.loadCricketSession(summary.id) ?: return@forEach
                        val stats = MatchStatsCalculator.cricketStats(
                            summary.participantIds,
                            summary.participantNames,
                            session.events,
                            session.state.players.associate { it.playerId to it.score },
                            session.state.players.associate { it.playerId to it.legsWon },
                            session.state.players.associate { it.playerId to it.setsWon },
                            summary.winnerPlayerId,
                        )
                        stats.forEach { row ->
                            aggregates[row.playerId]?.addCricket(row.marksPerRound)
                        }
                    }
                }
            }

            _rows.value = aggregates.values
                .map { it.toRow() }
                .sortedWith(compareByDescending<PlayerAggregateStats> { it.matchCount }.thenBy { it.name })
        }
    }

    private class MutableAggregate(val playerId: String, val name: String) {
        var matchCount: Int = 0
        private val x01Avgs = mutableListOf<Double>()
        private val cricketMprs = mutableListOf<Double>()

        fun addX01(avg: Double) {
            x01Avgs.add(avg)
        }

        fun addCricket(mpr: Double) {
            cricketMprs.add(mpr)
        }

        fun toRow() = PlayerAggregateStats(
            playerId = playerId,
            name = name,
            matchCount = matchCount,
            x01Average = x01Avgs.takeIf { it.isNotEmpty() }?.average(),
            cricketMpr = cricketMprs.takeIf { it.isNotEmpty() }?.average(),
        )
    }
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val matchSessionService: MatchSessionService,
    private val matchRepository: MatchRepository,
    private val json: Json,
) : ViewModel() {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(HistoryDetailUiState())
    val state: kotlinx.coroutines.flow.StateFlow<HistoryDetailUiState> = _state.asStateFlow()

    fun load(matchId: String) {
        viewModelScope.launch {
            _state.value = HistoryDetailUiState(loading = true)
            try {
                val entity = matchSessionService.getMatchEntity(matchId)
                if (entity == null) {
                    _state.value = HistoryDetailUiState(loading = false, loadFailed = true)
                    return@launch
                }
                val names = json.decodeFromString<List<String>>(entity.participantNamesJson)
                val ids = json.decodeFromString<List<String>>(entity.participantIdsJson)
                val type = MatchType.valueOf(entity.matchType)
                val stats = when (type) {
                    MatchType.X01 -> {
                        val session = matchSessionService.loadX01Session(matchId)
                            ?: run {
                                _state.value = HistoryDetailUiState(loading = false, loadFailed = true)
                                return@launch
                            }
                        MatchStatsCalculator.x01Stats(
                            ids,
                            names,
                            session.events,
                            session.state.players.associate { it.playerId to it.legsWon },
                            session.state.players.associate { it.playerId to it.setsWon },
                            entity.winnerPlayerId,
                        )
                    }
                    MatchType.Cricket -> {
                        val session = matchSessionService.loadCricketSession(matchId)
                            ?: run {
                                _state.value = HistoryDetailUiState(loading = false, loadFailed = true)
                                return@launch
                            }
                        MatchStatsCalculator.cricketStats(
                            ids,
                            names,
                            session.events,
                            session.state.players.associate { it.playerId to it.score },
                            session.state.players.associate { it.playerId to it.legsWon },
                            session.state.players.associate { it.playerId to it.setsWon },
                            entity.winnerPlayerId,
                        )
                    }
                }
                val winnerIndex = ids.indexOf(entity.winnerPlayerId)
                _state.value = HistoryDetailUiState(
                    loading = false,
                    detail = HistoryDetailState(
                        id = entity.id,
                        matchType = type,
                        status = MatchStatus.valueOf(entity.status),
                        participantNames = names,
                        modeLabel = MatchStatsCalculator.modeLabel(type),
                        winnerName = names.getOrNull(winnerIndex),
                        playerStats = stats,
                    ),
                )
            } catch (_: Exception) {
                _state.value = HistoryDetailUiState(loading = false, loadFailed = true)
            }
        }
    }

    fun deleteMatch(matchId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            matchRepository.deleteMatch(matchId)
            onDone()
        }
    }
}

data class HistoryDetailUiState(
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    val detail: HistoryDetailState? = null,
)

data class HistoryDetailState(
    val id: String,
    val matchType: MatchType,
    val status: MatchStatus,
    val participantNames: List<String>,
    val modeLabel: String,
    val winnerName: String? = null,
    val playerStats: List<com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats> = emptyList(),
)

@Composable
fun ActivityScreen(
    onOpenMatch: (String) -> Unit,
    onResumeMatch: (String, String) -> Unit = { _, _ -> },
    onStartMatch: () -> Unit = {},
    viewModel: ActivityViewModel = hiltViewModel(),
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val stats by statisticsViewModel.rows.collectAsStateWithLifecycle()
    var segment by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(ActivityFilter.All) }
    var period by remember { mutableStateOf(ActivityPeriod.All) }
    var playerFilterId by remember { mutableStateOf<String?>(null) }
    var historyLimit by remember { mutableIntStateOf(30) }

    androidx.compose.runtime.LaunchedEffect(segment, filter, period, playerFilterId) {
        if (segment == 1) statisticsViewModel.load(filter, period, playerFilterId)
    }

    val filteredHistory = ActivityFilters.filterMatches(history, filter, period, playerFilterId)

    val filterLabels = mapOf(
        ActivityFilter.All to stringResource(R.string.activity_filter_all),
        ActivityFilter.X01 to stringResource(R.string.activity_filter_x01),
        ActivityFilter.Cricket to stringResource(R.string.activity_filter_cricket),
    )
    val periodLabels = mapOf(
        ActivityPeriod.Today to stringResource(R.string.stats_period_today),
        ActivityPeriod.Days7 to stringResource(R.string.stats_period_7d),
        ActivityPeriod.Days30 to stringResource(R.string.stats_period_30d),
        ActivityPeriod.All to stringResource(R.string.stats_period_all),
    )
    val playerOptions = listOf<Pair<String?, String>>(null to stringResource(R.string.activity_filter_all_players)) +
        players.filter { !it.isBot }.map { it.id to it.name }

    TabRootScaffold {
        com.jacobrozell.dartbuddy.ui.designsystem.BrandRootScreenTitle(title = stringResource(R.string.activity_title))

        activeMatch?.let { match ->
            ResumeBanner(
                matchType = match.matchType,
                onClick = { onResumeMatch(match.matchType, match.id) },
            )
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = segment == 0,
                onClick = { segment = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.activity_segment_history)) }
            SegmentedButton(
                selected = segment == 1,
                onClick = { segment = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.activity_segment_statistics)) }
        }

        HorizontalOptionChips(
            label = "",
            options = ActivityFilter.entries,
            selected = filter,
            onSelect = { filter = it },
            labelFor = { filterLabels[it] ?: it.name },
        )
        HorizontalOptionChips(
            label = "",
            options = ActivityPeriod.entries,
            selected = period,
            onSelect = { period = it },
            labelFor = { periodLabels[it] ?: it.name },
        )
        HorizontalOptionChips(
            label = stringResource(R.string.activity_filter_player),
            options = playerOptions,
            selected = playerOptions.find { it.first == playerFilterId } ?: playerOptions.first(),
            onSelect = { playerFilterId = it.first },
            labelFor = { it.second },
        )

        when (segment) {
            0 -> {
                if (filteredHistory.isEmpty()) {
                    Text(stringResource(R.string.activity_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryActionButton(
                        title = stringResource(R.string.activity_empty_start_match),
                        onClick = onStartMatch,
                    )
                } else {
                    val visible = filteredHistory.take(historyLimit)
                    visible.forEach { match ->
                        HistoryRow(match, onClick = { onOpenMatch(match.id) })
                    }
                    if (filteredHistory.size > historyLimit) {
                        TextButton(onClick = { historyLimit += 30 }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.activity_load_more), color = Brand.Green)
                        }
                    }
                }
            }
            1 -> {
                if (stats.isEmpty()) {
                    Text(stringResource(R.string.activity_stats_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    StatsHeaderRow()
                    stats.forEach { row -> StatsRow(row) }
                }
            }
        }
    }
}

@Composable
private fun StatsHeaderRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = DS.s2), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.players_title), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
        Text(stringResource(R.string.activity_stats_matches), fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
        Text(stringResource(R.string.activity_stats_x01_avg), fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        Text(stringResource(R.string.activity_stats_cricket_mpr), fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun StatsRow(row: PlayerAggregateStats) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1.2f), horizontalArrangement = Arrangement.spacedBy(DS.s2), verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(name = row.name, colorToken = null, playerId = row.playerId, size = 28.dp)
            Text(row.name, fontWeight = FontWeight.SemiBold)
        }
        Text(row.matchCount.toString(), modifier = Modifier.weight(0.6f))
        Text(row.x01Average?.let { "%.1f".format(it) } ?: "—", modifier = Modifier.weight(0.8f))
        Text(row.cricketMpr?.let { "%.2f".format(it) } ?: "—", modifier = Modifier.weight(0.8f))
    }
}

@Composable
fun HistoryDetailScreen(
    matchId: String,
    onBack: () -> Unit,
    onResume: (String, String) -> Unit,
    onViewSummary: (String) -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(matchId) { viewModel.load(matchId) }

    TabRootScaffold {
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
        when {
            uiState.loading -> {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().padding(DS.s6),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            uiState.loadFailed -> {
                ErrorBanner(stringResource(R.string.activity_detail_error))
                TextButton(onClick = { viewModel.load(matchId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_detail_retry), color = Brand.Green)
                }
            }
            uiState.detail != null -> {
                val detail = uiState.detail!!
            GameModeBadge(detail.matchType.name)
            Text(detail.modeLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(detail.participantNames.joinToString(" vs "))
            Text(statusLabel(detail.status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            detail.winnerName?.let {
                Text(stringResource(R.string.play_summary_winner, it), fontWeight = FontWeight.Bold, color = Brand.Green)
            }

            if (detail.playerStats.isNotEmpty()) {
                Text(stringResource(R.string.activity_detail_stats_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                detail.playerStats.forEach { row ->
                    val dark = isSystemInDarkTheme()
                    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DS.radiusSm))
                            .background(cardBg)
                            .padding(DS.s3),
                    ) {
                        Text(row.name, fontWeight = FontWeight.Bold)
                        when (detail.matchType) {
                            MatchType.X01 -> {
                                Text("3-dart avg: ${"%.1f".format(row.average3Dart)}", style = MaterialTheme.typography.bodySmall)
                                Text("Darts: ${row.darts}", style = MaterialTheme.typography.bodySmall)
                                Text("Legs: ${row.legsWon}", style = MaterialTheme.typography.bodySmall)
                                if (row.highestCheckout > 0) {
                                    Text("Best checkout: ${row.highestCheckout}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            MatchType.Cricket -> {
                                Text("Score: ${row.score}", style = MaterialTheme.typography.bodySmall)
                                Text("MPR: ${"%.2f".format(row.marksPerRound)}", style = MaterialTheme.typography.bodySmall)
                                Text("Legs: ${row.legsWon}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            when (detail.status) {
                MatchStatus.InProgress -> {
                    PrimaryActionButton(title = stringResource(R.string.activity_resume), onClick = { onResume(detail.matchType.name, detail.id) })
                }
                MatchStatus.Completed, MatchStatus.Forfeited -> {
                    PrimaryActionButton(title = stringResource(R.string.activity_view_summary), onClick = { onViewSummary(detail.id) })
                }
                else -> Unit
            }
            TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.activity_detail_delete_match), color = Brand.Red)
            }
            }
        }
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.activity_detail_delete_match)) },
            text = { Text(stringResource(R.string.settings_resetConfirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMatch(matchId, onBack)
                    showDeleteConfirm = false
                }) { Text(stringResource(R.string.common_confirm), color = Brand.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun statusLabel(status: MatchStatus): String = when (status) {
    MatchStatus.Completed -> stringResource(R.string.activity_status_completed)
    MatchStatus.InProgress -> stringResource(R.string.activity_status_inProgress)
    MatchStatus.Forfeited -> stringResource(R.string.activity_status_forfeited)
    else -> status.name
}

@Composable
private fun HistoryRow(match: MatchSummary, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a").withZone(ZoneId.systemDefault())
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusMd))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(DS.s3),
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameModeBadge(match.matchType.name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(match.participantNames.joinToString(" vs "), fontWeight = FontWeight.SemiBold)
            Text(statusLabel(match.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatter.format(match.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
