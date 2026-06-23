package com.jacobrozell.dartbuddy.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat

@Immutable
data class OptionLabelMaps(
    val checkout: (X01CheckoutMode) -> String,
    val checkIn: (X01CheckInMode) -> String,
    val legFormat: (X01LegFormat) -> String,
    val cricketScoring: (CricketScoringMode) -> String,
    val matchType: (MatchType) -> String,
)

@Composable
fun rememberOptionLabelMaps(): OptionLabelMaps {
    val checkoutLabels = mapOf(
        X01CheckoutMode.SingleOut to stringResource(R.string.x01_checkout_straightOut),
        X01CheckoutMode.DoubleOut to stringResource(R.string.x01_checkout_doubleOut),
        X01CheckoutMode.MasterOut to stringResource(R.string.x01_checkout_masterOut),
    )
    val checkInLabels = mapOf(
        X01CheckInMode.StraightIn to stringResource(R.string.x01_checkin_straightIn),
        X01CheckInMode.DoubleIn to stringResource(R.string.x01_checkin_doubleIn),
        X01CheckInMode.MasterIn to stringResource(R.string.x01_checkin_masterIn),
    )
    val legFormatLabels = mapOf(
        X01LegFormat.FirstTo to stringResource(R.string.x01_legFormat_firstTo),
        X01LegFormat.BestOf to stringResource(R.string.x01_legFormat_bestOf),
    )
    val scoringLabels = mapOf(
        CricketScoringMode.Standard to stringResource(R.string.play_cricket_mode_normal),
        CricketScoringMode.CutThroat to stringResource(R.string.play_cricket_mode_cutThroat),
    )
    val modeLabels = mapOf(
        MatchType.X01 to stringResource(R.string.play_x01_title),
        MatchType.Cricket to stringResource(R.string.play_cricket_title),
    )
    return remember(checkoutLabels, checkInLabels, legFormatLabels, scoringLabels, modeLabels) {
        OptionLabelMaps(
            checkout = { checkoutLabels[it] ?: it.name },
            checkIn = { checkInLabels[it] ?: it.name },
            legFormat = { legFormatLabels[it] ?: it.name },
            cricketScoring = { scoringLabels[it] ?: it.name },
            matchType = { modeLabels[it] ?: it.name },
        )
    }
}
