package com.jacobrozell.dartbuddy.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats
import com.jacobrozell.dartbuddy.support.L10n
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.components.ResumeBanner
import com.jacobrozell.dartbuddy.ui.designsystem.ErrorBanner
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.support.HistoryListRow
import com.jacobrozell.dartbuddy.ui.designsystem.BrandSegmented
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
    val wins: Int,
    val stats: PlayerMatchStats,
) {
    val x01Average: Double? = stats.average3Dart.takeIf { stats.darts > 0 }
    val cricketMpr: Double? = stats.marksPerRound.takeIf { it > 0 }
    val winPercent: Double get() = if (matchCount > 0) wins.toDouble() / matchCount * 100 else 0.0
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
) : ViewModel() {
    val history = matchRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyRows = matchRepository.observeHistoryRows()
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
    private val _includesPartial = kotlinx.coroutines.flow.MutableStateFlow(false)
    val includesPartial = _includesPartial.asStateFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun load(filter: ActivityFilter, period: ActivityPeriod, playerId: String?) {
        viewModelScope.launch {
            val history = matchRepository.observeHistory().first()
            val filtered = ActivityFilters.filterMatches(history, filter, period, playerId)
            _includesPartial.value = filtered.any { it.status == MatchStatus.InProgress }

            val aggregates = mutableMapOf<String, MutableAggregate>()
            filtered.forEach { summary ->
                val isPartial = summary.status == MatchStatus.InProgress
                if (summary.status != MatchStatus.Completed &&
                    summary.status != MatchStatus.Forfeited &&
                    !isPartial
                ) {
                    return@forEach
                }
                summary.participantIds.zip(summary.participantNames).forEach { (id, name) ->
                    val bucket = aggregates.getOrPut(id) { MutableAggregate(id, name) }
                    if (!isPartial) {
                        bucket.matchCount++
                        if (summary.winnerPlayerId == id) bucket.wins++
                    }
                }
                val playerStats = when (summary.matchType) {
                    MatchType.X01 -> {
                        val session = matchSessionService.loadX01Session(summary.id) ?: return@forEach
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
                        val session = matchSessionService.loadCricketSession(summary.id) ?: return@forEach
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
                playerStats.forEach { row -> aggregates[row.playerId]?.merge(row) }
            }

            _rows.value = aggregates.values
                .map { it.toRow() }
                .sortedWith(compareByDescending<PlayerAggregateStats> { it.matchCount }.thenBy { it.name })
        }
    }

    private class MutableAggregate(val playerId: String, val name: String) {
        var matchCount: Int = 0
        var wins: Int = 0
        private var stats = PlayerMatchStats(
            playerId = playerId,
            name = name,
            darts = 0,
            points = 0,
            highestScore = 0,
            highestCheckout = 0,
            checkouts = 0,
            legsWon = 0,
            setsWon = 0,
            score = 0,
            marksPerRound = 0.0,
            doubles = 0,
            triples = 0,
        )
        private var cricketMarks = 0
        private var cricketRounds = 0

        fun merge(row: PlayerMatchStats) {
            stats = stats.copy(
                darts = stats.darts + row.darts,
                points = stats.points + row.points,
                highestScore = maxOf(stats.highestScore, row.highestScore),
                highestCheckout = maxOf(stats.highestCheckout, row.highestCheckout),
                checkouts = stats.checkouts + row.checkouts,
                legsWon = stats.legsWon + row.legsWon,
                setsWon = stats.setsWon + row.setsWon,
                score = stats.score + row.score,
                doubles = stats.doubles + row.doubles,
                triples = stats.triples + row.triples,
                hitsBySector = mergeSectorHits(stats.hitsBySector, row.hitsBySector),
            )
            if (row.marksPerRound > 0) {
                cricketMarks += (row.marksPerRound * row.darts / 3).toInt().coerceAtLeast(1)
                cricketRounds += 1
            }
        }

        fun toRow(): PlayerAggregateStats {
            val mpr = if (cricketRounds > 0) cricketMarks.toDouble() / cricketRounds else stats.marksPerRound
            return PlayerAggregateStats(
                playerId = playerId,
                name = name,
                matchCount = matchCount,
                wins = wins,
                stats = stats.copy(marksPerRound = mpr),
            )
        }

        private fun mergeSectorHits(
            left: Map<String, Int>,
            right: Map<String, Int>,
        ): Map<String, Int> {
            val merged = left.toMutableMap()
            right.forEach { (sector, count) ->
                merged[sector] = (merged[sector] ?: 0) + count
            }
            return merged
        }
    }
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val matchSessionService: MatchSessionService,
    private val matchRepository: MatchRepository,
    private val l10n: L10n,
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
                val nameMap = ids.zip(names).toMap()
                val (stats, timeline) = when (type) {
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
                        ) to MatchStatsCalculator.x01Timeline(session.events, nameMap, l10n)
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
                        ) to MatchStatsCalculator.cricketTimeline(session.events, nameMap, l10n)
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
                        timeline = timeline,
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
    val playerStats: List<PlayerMatchStats> = emptyList(),
    val timeline: List<String> = emptyList(),
)

@Composable
fun ActivityScreen(
    onOpenMatch: (String) -> Unit,
    onResumeMatch: (String, String) -> Unit = { _, _ -> },
    onStartMatch: () -> Unit = {},
    viewModel: ActivityViewModel = hiltViewModel(),
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
) {
    val historyRows by viewModel.historyRows.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val stats by statisticsViewModel.rows.collectAsStateWithLifecycle()
    val includesPartial by statisticsViewModel.includesPartial.collectAsStateWithLifecycle()
    var segment by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(ActivityFilter.All) }
    var period by remember { mutableStateOf(ActivityPeriod.All) }
    var playerFilterId by remember { mutableStateOf<String?>(null) }
    var historyLimit by remember { mutableIntStateOf(30) }

    androidx.compose.runtime.LaunchedEffect(segment, filter, period, playerFilterId) {
        if (segment == 1) statisticsViewModel.load(filter, period, playerFilterId)
    }

    val filteredHistory = ActivityFilters.filterHistoryRows(historyRows, filter, period, playerFilterId)
    val hasActiveFilters = filter != ActivityFilter.All || period != ActivityPeriod.All || playerFilterId != null

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

        BrandSegmented(
            options = listOf(0 to stringResource(R.string.activity_segment_history), 1 to stringResource(R.string.activity_segment_statistics)),
            selected = segment,
            onSelect = { segment = it },
        )

        BrandSegmented(
            options = ActivityFilter.entries.map { it to (filterLabels[it] ?: it.name) },
            selected = filter,
            onSelect = { filter = it },
        )
        BrandSegmented(
            options = ActivityPeriod.entries.map { it to (periodLabels[it] ?: it.name) },
            selected = period,
            onSelect = { period = it },
        )
        BrandSegmented(
            options = playerOptions.map { it.first to it.second },
            selected = playerFilterId,
            onSelect = { playerFilterId = it },
        )

        when (segment) {
            0 -> {
                if (filteredHistory.isEmpty()) {
                    Text(
                        if (hasActiveFilters) stringResource(R.string.activity_empty_filtered)
                        else stringResource(R.string.activity_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!hasActiveFilters && historyRows.isEmpty()) {
                        PrimaryActionButton(
                            title = stringResource(R.string.activity_empty_start_match),
                            onClick = onStartMatch,
                        )
                    }
                } else {
                    val visible = filteredHistory.take(historyLimit)
                    visible.forEach { row ->
                        MatchHistoryCard(
                            row = row,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMatch(row.id) },
                        )
                    }
                    if (filteredHistory.size > historyLimit) {
                        TextButton(onClick = { historyLimit += 30 }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.activity_load_more), color = Brand.Green)
                        }
                    }
                }
            }
            1 -> {
                if (includesPartial) {
                    PartialMatchBanner()
                }
                if (stats.isEmpty()) {
                    Text(stringResource(R.string.activity_stats_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    StatisticsTablesContent(
                        stats = stats,
                        filter = filter,
                        playersById = players.associateBy { it.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsTablesContent(
    stats: List<PlayerAggregateStats>,
    filter: ActivityFilter,
    playersById: Map<String, com.jacobrozell.dartbuddy.data.repository.PlayerSummary>,
) {
    val playerStats = stats.map { it.stats }
    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        StatTable(
            title = stringResource(R.string.stats_games),
            columns = listOf(
                StatColumn(stringResource(R.string.stats_games), 0.7f),
                StatColumn(stringResource(R.string.stats_wins), 0.6f),
                StatColumn(stringResource(R.string.stats_column_wins_percent), 0.8f),
            ),
            rows = playerStats,
        ) { row ->
            val aggregate = stats.first { it.stats.playerId == row.playerId }
            listOf(
                aggregate.matchCount.toString(),
                aggregate.wins.toString(),
                "%.0f%%".format(aggregate.winPercent),
            )
        }

        if (filter != ActivityFilter.Cricket) {
            Text(stringResource(R.string.stats_section_average_highest), fontWeight = FontWeight.Bold)
            StatTable(
                columns = listOf(
                    StatColumn(stringResource(R.string.stats_three_dart_average), 0.9f),
                    StatColumn(stringResource(R.string.stats_highest_score), 0.8f),
                ),
                rows = playerStats,
            ) { row ->
                listOf("%.1f".format(row.average3Dart), row.highestScore.toString())
            }
            PlayerAverageBarChart(rows = playerStats, valueFor = { it.average3Dart })
            Text(stringResource(R.string.stats_section_legs_checkout), fontWeight = FontWeight.Bold)
            StatTable(
                columns = listOf(
                    StatColumn(stringResource(R.string.stats_column_legs), 0.6f),
                    StatColumn(stringResource(R.string.stats_checkouts), 0.9f),
                    StatColumn(stringResource(R.string.stats_best_checkout), 0.8f),
                ),
                rows = playerStats,
            ) { row ->
                listOf(
                    row.legsWon.toString(),
                    row.checkouts.toString(),
                    if (row.highestCheckout > 0) row.highestCheckout.toString() else "—",
                )
            }
        }

        if (filter == ActivityFilter.Cricket) {
            Text(stringResource(R.string.stats_section_marks_per_round), fontWeight = FontWeight.Bold)
            StatTable(
                columns = listOf(StatColumn(stringResource(R.string.activity_stats_cricket_mpr), 1f)),
                rows = playerStats,
            ) { row -> listOf("%.2f".format(row.marksPerRound)) }
        }

        Text(stringResource(R.string.stats_points), fontWeight = FontWeight.Bold)
        StatTable(
            columns = listOf(StatColumn(stringResource(R.string.stats_points), 1f)),
            rows = playerStats,
        ) { row -> listOf(row.points.toString()) }

        Text(stringResource(R.string.stats_throws), fontWeight = FontWeight.Bold)
        StatTable(
            columns = listOf(
                StatColumn(stringResource(R.string.stats_throws), 0.7f),
                StatColumn(stringResource(R.string.stats_double_percent), 0.8f),
                StatColumn(stringResource(R.string.stats_triple_percent), 0.8f),
            ),
            rows = playerStats,
        ) { row ->
            listOf(
                row.darts.toString(),
                "%.1f%%".format(row.doublePercent),
                "%.1f%%".format(row.triplePercent),
            )
        }

        if (filter != ActivityFilter.All) {
            val mode = if (filter == ActivityFilter.Cricket) MatchType.Cricket else MatchType.X01
            Text(stringResource(R.string.stats_hits_in_sector), fontWeight = FontWeight.Bold)
            SectorHitsChart(
                hitsBySector = MatchStatsCalculator.mergedSectorHits(playerStats),
                matchType = mode,
            )
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
private fun StatsRow(
    row: PlayerAggregateStats,
    player: com.jacobrozell.dartbuddy.data.repository.PlayerSummary? = null,
) {
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
            PlayerAvatar(
                name = row.name,
                colorToken = player?.colorToken,
                playerId = row.playerId,
                avatarStyleRaw = player?.avatarStyleRaw,
                isBot = player?.isBot == true,
                size = 28.dp,
            )
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
                if (detail.matchType == MatchType.X01) {
                    StatTable(
                        title = stringResource(R.string.stats_section_average_highest),
                        columns = listOf(
                            StatColumn(stringResource(R.string.stats_three_dart_average), 0.9f),
                            StatColumn(stringResource(R.string.stats_highest_score), 0.8f),
                        ),
                        rows = detail.playerStats,
                    ) { row -> listOf("%.1f".format(row.average3Dart), row.highestScore.toString()) }
                    StatTable(
                        title = stringResource(R.string.stats_section_legs_checkout),
                        columns = listOf(
                            StatColumn(stringResource(R.string.stats_column_legs), 0.6f),
                            StatColumn(stringResource(R.string.stats_checkouts), 0.9f),
                            StatColumn(stringResource(R.string.stats_best_checkout), 0.8f),
                        ),
                        rows = detail.playerStats,
                    ) { row ->
                        listOf(
                            row.legsWon.toString(),
                            row.checkouts.toString(),
                            if (row.highestCheckout > 0) row.highestCheckout.toString() else "—",
                        )
                    }
                } else {
                    StatTable(
                        title = stringResource(R.string.stats_section_marks_per_round),
                        columns = listOf(StatColumn(stringResource(R.string.activity_stats_cricket_mpr), 1f)),
                        rows = detail.playerStats,
                    ) { row -> listOf("%.2f".format(row.marksPerRound)) }
                }
                StatTable(
                    title = stringResource(R.string.stats_throws),
                    columns = listOf(
                        StatColumn(stringResource(R.string.stats_throws), 0.7f),
                        StatColumn(stringResource(R.string.stats_double_percent), 0.8f),
                        StatColumn(stringResource(R.string.stats_triple_percent), 0.8f),
                    ),
                    rows = detail.playerStats,
                ) { row ->
                    listOf(
                        row.darts.toString(),
                        "%.1f%%".format(row.doublePercent),
                        "%.1f%%".format(row.triplePercent),
                    )
                }
                detail.playerStats.filter { it.hitsBySector.isNotEmpty() }.forEach { row ->
                    Text(row.name, fontWeight = FontWeight.SemiBold)
                    SectorHitsChart(hitsBySector = row.hitsBySector, matchType = detail.matchType)
                }
                HistoryTimelineSection(detail.timeline)
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
private fun HistoryTimelineSection(timeline: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3),
        verticalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.history_timeline_section),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }
        if (expanded) {
            if (timeline.isEmpty()) {
                Text(stringResource(R.string.history_timeline_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                timeline.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PartialMatchBanner() {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DS.radiusSm),
        color = cardBg,
        border = BorderStroke(1.dp, Brand.Amber.copy(alpha = 0.5f)),
    ) {
        Text(
            stringResource(R.string.stats_partial_match_banner),
            modifier = Modifier.padding(DS.s3),
            color = Brand.Amber,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
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
