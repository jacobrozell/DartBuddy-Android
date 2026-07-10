package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.model.MatchStatus

data class PlayerListSummary(
    val games: Int = 0,
    val wins: Int = 0,
)

object PlayerListStatsLoader {
    fun build(matches: List<MatchSummary>): Map<String, PlayerListSummary> {
        val summaries = mutableMapOf<String, PlayerListSummary>()
        for (match in matches) {
            if (match.status != MatchStatus.Completed && match.status != MatchStatus.Forfeited) continue
            for (id in match.participantIds) {
                val current = summaries[id] ?: PlayerListSummary()
                summaries[id] = current.copy(
                    games = current.games + 1,
                    wins = current.wins + if (match.winnerPlayerId == id) 1 else 0,
                )
            }
        }
        return summaries
    }
}
