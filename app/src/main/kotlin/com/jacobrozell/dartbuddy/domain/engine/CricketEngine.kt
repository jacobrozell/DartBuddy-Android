package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.error.AppError
import com.jacobrozell.dartbuddy.domain.error.AppErrorCode
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import com.jacobrozell.dartbuddy.domain.scoring.parseSegmentRaw
import com.jacobrozell.dartbuddy.domain.scoring.segmentRaw
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

enum class CricketTarget(val raw: String) {
    T20("20"),
    T19("19"),
    T18("18"),
    T17("17"),
    T16("16"),
    T15("15"),
    Bull("bull"),
    ;

    val points: Int
        get() = when (this) {
            Bull -> 25
            else -> raw.toInt()
        }

    companion object {
        val allCases = entries
        fun fromRaw(raw: String): CricketTarget? = entries.find { it.raw == raw }
    }
}

@Serializable
data class CricketDartTouch(
    val dartOrder: Int,
    val targetRaw: String,
    val multiplierRaw: String,
    val marksAdded: Int,
    val overflowMarks: Int,
    val pointsAdded: Int,
    val wasMiss: Boolean,
    val segmentRaw: String? = null,
)

@Serializable
data class CricketTurnEvent(
    val payloadVersion: Int = 2,
    val id: String,
    val playerId: String,
    val turnIndex: Int,
    val roundIndex: Int,
    val legIndex: Int? = null,
    val setIndex: Int? = null,
    val totalPointsAdded: Int,
    val targetsTouched: List<CricketDartTouch>,
    val timestampEpochMillis: Long,
) {
    val effectiveLegIndex: Int get() = legIndex ?: 0
    val effectiveSetIndex: Int get() = setIndex ?: 0
}

@Serializable
data class CricketPlayerState(
    val playerId: String,
    val score: Int,
    val marks: Map<String, Int>,
    val legsWon: Int = 0,
    val setsWon: Int = 0,
)

@Serializable
data class CricketState(
    val config: MatchConfigCricket,
    val players: List<CricketPlayerState>,
    val currentPlayerIndex: Int,
    val roundIndex: Int,
    val turnIndex: Int,
    val legIndex: Int = 0,
    val setIndex: Int = 0,
    val winnerPlayerId: String? = null,
    val isComplete: Boolean = false,
)

data class CricketTurnOutcome(
    val updatedState: CricketState,
    val event: CricketTurnEvent,
)

object CricketEngine {
    fun makeInitialState(config: MatchConfigCricket, playerIds: List<String>): CricketState {
        require(config.legsToWin > 0) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.x01.invalidLegCount")
        }
        if (config.setsEnabled && (config.setsToWin ?: 0) <= 0) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.x01.invalidSetCount")
        }
        require(playerIds.size >= 2) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.players.minimum")
        }

        val marksSeed = CricketTarget.allCases.associate { it.raw to 0 }
        val players = playerIds.map { id ->
            CricketPlayerState(playerId = id, score = 0, marks = marksSeed)
        }
        return CricketState(
            config = config,
            players = players,
            currentPlayerIndex = 0,
            roundIndex = 0,
            turnIndex = 0,
        )
    }

    fun submitTurn(
        state: CricketState,
        darts: List<DartInput>,
        timestamp: Instant = Instant.now(),
    ): CricketTurnOutcome {
        if (state.isComplete) {
            throw AppError(AppErrorCode.InvalidGameState, "error.match.completed")
        }
        require(darts.size <= 3) {
            throw AppError(AppErrorCode.ValidationFailed, "error.turn.maxDarts")
        }

        var updated = state
        val playerIndex = updated.currentPlayerIndex
        val playerId = updated.players[playerIndex].playerId
        val touches = mutableListOf<CricketDartTouch>()
        var totalPointsAdded = 0
        val updatedPlayers = updated.players.toMutableList()

        for ((offset, dart) in darts.withIndex()) {
            val order = offset + 1
            val targetRaw = dart.segment.cricketTargetRaw
            val target = targetRaw?.let { CricketTarget.fromRaw(it) }

            if (target == null) {
                touches.add(
                    CricketDartTouch(
                        dartOrder = order,
                        targetRaw = "miss",
                        multiplierRaw = dart.multiplier.name.lowercase(),
                        marksAdded = 0,
                        overflowMarks = 0,
                        pointsAdded = 0,
                        wasMiss = true,
                        segmentRaw = segmentRaw(dart.segment),
                    ),
                )
                continue
            }

            val incomingMarks = marksForCricket(dart)
            val beforeMarks = updatedPlayers[playerIndex].marks[target.raw] ?: 0
            val neededToClose = maxOf(0, 3 - beforeMarks)
            val marksAdded = minOf(neededToClose, incomingMarks)
            val overflow = maxOf(0, incomingMarks - marksAdded)
            val newMarks = (updatedPlayers[playerIndex].marks + (target.raw to minOf(3, beforeMarks + incomingMarks)))
            updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(marks = newMarks)

            val scoring = applyOverflowScoring(
                target = target,
                overflowMarks = overflow,
                actingPlayerIndex = playerIndex,
                players = updatedPlayers,
                config = updated.config,
            )
            updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(
                score = updatedPlayers[playerIndex].score + scoring.throwerPoints,
            )
            totalPointsAdded += scoring.visitTotal

            touches.add(
                CricketDartTouch(
                    dartOrder = order,
                    targetRaw = target.raw,
                    multiplierRaw = dart.multiplier.name.lowercase(),
                    marksAdded = marksAdded,
                    overflowMarks = overflow,
                    pointsAdded = scoring.visitTotal,
                    wasMiss = dart.isMiss,
                    segmentRaw = segmentRaw(dart.segment),
                ),
            )
        }

        updated = updated.copy(players = updatedPlayers)

        legWinnerIndex(updated, playerIndex)?.let { winnerIndex ->
            updated = completeLeg(updated, winnerIndex)
        }

        if (!updated.isComplete) {
            val nextIndex = (playerIndex + 1) % updated.players.size
            var roundIndex = updated.roundIndex
            if (nextIndex == 0) roundIndex += 1
            updated = updated.copy(currentPlayerIndex = nextIndex, roundIndex = roundIndex)
        }
        updated = updated.copy(turnIndex = updated.turnIndex + 1)

        val event = CricketTurnEvent(
            id = UUID.randomUUID().toString(),
            playerId = playerId,
            turnIndex = state.turnIndex,
            roundIndex = state.roundIndex,
            legIndex = state.legIndex,
            setIndex = state.setIndex,
            totalPointsAdded = totalPointsAdded,
            targetsTouched = touches,
            timestampEpochMillis = timestamp.toEpochMilli(),
        )
        return CricketTurnOutcome(updated, event)
    }

    fun replay(config: MatchConfigCricket, playerIds: List<String>, events: List<CricketTurnEvent>): CricketState {
        var state = makeInitialState(config, playerIds)
        for (event in events) {
            val darts = event.targetsTouched.map(::dartInputFromTouch)
            state = submitTurn(state, darts, Instant.ofEpochMilli(event.timestampEpochMillis)).updatedState
        }
        return state
    }

    fun dartInputFromTouch(touch: CricketDartTouch): DartInput {
        val multiplier = DartMultiplier.entries.find { it.name.equals(touch.multiplierRaw, ignoreCase = true) }
            ?: DartMultiplier.Single
        val segment = touch.segmentRaw?.let { parseSegmentRaw(it) } ?: parseTargetToSegment(touch.targetRaw)
        return DartInput(multiplier = multiplier, segment = segment, isMiss = touch.wasMiss)
    }

    private data class OverflowScoringResult(val throwerPoints: Int, val visitTotal: Int)

    private fun applyOverflowScoring(
        target: CricketTarget,
        overflowMarks: Int,
        actingPlayerIndex: Int,
        players: MutableList<CricketPlayerState>,
        config: MatchConfigCricket,
    ): OverflowScoringResult {
        if (overflowMarks <= 0 || !config.pointsEnabled) {
            return OverflowScoringResult(0, 0)
        }
        val valuePerMark = target.points
        return when (config.scoringMode) {
            CricketScoringMode.Standard -> {
                val anyOpponentOpen = players.withIndex().any { (index, player) ->
                    index != actingPlayerIndex && (player.marks[target.raw] ?: 0) < 3
                }
                if (!anyOpponentOpen) return OverflowScoringResult(0, 0)
                val points = overflowMarks * valuePerMark
                OverflowScoringResult(points, points)
            }
            CricketScoringMode.CutThroat -> {
                var inflicted = 0
                for (index in players.indices) {
                    if (index == actingPlayerIndex) continue
                    if ((players[index].marks[target.raw] ?: 0) >= 3) continue
                    val credit = overflowMarks * valuePerMark
                    players[index] = players[index].copy(score = players[index].score + credit)
                    inflicted += credit
                }
                OverflowScoringResult(0, inflicted)
            }
        }
    }

    private fun legWinnerIndex(state: CricketState, actingPlayerIndex: Int): Int? {
        if (state.config.pointsEnabled) {
            if (!allPlayersClosedAllTargets(state.players)) return null
            return legWinnerIndexInPlayers(state.players, state.config)
        }
        if (!isPlayerClosedAllTargets(state.players[actingPlayerIndex])) return null
        return actingPlayerIndex
    }

    private fun completeLeg(state: CricketState, winnerIndex: Int): CricketState {
        val updatedPlayers = state.players.toMutableList()
        val winnerLegs = updatedPlayers[winnerIndex].legsWon + 1
        updatedPlayers[winnerIndex] = updatedPlayers[winnerIndex].copy(legsWon = winnerLegs)
        var updated = state.copy(players = updatedPlayers, legIndex = state.legIndex + 1)

        if (winnerLegs >= effectiveLegsToWin(state.config)) {
            if (state.config.setsEnabled) {
                val winnerSets = updatedPlayers[winnerIndex].setsWon + 1
                updatedPlayers[winnerIndex] = updatedPlayers[winnerIndex].copy(setsWon = winnerSets)
                for (i in updatedPlayers.indices) {
                    updatedPlayers[i] = updatedPlayers[i].copy(legsWon = 0)
                }
                updated = updated.copy(players = updatedPlayers, setIndex = state.setIndex + 1)
                if (winnerSets >= effectiveSetsToWin(state.config)) {
                    updated = updated.copy(
                        winnerPlayerId = updatedPlayers[winnerIndex].playerId,
                        isComplete = true,
                    )
                }
            } else {
                updated = updated.copy(
                    winnerPlayerId = updatedPlayers[winnerIndex].playerId,
                    isComplete = true,
                )
            }
        }
        return if (!updated.isComplete) resetLeg(updated) else updated
    }

    private fun resetLeg(state: CricketState): CricketState {
        val marksSeed = CricketTarget.allCases.associate { it.raw to 0 }
        val players = state.players.map { it.copy(score = 0, marks = marksSeed) }
        return state.copy(players = players)
    }

    private fun effectiveLegsToWin(config: MatchConfigCricket): Int = when (config.legFormat) {
        X01LegFormat.FirstTo -> config.legsToWin
        X01LegFormat.BestOf -> config.legsToWin / 2 + 1
    }

    private fun effectiveSetsToWin(config: MatchConfigCricket): Int {
        val target = config.setsToWin ?: 1
        return when (config.legFormat) {
            X01LegFormat.FirstTo -> target
            X01LegFormat.BestOf -> target / 2 + 1
        }
    }

    private fun isPlayerClosedAllTargets(player: CricketPlayerState): Boolean =
        CricketTarget.allCases.all { (player.marks[it.raw] ?: 0) >= 3 }

    private fun allPlayersClosedAllTargets(players: List<CricketPlayerState>): Boolean =
        players.all(::isPlayerClosedAllTargets)

    private fun legWinnerIndexInPlayers(players: List<CricketPlayerState>, config: MatchConfigCricket): Int? =
        when (config.scoringMode) {
            CricketScoringMode.Standard -> {
                val maxScore = players.maxOfOrNull { it.score } ?: return null
                players.indexOfFirst { it.score == maxScore }.takeIf { it >= 0 }
            }
            CricketScoringMode.CutThroat -> {
                val minScore = players.minOfOrNull { it.score } ?: return null
                players.indexOfFirst { it.score == minScore }.takeIf { it >= 0 }
            }
        }

    private fun marksForCricket(dart: DartInput): Int {
        if (dart.isMiss) return 0
        return when (dart.segment) {
            DartSegment.InnerBull -> 2
            DartSegment.OuterBull -> 1
            is DartSegment.OneToTwenty -> dart.multiplier.markValue
            DartSegment.Miss -> 0
        }
    }

    private fun parseTargetToSegment(raw: String): DartSegment = when (raw) {
        "bull" -> DartSegment.OuterBull
        else -> raw.toIntOrNull()?.takeIf { it in 15..20 }?.let { DartSegment.OneToTwenty(it) }
            ?: DartSegment.Miss
    }
}
