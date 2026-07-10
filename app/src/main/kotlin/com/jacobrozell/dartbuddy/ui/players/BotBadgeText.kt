package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator

@Composable
fun botBadgeText(player: PlayerSummary): String? {
    if (!player.isBot) return null
    if (player.botKindRaw == "custom") {
        return BotTurnCoordinator.customMetricsFor(player)?.let {
            stringResource(R.string.custom_bot_badge_format, it.x01Average, it.cricketMPR)
        } ?: stringResource(R.string.players_bot_label)
    }
    return BotTurnCoordinator.difficultyFor(player)?.displayNameComposable()
        ?: stringResource(R.string.play_setup_botBadge)
}
