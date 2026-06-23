package com.jacobrozell.dartbuddy.domain.bots

import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import kotlinx.coroutines.delay

object BotTurnCoordinator {
    private const val THINK_DELAY_MS = 450L
    private const val DART_STAGGER_MS = 380L
    private const val SUBMIT_PAUSE_MS = 200L

    fun difficultyFor(player: PlayerSummary): BotDifficulty? {
        if (player.botKindRaw == "custom") return null
        val raw = player.botDifficultyRaw ?: return null
        return runCatching { BotDifficulty.valueOf(raw) }.getOrNull()
            ?: BotDifficulty.fromRawValue(raw)
    }

    fun skillProfileFor(player: PlayerSummary, @Suppress("UNUSED_PARAMETER") matchType: MatchType): BotSkillProfile? {
        if (!player.isBot) return null
        if (player.botKindRaw == "custom") {
            return CustomBotConfigurationCodec.decode(player.botDifficultyRaw)?.resolvedCanonicalProfile()
        }
        return difficultyFor(player)?.skillProfile
    }

    fun customMetricsFor(player: PlayerSummary): CustomBotMetrics? {
        if (player.botKindRaw != "custom") return null
        return CustomBotConfigurationCodec.decode(player.botDifficultyRaw)?.metrics
    }

    fun customConfigurationFor(player: PlayerSummary): CustomBotConfiguration? {
        if (player.botKindRaw != "custom") return null
        return CustomBotConfigurationCodec.decode(player.botDifficultyRaw)
            ?: CustomBotConfiguration.from(
                CustomBotMetrics(
                    x01Average = CustomBotMetrics.DEFAULT_X01_AVERAGE,
                    cricketMPR = CustomBotMetrics.DEFAULT_CRICKET_MPR,
                ),
            )
    }

    suspend fun thinkDelay() {
        delay(THINK_DELAY_MS)
    }

    suspend fun playStaggeredDarts(
        darts: List<DartInput>,
        soundEnabled: Boolean,
        botDartHapticsEnabled: Boolean,
        feedback: FeedbackServices,
        onDart: (List<DartInput>) -> Unit,
    ) {
        val accumulated = mutableListOf<DartInput>()
        for (dart in darts) {
            delay(DART_STAGGER_MS)
            feedback.playBotDartFeedback(dart, soundEnabled, botDartHapticsEnabled)
            accumulated.add(dart)
            onDart(accumulated.toList())
        }
        delay(SUBMIT_PAUSE_MS)
    }
}
