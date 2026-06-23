package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.engine.CricketPlayerState
import com.jacobrozell.dartbuddy.domain.engine.CricketState
import com.jacobrozell.dartbuddy.domain.engine.X01PlayerState
import com.jacobrozell.dartbuddy.domain.engine.X01State

data class ForfeitCandidate(
    val playerId: String,
    val displayName: String,
    val standingSummary: String,
)

sealed class ForfeitWinnerResolution {
    data class Automatic(val winnerPlayerId: String?) : ForfeitWinnerResolution()
    data class ChooseAmongTied(val candidates: List<ForfeitCandidate>) : ForfeitWinnerResolution()
}

private data class ForfeitStanding(
    val playerId: String,
    val primaryScore: Int,
    val tieBreakKey: Int,
    val summaryValue: Int,
    val prefersLowerScore: Boolean,
    val isX01: Boolean,
)

object MatchForfeitResolver {
    fun humanParticipantIds(participantIds: List<String>, isBotById: Map<String, Boolean>): List<String> =
        participantIds.filter { isBotById[it] != true }

    fun resolveX01(state: X01State, participantNames: List<String>, participantIds: List<String>, forfeitingPlayerId: String): ForfeitWinnerResolution =
        resolve(
            remainingIds = participantIds.filter { it != forfeitingPlayerId },
            participantNames = participantNames,
            participantIds = participantIds,
            standings = { playerId, turnOrder ->
                val player = state.players.first { it.playerId == playerId }
                x01Standing(player, state, turnOrder)
            },
        )

    fun resolveCricket(state: CricketState, participantNames: List<String>, participantIds: List<String>, forfeitingPlayerId: String): ForfeitWinnerResolution =
        resolve(
            remainingIds = participantIds.filter { it != forfeitingPlayerId },
            participantNames = participantNames,
            participantIds = participantIds,
            standings = { playerId, turnOrder ->
                val player = state.players.first { it.playerId == playerId }
                cricketStanding(player, turnOrder)
            },
        )

    private fun resolve(
        remainingIds: List<String>,
        participantNames: List<String>,
        participantIds: List<String>,
        standings: (playerId: String, turnOrder: Int) -> ForfeitStanding,
    ): ForfeitWinnerResolution {
        if (remainingIds.isEmpty()) return ForfeitWinnerResolution.Automatic(null)
        if (remainingIds.size == 1) return ForfeitWinnerResolution.Automatic(remainingIds.first())

        val nameById = participantIds.zip(participantNames).toMap()
        val sorted = remainingIds.map { id ->
            standings(id, participantIds.indexOf(id))
        }.sortedWith(compareStandings)

        val best = sorted.first()
        val tied = sorted.filter { isTied(it, best) }
        if (tied.size == 1) return ForfeitWinnerResolution.Automatic(best.playerId)

        val candidates = tied.map { standing ->
            ForfeitCandidate(
                playerId = standing.playerId,
                displayName = nameById[standing.playerId] ?: standing.playerId.take(6),
                standingSummary = if (standing.isX01) {
                    "${standing.summaryValue} remaining"
                } else {
                    "${standing.summaryValue} points"
                },
            )
        }
        return ForfeitWinnerResolution.ChooseAmongTied(candidates)
    }

    private val compareStandings = Comparator<ForfeitStanding> { lhs, rhs ->
        when {
            lhs.prefersLowerScore && lhs.primaryScore != rhs.primaryScore -> lhs.primaryScore.compareTo(rhs.primaryScore)
            !lhs.prefersLowerScore && lhs.primaryScore != rhs.primaryScore -> rhs.primaryScore.compareTo(lhs.primaryScore)
            else -> lhs.tieBreakKey.compareTo(rhs.tieBreakKey)
        }
    }

    private fun isTied(lhs: ForfeitStanding, rhs: ForfeitStanding): Boolean =
        lhs.primaryScore == rhs.primaryScore && lhs.tieBreakKey == rhs.tieBreakKey

    private fun x01Standing(player: X01PlayerState, state: X01State, turnOrder: Int): ForfeitStanding {
        val legsLost = state.players.maxOfOrNull { it.legsWon }?.let { maxWon -> maxOf(0, maxWon - player.legsWon) } ?: 0
        val setsLost = state.players.maxOfOrNull { it.setsWon }?.let { maxWon -> maxOf(0, maxWon - player.setsWon) } ?: 0
        return ForfeitStanding(
            playerId = player.playerId,
            primaryScore = player.remainingScore,
            tieBreakKey = legsLost * 10_000 + setsLost * 100 + turnOrder,
            summaryValue = player.remainingScore,
            prefersLowerScore = true,
            isX01 = true,
        )
    }

    private fun cricketStanding(player: CricketPlayerState, turnOrder: Int): ForfeitStanding {
        val marksClosed = player.marks.values.sumOf { minOf(it, 3) }
        return ForfeitStanding(
            playerId = player.playerId,
            primaryScore = player.score,
            tieBreakKey = -(marksClosed * 100) + turnOrder,
            summaryValue = player.score,
            prefersLowerScore = false,
            isX01 = false,
        )
    }
}
