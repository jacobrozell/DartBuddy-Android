package com.jacobrozell.dartbuddy.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MatchType {
    X01,
    Cricket,
}

enum class MatchStatus {
    NotStarted,
    InProgress,
    Completed,
    Forfeited,
    Abandoned,
}

@Serializable
enum class X01CheckoutMode {
    SingleOut,
    DoubleOut,
    MasterOut,
}

@Serializable
enum class X01CheckInMode {
    StraightIn,
    DoubleIn,
    MasterIn,
}

@Serializable
enum class X01LegFormat {
    FirstTo,
    BestOf,
}

@Serializable
enum class CricketScoringMode {
    Standard,
    CutThroat,
}

@Serializable
data class MatchConfigX01(
    val payloadVersion: Int = 1,
    val startScore: Int,
    val legsToWin: Int,
    val setsEnabled: Boolean,
    val setsToWin: Int? = null,
    val checkoutMode: X01CheckoutMode,
    val checkInMode: X01CheckInMode = X01CheckInMode.StraightIn,
    val legFormat: X01LegFormat = X01LegFormat.FirstTo,
)

@Serializable
data class MatchConfigCricket(
    val payloadVersion: Int = 2,
    val bullScoreValue: Int = 25,
    val pointsEnabled: Boolean = true,
    val scoringMode: CricketScoringMode = CricketScoringMode.Standard,
    val legsToWin: Int = 1,
    val setsEnabled: Boolean = false,
    val setsToWin: Int? = null,
    val legFormat: X01LegFormat = X01LegFormat.FirstTo,
)

@Serializable
sealed class MatchConfigPayload {
    @Serializable
    @SerialName("x01")
    data class X01(val config: MatchConfigX01) : MatchConfigPayload()

    @Serializable
    @SerialName("cricket")
    data class Cricket(val config: MatchConfigCricket) : MatchConfigPayload()
}
