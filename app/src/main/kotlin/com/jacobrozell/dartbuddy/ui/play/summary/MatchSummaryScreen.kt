package com.jacobrozell.dartbuddy.ui.play.summary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.jacobrozell.dartbuddy.support.motion.MotionPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.MatchStatsCalculator
import com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.ErrorBanner
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.StatChip
import com.jacobrozell.dartbuddy.ui.layout.contentMaxWidth
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.usesLandscapeMatchSummaryLayout
import com.jacobrozell.dartbuddy.ui.layout.usesMatchSummarySideBySidePlayerGrid
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun MatchSummaryScreen(
    matchId: String,
    onDone: () -> Unit,
    onRematch: () -> Unit,
    onViewStatistics: () -> Unit = {},
    onResumeMatch: (MatchType, String) -> Unit = { _, _ -> },
    viewModel: MatchSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(matchId) { viewModel.load(matchId) }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val dark = isSystemInDarkTheme()
    val bg = if (dark) Brand.BackgroundDark else Brand.BackgroundLight
    val layout = rememberLayoutContext(playerCount = state.playerStats.size)
    val usesSplit = usesLandscapeMatchSummaryLayout(layout)
    val usesGrid = usesMatchSummarySideBySidePlayerGrid(layout, state.playerStats.size)
    val readableMax = contentMaxWidth(layout)

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .padding(DS.s4),
    ) {
        if (usesSplit) {
            Column(
                Modifier
                    .fillMaxSize()
                    .widthIn(max = readableMax)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(DS.s3),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DS.s4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryCelebrationHeader(
                        state = state,
                        modifier = Modifier.widthIn(max = 280.dp),
                        trophySize = 72.dp,
                    )
                    SummaryPlayerCards(
                        state = state,
                        dark = dark,
                        usesGrid = usesGrid,
                        modifier = Modifier.weight(1f),
                    )
                }
                SummaryActions(
                    state = state,
                    viewModel = viewModel,
                    horizontal = true,
                    onDone = onDone,
                    onRematch = onRematch,
                    onViewStatistics = onViewStatistics,
                    onResumeMatch = onResumeMatch,
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = readableMax)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(DS.s4),
            ) {
                SummaryCelebrationHeader(state = state, showModeLabel = true)
                SummaryPlayerCards(state = state, dark = dark, usesGrid = usesGrid)
                SummaryActions(
                    state = state,
                    viewModel = viewModel,
                    onDone = onDone,
                    onRematch = onRematch,
                    onViewStatistics = onViewStatistics,
                    onResumeMatch = onResumeMatch,
                )
            }
        }
    }
}

@Composable
private fun SummaryCelebrationHeader(
    state: MatchSummaryUiState,
    modifier: Modifier = Modifier,
    trophySize: androidx.compose.ui.unit.Dp = 72.dp,
    showModeLabel: Boolean = false,
) {
    Column(
        modifier = modifier.a11yId(AccessibilityIds.MATCH_SUMMARY_HEADER),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        when {
            state.isForfeited -> Icon(
                Icons.Default.Flag,
                contentDescription = null,
                tint = Brand.Amber,
                modifier = Modifier.size(48.dp),
            )
            state.winnerName != null -> {
                val context = LocalContext.current
                val reduceMotion = !MotionPolicy.shouldAnimate(context)
                var animateTrophy by remember { mutableStateOf(reduceMotion) }
                LaunchedEffect(Unit) {
                    if (!reduceMotion) animateTrophy = true
                }
                val scale by animateFloatAsState(
                    targetValue = if (animateTrophy) 1f else 0.6f,
                    animationSpec = MotionPolicy.trophyAnimationSpec(reduceMotion),
                    label = "trophy",
                )
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = stringResource(R.string.play_summary_trophy_accessibility),
                    tint = Brand.Amber,
                    modifier = Modifier.size(trophySize).scale(scale),
                )
            }
        }
        if (showModeLabel && state.matchType != null) {
            GameModeBadge(state.matchType.name)
            Text(
                MatchStatsCalculator.modeLabel(state.matchType),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        SummaryHeadline(state)
        Text(
            if (state.isForfeited) stringResource(R.string.play_summary_forfeit_statsSaved)
            else stringResource(R.string.play_summary_gameRecorded),
            color = if (state.isForfeited) Brand.Amber else Brand.Green,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.a11yId(AccessibilityIds.MATCH_SUMMARY_GAME_RECORDED),
        )
    }
}

@Composable
private fun SummaryHeadline(state: MatchSummaryUiState) {
    when {
        state.isForfeited -> Text(
            stringResource(R.string.play_summary_forfeited, state.forfeiterName ?: "Someone"),
            color = Brand.Amber,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        state.winnerName != null -> Text(
            stringResource(R.string.play_summary_winner, state.winnerName!!),
            fontWeight = FontWeight.Bold,
            color = Brand.Green,
            textAlign = TextAlign.Center,
        )
        else -> Text(
            stringResource(R.string.play_summary_match_complete),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SummaryPlayerCards(
    state: MatchSummaryUiState,
    dark: Boolean,
    usesGrid: Boolean,
    modifier: Modifier = Modifier,
) {
    if (state.playerStats.isEmpty()) return
    if (usesGrid && state.playerStats.size >= 2) {
        if (state.playerStats.size > 2) {
            FlowRow(
                modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DS.s3),
                verticalArrangement = Arrangement.spacedBy(DS.s3),
                maxItemsInEachRow = 2,
            ) {
                state.playerStats.forEach { row ->
                    SummaryPlayerCard(
                        row = row,
                        matchType = state.matchType,
                        isWinner = row.name == state.winnerName,
                        setsEnabled = state.setsEnabled,
                        dark = dark,
                        modifier = Modifier.fillMaxWidth(0.48f),
                    )
                }
            }
        } else {
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DS.s3)) {
                state.playerStats.forEach { row ->
                    SummaryPlayerCard(
                        row = row,
                        matchType = state.matchType,
                        isWinner = row.name == state.winnerName,
                        setsEnabled = state.setsEnabled,
                        dark = dark,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(DS.s3)) {
            state.playerStats.forEach { row ->
                SummaryPlayerCard(
                    row = row,
                    matchType = state.matchType,
                    isWinner = row.name == state.winnerName,
                    setsEnabled = state.setsEnabled,
                    dark = dark,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryPlayerCard(
    row: PlayerMatchStats,
    matchType: MatchType?,
    isWinner: Boolean,
    setsEnabled: Boolean,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(DS.radiusMd))
            .background(cardBg),
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(if (isWinner) Brand.Amber else androidx.compose.ui.graphics.Color.Transparent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(DS.s3),
            verticalArrangement = Arrangement.spacedBy(DS.s2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DS.s2)) {
                if (isWinner) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(18.dp))
                }
                Text(
                    row.name,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) Brand.Amber else MaterialTheme.colorScheme.onSurface,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DS.s3),
                verticalArrangement = Arrangement.spacedBy(DS.s2),
            ) {
                summaryStatChips(row, matchType, setsEnabled).forEach { (value, label) ->
                    StatChip(value = value, label = label, compact = true)
                }
            }
        }
    }
}

@Composable
private fun summaryStatChips(
    row: PlayerMatchStats,
    matchType: MatchType?,
    setsEnabled: Boolean,
): List<Pair<String, String>> = when (matchType) {
    MatchType.X01 -> buildList {
        add("%.1f".format(row.average3Dart) to stringResource(R.string.stats_three_dart_average))
        if (setsEnabled) add(row.setsWon.toString() to stringResource(R.string.play_summary_stat_sets))
        add(row.legsWon.toString() to stringResource(R.string.play_summary_stat_legs))
        add(
            (if (row.highestCheckout > 0) row.highestCheckout.toString() else "—") to
                stringResource(R.string.play_summary_stat_best_out),
        )
    }
    MatchType.Cricket -> buildList {
        add(row.score.toString() to stringResource(R.string.play_summary_stat_score))
        add(row.darts.toString() to stringResource(R.string.play_summary_stat_darts))
        add("%.2f".format(row.marksPerRound) to stringResource(R.string.play_summary_stat_mpr))
        if (setsEnabled) add(row.setsWon.toString() to stringResource(R.string.play_summary_stat_sets))
        add(row.legsWon.toString() to stringResource(R.string.play_summary_stat_legs))
    }
    null -> emptyList()
}

@Composable
private fun SummaryActions(
    state: MatchSummaryUiState,
    viewModel: MatchSummaryViewModel,
    horizontal: Boolean = false,
    onDone: () -> Unit,
    onRematch: () -> Unit,
    onViewStatistics: () -> Unit,
    onResumeMatch: (MatchType, String) -> Unit,
) {
    val actionContent: @Composable () -> Unit = {
        state.undoError?.let { ErrorBanner(it) }
        if (viewModel.canUndoLastThrow()) {
            Surface(
                onClick = { viewModel.undoLastThrow(onResumeMatch) },
                enabled = !state.isUndoing,
                modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.MATCH_SUMMARY_UNDO_LAST_THROW),
                shape = RoundedCornerShape(DS.radiusSm),
                color = if (isSystemInDarkTheme()) Brand.CardDark else Brand.CardLight,
            ) {
                Text(
                    stringResource(R.string.play_summary_undoLastThrow),
                    modifier = Modifier.padding(vertical = DS.s3),
                    color = Brand.Green,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                stringResource(R.string.play_summary_undoLastThrow_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryActionButton(
            title = stringResource(R.string.play_summary_rematch),
            onClick = { viewModel.prepareRematch(); onRematch() },
            testTag = AccessibilityIds.MATCH_SUMMARY_REMATCH,
        )
        Surface(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.MATCH_SUMMARY_DONE),
            shape = RoundedCornerShape(DS.radiusSm),
            color = if (isSystemInDarkTheme()) Brand.CardElevatedDark else Brand.CardElevatedLight,
        ) {
            Text(
                stringResource(R.string.play_summary_done),
                modifier = Modifier.padding(vertical = DS.s3),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        Surface(
            onClick = onViewStatistics,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DS.radiusSm),
            color = if (isSystemInDarkTheme()) Brand.CardDark else Brand.CardLight,
        ) {
            Text(
                stringResource(R.string.play_summary_view_stats),
                modifier = Modifier.padding(vertical = DS.s3),
                color = Brand.Green,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (horizontal) {
        Column(verticalArrangement = Arrangement.spacedBy(DS.s3), content = { actionContent() })
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(DS.s3), content = { actionContent() })
    }
}
