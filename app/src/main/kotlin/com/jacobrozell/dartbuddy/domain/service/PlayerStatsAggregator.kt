package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import java.time.Instant

data class StatsTrendPoint(
    val id: String,
    val date: Instant,
    val average3Dart: Double,
)

data class PlayerModeStats(
    val games: Int,
    val wins: Int,
    val stats: PlayerMatchStats,
) {
    val winPercent: Double get() = if (games > 0) wins.toDouble() / games * 100.0 else 0.0
}

data class RecentPlayerMatch(
    val id: String,
    val matchType: MatchType,
    val opponentLabel: String,
    val playedAt: Instant,
    val didWin: Boolean,
)

object PlayerStatsAggregator {
    suspend fun aggregate(
        matches: List<MatchSummary>,
        playerId: String,
        playerName: String,
        matchType: MatchType,
        statsForMatch: suspend (MatchSummary) -> List<PlayerMatchStats>?,
    ): PlayerModeStats? {
        var games = 0
        var wins = 0
        val bucket = MutableBucket(playerId, playerName)
        matches
            .filter { it.matchType == matchType }
            .forEach { summary ->
                val row = statsForMatch(summary)?.find { it.playerId == playerId } ?: return@forEach
                games++
                if (summary.winnerPlayerId == playerId) wins++
                bucket.merge(row)
            }
        if (games == 0) return null
        return PlayerModeStats(games = games, wins = wins, stats = bucket.toStats())
    }

    fun recentMatches(
        matches: List<MatchSummary>,
        playerId: String,
        limit: Int = 10,
    ): List<RecentPlayerMatch> = matches
        .filter { it.participantIds.contains(playerId) }
        .filter { it.status == MatchStatus.Completed || it.status == MatchStatus.Forfeited }
        .sortedByDescending { it.startedAt }
        .take(limit)
        .map { summary ->
            val opponents = summary.participantIds.zip(summary.participantNames)
                .filter { (id, _) -> id != playerId }
                .map { (_, name) -> name }
            RecentPlayerMatch(
                id = summary.id,
                matchType = summary.matchType,
                opponentLabel = opponents.joinToString(", ").ifBlank { "—" },
                playedAt = summary.startedAt,
                didWin = summary.winnerPlayerId == playerId,
            )
        }

    private class MutableBucket(private val playerId: String, private val name: String) {
        private var darts = 0
        private var points = 0
        private var highestScore = 0
        private var highestCheckout = 0
        private var checkouts = 0
        private var legsWon = 0
        private var setsWon = 0
        private var score = 0
        private var doubles = 0
        private var triples = 0
        private var cricketMarks = 0
        private var cricketRounds = 0
        private val hitsBySector = mutableMapOf<String, Int>()

        fun merge(row: PlayerMatchStats) {
            darts += row.darts
            points += row.points
            highestScore = maxOf(highestScore, row.highestScore)
            highestCheckout = maxOf(highestCheckout, row.highestCheckout)
            checkouts += row.checkouts
            legsWon += row.legsWon
            setsWon += row.setsWon
            score += row.score
            doubles += row.doubles
            triples += row.triples
            row.hitsBySector.forEach { (sector, count) ->
                hitsBySector[sector] = (hitsBySector[sector] ?: 0) + count
            }
            if (row.marksPerRound > 0) {
                cricketMarks += (row.marksPerRound * row.darts / 3).toInt().coerceAtLeast(1)
                cricketRounds += 1
            }
        }

        fun toStats(): PlayerMatchStats {
            val mpr = if (cricketRounds > 0) cricketMarks.toDouble() / cricketRounds else 0.0
            return PlayerMatchStats(
                playerId = playerId,
                name = name,
                darts = darts,
                points = points,
                highestScore = highestScore,
                highestCheckout = highestCheckout,
                checkouts = checkouts,
                legsWon = legsWon,
                setsWon = setsWon,
                score = score,
                marksPerRound = mpr,
                doubles = doubles,
                triples = triples,
                hitsBySector = hitsBySector.toMap(),
            )
        }
    }

    suspend fun x01TrendPoints(
        matches: List<MatchSummary>,
        playerId: String,
        eventsForMatch: suspend (MatchSummary) -> List<X01TurnEvent>?,
    ): List<StatsTrendPoint> = matches
        .filter { it.matchType == MatchType.X01 && it.participantIds.contains(playerId) }
        .sortedBy { it.startedAt }
        .mapNotNull { summary ->
            val events = eventsForMatch(summary) ?: return@mapNotNull null
            var points = 0
            var darts = 0
            events.filter { it.playerId == playerId }.forEach { turn ->
                points += turn.appliedTotal
                darts += turn.effectiveDartsThrown
            }
            if (darts == 0) return@mapNotNull null
            StatsTrendPoint(
                id = summary.id,
                date = summary.startedAt,
                average3Dart = (points.toDouble() / darts) * 3.0,
            )
        }
}
