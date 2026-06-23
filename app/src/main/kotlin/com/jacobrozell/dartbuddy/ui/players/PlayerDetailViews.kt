package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.bots.BotKind
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotEligibility
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.domain.service.PlayerModeStats
import com.jacobrozell.dartbuddy.domain.service.RecentPlayerMatch
import com.jacobrozell.dartbuddy.domain.service.StatsTrendPoint
import com.jacobrozell.dartbuddy.ui.activity.AverageTrendChart
import com.jacobrozell.dartbuddy.ui.activity.SectorHitsChart
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.theme.Brand
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PlayerIdentityCard(
    player: PlayerSummary,
    colorToken: String,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        PlayerAvatar(
            name = player.name,
            colorToken = colorToken,
            playerId = player.id,
            avatarStyleRaw = player.avatarStyleRaw,
            isBot = player.isBot,
            size = 72.dp,
        )
        Text(
            player.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (player.isBot) {
            Text(
                botBadgeText(player),
                color = Brand.ProBot,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (player.isPrimary) {
            Text(
                stringResource(R.string.players_primary),
                style = MaterialTheme.typography.labelMedium,
                color = Brand.Green,
                fontWeight = FontWeight.SemiBold,
            )
        }
        player.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun botBadgeText(player: PlayerSummary): String {
    if (player.botKindRaw == BotKind.CUSTOM) {
        return BotTurnCoordinator.customMetricsFor(player)?.let {
            stringResource(R.string.custom_bot_badge_format, it.x01Average, it.cricketMPR)
        } ?: stringResource(R.string.players_bot_label)
    }
    return stringResource(R.string.players_bot_label)
}

@Composable
fun PlayerDetailStatsContent(
    isLoading: Boolean,
    x01: PlayerModeStats?,
    cricket: PlayerModeStats?,
    x01TrendPoints: List<StatsTrendPoint>,
    recentMatches: List<RecentPlayerMatch>,
    lastPlayedLabel: String?,
    onSelectMatch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DS.s4)) {
        lastPlayedLabel?.let {
            Text(
                stringResource(R.string.players_detail_last_played, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            isLoading -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = DS.s6),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            x01 == null && cricket == null -> {
                Text(
                    stringResource(R.string.players_detail_no_games),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DS.s6),
                )
            }
            else -> {
                x01?.takeIf { it.games > 0 }?.let {
                    PlayerModeStatsSection(
                        title = stringResource(R.string.play_x01_title),
                        modeStats = it,
                        isX01 = true,
                        trendPoints = x01TrendPoints,
                    )
                }
                cricket?.takeIf { it.games > 0 }?.let {
                    PlayerModeStatsSection(
                        title = stringResource(R.string.play_cricket_title),
                        modeStats = it,
                        isX01 = false,
                    )
                }
                if (recentMatches.isNotEmpty()) {
                    RecentMatchesSection(recentMatches, onSelectMatch)
                }
            }
        }
    }
}

@Composable
private fun PlayerModeStatsSection(
    title: String,
    modeStats: PlayerModeStats,
    isX01: Boolean,
    trendPoints: List<StatsTrendPoint> = emptyList(),
) {
    val stats = modeStats.stats
    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        val tiles = buildList {
            add(stringResource(R.string.stats_games) to modeStats.games.toString())
            add(
                stringResource(R.string.stats_wins) to
                    "${modeStats.wins} (${"%.0f".format(modeStats.winPercent)}%)",
            )
            add(stringResource(R.string.stats_throws) to stats.darts.toString())
            add(stringResource(R.string.stats_points) to stats.points.toString())
            if (isX01) {
                add(stringResource(R.string.stats_column_legs) to stats.legsWon.toString())
                add(stringResource(R.string.stats_three_dart_average) to "%.1f".format(stats.average3Dart))
                add(stringResource(R.string.stats_highest_score) to stats.highestScore.toString())
                add(stringResource(R.string.stats_checkouts) to stats.checkouts.toString())
                add(
                    stringResource(R.string.stats_best_checkout) to
                        if (stats.highestCheckout > 0) stats.highestCheckout.toString() else "—",
                )
            } else {
                add(stringResource(R.string.activity_stats_cricket_mpr) to "%.2f".format(stats.marksPerRound))
                add(stringResource(R.string.stats_column_legs) to stats.legsWon.toString())
                add(stringResource(R.string.stats_final_score) to stats.score.toString())
            }
            add(stringResource(R.string.stats_double_percent) to "%.1f%%".format(stats.doublePercent))
            add(stringResource(R.string.stats_triple_percent) to "%.1f%%".format(stats.triplePercent))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DS.s3),
            verticalArrangement = Arrangement.spacedBy(DS.s3),
            userScrollEnabled = false,
        ) {
            items(tiles) { (label, value) ->
                PlayerStatTile(label = label, value = value)
            }
        }

        if (isX01 && trendPoints.size >= 2) {
            Text(
                stringResource(R.string.stats_trend_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            AverageTrendChart(points = trendPoints)
        }

        if (stats.hitsBySector.isNotEmpty()) {
            Text(
                stringResource(R.string.stats_hits_in_sector),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            SectorHitsChart(
                hitsBySector = stats.hitsBySector,
                matchType = if (isX01) MatchType.X01 else MatchType.Cricket,
            )
        }
    }
}

@Composable
private fun PlayerStatTile(label: String, value: String) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentMatchesSection(
    matches: List<RecentPlayerMatch>,
    onSelectMatch: (String) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        Text(
            stringResource(R.string.players_detail_recent_matches),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DS.radiusSm))
                .background(cardBg),
        ) {
            matches.forEachIndexed { index, match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMatch(match.id) }
                        .padding(horizontal = DS.s3, vertical = DS.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DS.s3),
                ) {
                    GameModeBadge(match.matchType.name)
                    Column(Modifier.weight(1f)) {
                        Text(
                            match.opponentLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            dateFormatter.format(match.playedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (match.didWin) stringResource(R.string.players_detail_win) else stringResource(R.string.players_detail_loss),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (match.didWin) Brand.Green else Brand.Red,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (index < matches.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingPartnerSection(
    trainingBot: PlayerSummary?,
    x01Eligibility: TrainingBotEligibility,
    cricketEligibility: TrainingBotEligibility,
    isCreating: Boolean,
    errorRes: Int?,
    calibratedX01: String?,
    calibratedCricket: String?,
    onCreate: () -> Unit,
    onPractice: (MatchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canCreate = x01Eligibility.isEligible || cricketEligibility.isEligible

    SetupSectionCard(modifier = modifier) {
        Text(
            stringResource(R.string.training_bot_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (trainingBot != null) {
            Text(trainingBot.name, fontWeight = FontWeight.SemiBold)
            calibratedX01?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            calibratedCricket?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DS.s2)) {
                OutlinedButton(
                    onClick = { onPractice(MatchType.X01) },
                    enabled = x01Eligibility.isEligible,
                ) { Text(stringResource(R.string.play_x01_title), color = Brand.Green) }
                OutlinedButton(
                    onClick = { onPractice(MatchType.Cricket) },
                    enabled = cricketEligibility.isEligible,
                ) { Text(stringResource(R.string.play_cricket_title), color = Brand.Green) }
            }
        } else {
            if (!x01Eligibility.isEligible) {
                Text(
                    stringResource(
                        R.string.training_bot_eligibility_progress,
                        x01Eligibility.gamesPlayed,
                        x01Eligibility.requiredGames,
                        stringResource(R.string.play_x01_title),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!cricketEligibility.isEligible) {
                Text(
                    stringResource(
                        R.string.training_bot_eligibility_progress,
                        cricketEligibility.gamesPlayed,
                        cricketEligibility.requiredGames,
                        stringResource(R.string.play_cricket_title),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryActionButton(
                title = stringResource(R.string.training_bot_create),
                onClick = onCreate,
                enabled = canCreate && !isCreating,
            )
            if (isCreating) {
                CircularProgressIndicator(modifier = Modifier.padding(top = DS.s2))
            }
        }
        errorRes?.let {
            Text(stringResource(it), color = Brand.Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerDetailActionRow(
    isArchived: Boolean,
    showExport: Boolean,
    isExporting: Boolean,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        OutlinedButton(onClick = onEdit) {
            Text(stringResource(R.string.players_edit), color = Brand.Green)
        }
        OutlinedButton(onClick = onArchiveToggle) {
            Text(
                if (isArchived) stringResource(R.string.players_unarchive) else stringResource(R.string.players_archive),
                color = Brand.Amber,
            )
        }
        if (showExport) {
            OutlinedButton(onClick = onExport, enabled = !isExporting) {
                Text(stringResource(R.string.players_detail_export), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PlayerEditSheet(
    name: String,
    notes: String,
    colorToken: String,
    avatarStyle: PlayerAvatarStyle,
    isPrimary: Boolean,
    nameEnabled: Boolean,
    showAvatarPicker: Boolean,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onAvatarStyleChange: (PlayerAvatarStyle) -> Unit,
    onPrimaryChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    SetupSectionCard {
        Text(
            stringResource(R.string.players_edit),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.players_new_name)) },
            enabled = nameEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.players_notes)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (showAvatarPicker) {
            AvatarStylePicker(selected = avatarStyle, onSelect = onAvatarStyleChange)
        }
        HorizontalOptionChips(
            label = stringResource(R.string.players_edit_color),
            options = PlayerColorToken.all.take(8),
            selected = PlayerColorToken.resolve(colorToken),
            onSelect = { onColorChange(it.raw) },
            labelFor = { it.raw },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.players_primary))
            Switch(checked = isPrimary, onCheckedChange = onPrimaryChange)
        }
        PrimaryActionButton(
            title = stringResource(R.string.players_edit),
            onClick = onSave,
            enabled = name.isNotBlank(),
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.common_cancel))
        }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.players_delete), color = Brand.Red)
        }
    }
}
