package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.model.MatchType

data class PlayerMatchStats(
    val playerId: String,
    val name: String,
    val darts: Int,
    val points: Int,
    val highestCheckout: Int,
    val legsWon: Int,
    val setsWon: Int,
    val score: Int,
    val marksPerRound: Double,
) {
    val average3Dart: Double
        get() = if (darts > 0) (points.toDouble() / darts) * 3 else 0.0
}

object MatchStatsCalculator {
    fun x01Stats(
        participantIds: List<String>,
        names: List<String>,
        events: List<X01TurnEvent>,
        finalLegsWon: Map<String, Int>,
        finalSetsWon: Map<String, Int>,
        winnerId: String?,
    ): List<PlayerMatchStats> = participantIds.mapIndexed { index, id ->
        val playerEvents = events.filter { it.playerId == id }
        val points = playerEvents.sumOf { it.appliedTotal }
        val darts = playerEvents.sumOf { it.effectiveDartsThrown }
        val highestCheckout = playerEvents.filter { it.didCheckout }.maxOfOrNull { it.startRemaining } ?: 0
        PlayerMatchStats(
            playerId = id,
            name = names.getOrElse(index) { "Player ${index + 1}" },
            darts = darts,
            points = points,
            highestCheckout = highestCheckout,
            legsWon = finalLegsWon[id] ?: 0,
            setsWon = finalSetsWon[id] ?: 0,
            score = 0,
            marksPerRound = 0.0,
        )
    }.sortedWith(
        compareByDescending<PlayerMatchStats> { it.playerId == winnerId }
            .thenByDescending { it.average3Dart },
    )

    fun cricketStats(
        participantIds: List<String>,
        names: List<String>,
        events: List<CricketTurnEvent>,
        finalScores: Map<String, Int>,
        finalLegsWon: Map<String, Int>,
        finalSetsWon: Map<String, Int>,
        winnerId: String?,
    ): List<PlayerMatchStats> = participantIds.mapIndexed { index, id ->
        val playerEvents = events.filter { it.playerId == id }
        val darts = playerEvents.sumOf { it.targetsTouched.size }
        val marks = playerEvents.sumOf { turn ->
            turn.targetsTouched.sumOf { it.marksAdded + it.overflowMarks }
        }
        val rounds = playerEvents.map { it.roundIndex }.distinct().size.coerceAtLeast(1)
        PlayerMatchStats(
            playerId = id,
            name = names.getOrElse(index) { "Player ${index + 1}" },
            darts = darts,
            points = 0,
            highestCheckout = 0,
            legsWon = finalLegsWon[id] ?: 0,
            setsWon = finalSetsWon[id] ?: 0,
            score = finalScores[id] ?: 0,
            marksPerRound = marks.toDouble() / rounds,
        )
    }.sortedWith(
        compareByDescending<PlayerMatchStats> { it.playerId == winnerId }
            .thenByDescending { it.score },
    )

    fun modeLabel(type: MatchType): String = when (type) {
        MatchType.X01 -> "X01"
        MatchType.Cricket -> "Cricket"
    }
}
