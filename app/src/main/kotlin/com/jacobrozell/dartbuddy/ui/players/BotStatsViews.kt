package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.bots.BotDifficultyDisplayProfile
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun BotDifficultyStatsSection(
    profile: BotDifficultyDisplayProfile,
    showsHeader: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        if (showsHeader) {
            Text(
                stringResource(R.string.bot_stats_section),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        BotStatsModeCard(title = stringResource(R.string.play_mode_x01)) {
            BotStatRow(
                label = stringResource(R.string.bot_stats_scoring_visit_range),
                value = BotDifficultyDisplayProfile.range(profile.x01.scoringVisitMin, profile.x01.scoringVisitMax),
            )
            BotStatsDivider()
            HitChanceRows(profile.x01.hitChances)
            BotStatsDivider()
            BotStatRow(
                label = stringResource(R.string.bot_stats_checkout_attempt_rate),
                value = BotDifficultyDisplayProfile.percent(profile.x01.checkoutAttemptChance),
            )
            BotStatRow(
                label = stringResource(R.string.bot_stats_off_board_miss),
                value = BotDifficultyDisplayProfile.percent(profile.x01.offBoardMissChance),
            )
            BotStatRow(
                label = stringResource(R.string.bot_stats_bust_risk),
                value = BotDifficultyDisplayProfile.percent(profile.x01.riskyBustChance),
            )
            BotStatRow(
                label = stringResource(R.string.bot_stats_triple_preference),
                value = BotDifficultyDisplayProfile.percent(profile.x01.triplePreference),
            )
            BotStatRow(
                label = stringResource(R.string.bot_stats_check_in_boost),
                value = BotDifficultyDisplayProfile.percent(profile.x01.checkInHitBoost, signed = true),
            )
            if (profile.x01.innerBullAimChance > 0) {
                BotStatRow(
                    label = stringResource(R.string.bot_stats_inner_bull_aim),
                    value = BotDifficultyDisplayProfile.percent(profile.x01.innerBullAimChance),
                )
            }
            if (profile.x01.masterInTripleOpenerChance > 0) {
                BotStatRow(
                    label = stringResource(R.string.bot_stats_master_in_triple),
                    value = BotDifficultyDisplayProfile.percent(profile.x01.masterInTripleOpenerChance),
                )
            }
        }

        BotStatsModeCard(title = stringResource(R.string.play_mode_cricket)) {
            HitChanceRows(profile.cricket.hitChances)
            BotStatsDivider()
            BotStatRow(
                label = stringResource(R.string.bot_stats_off_board_miss),
                value = BotDifficultyDisplayProfile.percent(profile.cricket.offBoardMissChance),
            )
            BotStatRow(
                label = stringResource(R.string.bot_stats_wrong_bed),
                value = BotDifficultyDisplayProfile.percent(profile.cricket.wrongBedChance),
            )
        }

        Text(
            stringResource(R.string.bot_stats_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BotStatsModeCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DS.s3, vertical = DS.s2),
            verticalArrangement = Arrangement.spacedBy(DS.s2),
        ) {
            content()
        }
    }
}

@Composable
private fun HitChanceRows(chances: BotDifficultyDisplayProfile.HitChances) {
    BotStatRow(
        label = stringResource(R.string.bot_stats_hit_single),
        value = BotDifficultyDisplayProfile.percent(chances.single),
    )
    BotStatRow(
        label = stringResource(R.string.bot_stats_hit_double),
        value = BotDifficultyDisplayProfile.percent(chances.double),
    )
    BotStatRow(
        label = stringResource(R.string.bot_stats_hit_triple),
        value = BotDifficultyDisplayProfile.percent(chances.triple),
    )
}

@Composable
private fun BotStatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Brand.Green, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BotStatsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
fun BotStatSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s1)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                BotDifficultyDisplayProfile.percent(value.toDouble()),
                color = Brand.Green,
                fontWeight = FontWeight.Bold,
            )
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}
