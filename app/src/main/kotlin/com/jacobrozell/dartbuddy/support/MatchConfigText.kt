package com.jacobrozell.dartbuddy.support

import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat

object MatchConfigText {
    fun x01InlineConfig(config: MatchConfigX01, l10n: L10n): String =
        x01DetailParts(config, l10n).joinToString(", ")

    fun cricketInlineConfig(config: MatchConfigCricket, l10n: L10n): String =
        cricketDetailParts(config, l10n).joinToString(" · ")

    private fun x01DetailParts(config: MatchConfigX01, l10n: L10n): List<String> {
        val parts = mutableListOf(
            config.startScore.toString(),
            checkoutLabel(config.checkoutMode, l10n),
        )
        if (config.checkInMode != X01CheckInMode.StraightIn) {
            parts.add(checkInLabel(config.checkInMode, l10n))
        }
        val format = legFormatLabel(config.legFormat, l10n)
        if (config.setsEnabled) {
            parts.add(targetSets(format, config.setsToWin ?: 1, l10n))
        }
        parts.add(targetLegs(format, config.legsToWin, l10n))
        return parts
    }

    private fun cricketDetailParts(config: MatchConfigCricket, l10n: L10n): List<String> {
        val parts = mutableListOf<String>()
        if (!config.pointsEnabled) {
            parts.add(l10n.get(R.string.play_cricket_subtitle_noScore))
        } else {
            parts.add(scoringModeLabel(config.scoringMode, l10n))
        }
        val format = legFormatLabel(config.legFormat, l10n)
        if (config.setsEnabled) {
            parts.add(targetSets(format, config.setsToWin ?: 1, l10n))
        }
        parts.add(targetLegs(format, config.legsToWin, l10n))
        return parts
    }

    private fun checkoutLabel(mode: X01CheckoutMode, l10n: L10n): String = when (mode) {
        X01CheckoutMode.SingleOut -> l10n.get(R.string.x01_checkout_straightOut)
        X01CheckoutMode.DoubleOut -> l10n.get(R.string.x01_checkout_doubleOut)
        X01CheckoutMode.MasterOut -> l10n.get(R.string.x01_checkout_masterOut)
    }

    private fun checkInLabel(mode: X01CheckInMode, l10n: L10n): String = when (mode) {
        X01CheckInMode.StraightIn -> l10n.get(R.string.x01_checkin_straightIn)
        X01CheckInMode.DoubleIn -> l10n.get(R.string.x01_checkin_doubleIn)
        X01CheckInMode.MasterIn -> l10n.get(R.string.x01_checkin_masterIn)
    }

    private fun legFormatLabel(format: X01LegFormat, l10n: L10n): String = when (format) {
        X01LegFormat.FirstTo -> l10n.get(R.string.x01_legFormat_firstTo)
        X01LegFormat.BestOf -> l10n.get(R.string.x01_legFormat_bestOf)
    }

    private fun scoringModeLabel(mode: CricketScoringMode, l10n: L10n): String = when (mode) {
        CricketScoringMode.Standard -> l10n.get(R.string.play_cricket_mode_normal)
        CricketScoringMode.CutThroat -> l10n.get(R.string.play_cricket_mode_cutThroat)
    }

    private fun targetSets(format: String, count: Int, l10n: L10n): String =
        if (count == 1) {
            l10n.format(R.string.match_config_setSingular, format, count)
        } else {
            l10n.format(R.string.match_config_setsPlural, format, count)
        }

    private fun targetLegs(format: String, count: Int, l10n: L10n): String =
        if (count == 1) {
            l10n.format(R.string.match_config_legSingular, format, count)
        } else {
            l10n.format(R.string.match_config_legsPlural, format, count)
        }
}
