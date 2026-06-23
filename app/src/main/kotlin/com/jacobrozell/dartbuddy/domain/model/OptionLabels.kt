package com.jacobrozell.dartbuddy.domain.model

import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.L10n

object OptionLabels {
    fun checkout(mode: X01CheckoutMode, l10n: L10n): String = when (mode) {
        X01CheckoutMode.SingleOut -> l10n.get(R.string.x01_checkout_straightOut)
        X01CheckoutMode.DoubleOut -> l10n.get(R.string.x01_checkout_doubleOut)
        X01CheckoutMode.MasterOut -> l10n.get(R.string.x01_checkout_masterOut)
    }

    fun checkIn(mode: X01CheckInMode, l10n: L10n): String = when (mode) {
        X01CheckInMode.StraightIn -> l10n.get(R.string.x01_checkin_straightIn)
        X01CheckInMode.DoubleIn -> l10n.get(R.string.x01_checkin_doubleIn)
        X01CheckInMode.MasterIn -> l10n.get(R.string.x01_checkin_masterIn)
    }

    fun legFormat(format: X01LegFormat, l10n: L10n): String = when (format) {
        X01LegFormat.FirstTo -> l10n.get(R.string.x01_legFormat_firstTo)
        X01LegFormat.BestOf -> l10n.get(R.string.x01_legFormat_bestOf)
    }

    fun cricketScoring(mode: CricketScoringMode, l10n: L10n): String = when (mode) {
        CricketScoringMode.Standard -> l10n.get(R.string.play_cricket_mode_normal)
        CricketScoringMode.CutThroat -> l10n.get(R.string.play_cricket_mode_cutThroat)
    }
}
