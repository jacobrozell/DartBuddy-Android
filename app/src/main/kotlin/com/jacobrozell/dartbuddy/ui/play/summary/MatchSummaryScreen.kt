package com.jacobrozell.dartbuddy.ui.play.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.MatchStatsCalculator
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.BrandRootScreenTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.designsystem.ErrorBanner
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
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
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    val dark = isSystemInDarkTheme()
    val bg = if (dark) Brand.BackgroundDark else Brand.BackgroundLight

    Column(
        Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(DS.s4),
        verticalArrangement = Arrangement.spacedBy(DS.s4),
    ) {
        BrandRootScreenTitle(title = stringResource(R.string.play_summary_title))
        if (state.winnerName != null && !state.isForfeited) {
            var animateTrophy by remember { mutableStateOf(false) }
            val trophyA11y = stringResource(R.string.play_summary_trophy_accessibility)
            LaunchedEffect(Unit) { animateTrophy = true }
            val scale by animateFloatAsState(
                targetValue = if (animateTrophy) 1f else 0.6f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "trophy",
            )
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = trophyA11y,
                tint = Brand.Amber,
                modifier = Modifier.size(72.dp).scale(scale),
            )
        }
        GameModeBadge(state.matchType?.name ?: "X01")
        Text(MatchStatsCalculator.modeLabel(state.matchType ?: MatchType.X01), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        when {
            state.isForfeited -> Text(stringResource(R.string.play_summary_forfeited, state.forfeiterName ?: "Someone"), color = Brand.Red)
            state.winnerName != null -> Text(stringResource(R.string.play_summary_winner, state.winnerName!!), fontWeight = FontWeight.Bold, color = Brand.Green)
            else -> Text("Match complete")
        }

        Text(
            if (state.isForfeited) stringResource(R.string.play_summary_forfeit_statsSaved) else stringResource(R.string.play_summary_gameRecorded),
            color = if (state.isForfeited) Brand.Amber else Brand.Green,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )

        state.playerStats.forEach { row ->
            val isWinner = row.name == state.winnerName
            val cardBg = if (isWinner) Brand.Green.copy(alpha = 0.12f) else if (dark) Brand.CardDark else Brand.CardLight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DS.radiusMd))
                    .background(cardBg)
                    .padding(DS.s3),
                horizontalArrangement = Arrangement.spacedBy(DS.s3),
            ) {
                PlayerAvatar(name = row.name, colorToken = null, playerId = row.playerId, size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.name, fontWeight = FontWeight.Bold)
                    when (state.matchType) {
                        MatchType.X01 -> {
                            Text("3-dart avg: ${"%.1f".format(row.average3Dart)}", style = MaterialTheme.typography.bodySmall)
                            Text("Darts: ${row.darts}", style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.play_summary_sets_legs_format, row.setsWon, row.legsWon), style = MaterialTheme.typography.bodySmall)
                            if (row.highestCheckout > 0) Text("Best checkout: ${row.highestCheckout}", style = MaterialTheme.typography.bodySmall)
                        }
                        MatchType.Cricket -> {
                            Text("Score: ${row.score}", style = MaterialTheme.typography.bodySmall)
                            Text("MPR: ${"%.2f".format(row.marksPerRound)}", style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.play_summary_sets_legs_format, row.setsWon, row.legsWon), style = MaterialTheme.typography.bodySmall)
                        }
                        null -> Unit
                    }
                }
            }
        }

        state.undoError?.let { ErrorBanner(it) }

        if (viewModel.canUndoLastThrow()) {
            OutlinedButton(
                onClick = { viewModel.undoLastThrow(onResumeMatch) },
                enabled = !state.isUndoing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.play_summary_undoLastThrow), color = Brand.Green)
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
        )
        OutlinedButton(onClick = onViewStatistics, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.play_summary_view_stats), color = Brand.Green)
        }
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.play_summary_done))
        }
    }
}
