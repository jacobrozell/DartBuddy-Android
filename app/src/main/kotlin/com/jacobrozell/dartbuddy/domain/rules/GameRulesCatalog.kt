package com.jacobrozell.dartbuddy.domain.rules

import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.MatchType

data class GameRulesSection(
    val id: String,
    val titleRes: Int,
    val bodyRes: Int,
)

data class GameRulesGuide(
    val id: String,
    val matchType: MatchType,
    val sections: List<GameRulesSection>,
)

object GameRulesCatalog {
    val supportedMatchTypes: List<MatchType> = listOf(MatchType.X01, MatchType.Cricket)

    fun hasGuide(matchType: MatchType): Boolean = all.any { it.matchType == matchType }

    fun guide(matchType: MatchType): GameRulesGuide =
        all.firstOrNull { it.matchType == matchType } ?: x01Guide

    private val x01Guide = GameRulesGuide(
        id = "x01",
        matchType = MatchType.X01,
        sections = listOf(
            GameRulesSection("overview", R.string.play_rules_x01_overview_title, R.string.play_rules_x01_overview_body),
            GameRulesSection("bust", R.string.play_rules_x01_bust_title, R.string.play_rules_x01_bust_body),
            GameRulesSection("checkIn", R.string.play_rules_x01_checkIn_title, R.string.play_rules_x01_checkIn_body),
            GameRulesSection("checkOut", R.string.play_rules_x01_checkOut_title, R.string.play_rules_x01_checkOut_body),
            GameRulesSection("format", R.string.play_rules_x01_format_title, R.string.play_rules_x01_format_body),
        ),
    )

    private val cricketGuide = GameRulesGuide(
        id = "cricket",
        matchType = MatchType.Cricket,
        sections = listOf(
            GameRulesSection("basics", R.string.play_rules_cricket_basics_title, R.string.play_rules_cricket_basics_body),
            GameRulesSection("normalScore", R.string.play_rules_cricket_normalScore_title, R.string.play_rules_cricket_normalScore_body),
            GameRulesSection("cutThroatScore", R.string.play_rules_cricket_cutThroatScore_title, R.string.play_rules_cricket_cutThroatScore_body),
            GameRulesSection("noScore", R.string.play_rules_cricket_noScore_title, R.string.play_rules_cricket_noScore_body),
        ),
    )

    private val all: List<GameRulesGuide> = listOf(x01Guide, cricketGuide)
}
