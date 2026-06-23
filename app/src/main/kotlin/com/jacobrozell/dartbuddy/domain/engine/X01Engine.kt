package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.error.AppError
import com.jacobrozell.dartbuddy.domain.error.AppErrorCode
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import com.jacobrozell.dartbuddy.domain.scoring.parseSegmentRaw
import com.jacobrozell.dartbuddy.domain.scoring.segmentRaw
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class X01PlayerState(
    val playerId: String,
    val remainingScore: Int,
    val legsWon: Int,
    val setsWon: Int,
    val checkedIn: Boolean? = null,
) {
    val isCheckedIn: Boolean get() = checkedIn ?: true
}

@Serializable
data class X01State(
    val config: MatchConfigX01,
    val players: List<X01PlayerState>,
    val currentPlayerIndex: Int,
    val legIndex: Int,
    val setIndex: Int,
    val turnIndex: Int,
    val winnerPlayerId: String? = null,
    val isComplete: Boolean = false,
)

@Serializable
data class X01DartEvent(
    val dartOrder: Int,
    val multiplierRaw: String,
    val segmentRaw: String,
    val points: Int,
    val wasMiss: Boolean,
)

@Serializable
data class X01TurnEvent(
    val payloadVersion: Int = 1,
    val id: String,
    val playerId: String,
    val turnIndex: Int,
    val legIndex: Int,
    val setIndex: Int,
    val startRemaining: Int,
    val enteredTotal: Int,
    val appliedTotal: Int,
    val endRemaining: Int,
    val isBust: Boolean,
    val didCheckout: Boolean,
    val checkoutModeRaw: String,
    val checkoutDartCount: Int? = null,
    val darts: List<X01DartEvent> = emptyList(),
    val timestampEpochMillis: Long,
    val dartsThrown: Int? = null,
) {
    val effectiveDartsThrown: Int get() = dartsThrown ?: darts.size

    fun reconstructedDarts(): List<DartInput> = darts.map { dartEvent ->
        DartInput(
            multiplier = DartMultiplier.entries.find { it.name.equals(dartEvent.multiplierRaw, ignoreCase = true) }
                ?: DartMultiplier.Single,
            segment = parseSegmentRaw(dartEvent.segmentRaw),
            isMiss = dartEvent.wasMiss,
        )
    }
}

data class X01TurnOutcome(
    val updatedState: X01State,
    val event: X01TurnEvent,
)

data class X01VisitScore(
    val appliedTotal: Int,
    val checkedInAfter: Boolean,
    val dartCounts: List<Boolean>,
)

object X01Engine {
    private val validStartScores = com.jacobrozell.dartbuddy.domain.model.X01StartScores.all.toSet()

    fun makeInitialState(config: MatchConfigX01, playerIds: List<String>): X01State {
        require(config.startScore in validStartScores) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.x01.invalidStartScore")
        }
        require(config.legsToWin > 0) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.x01.invalidLegCount")
        }
        if (config.setsEnabled && (config.setsToWin ?: 0) <= 0) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.x01.invalidSetCount")
        }
        require(playerIds.isNotEmpty()) {
            throw AppError(AppErrorCode.ValidationFailed, "error.match.players.minimum")
        }

        val opensImmediately = config.checkInMode == X01CheckInMode.StraightIn
        val players = playerIds.map { id ->
            X01PlayerState(
                playerId = id,
                remainingScore = config.startScore,
                legsWon = 0,
                setsWon = 0,
                checkedIn = opensImmediately,
            )
        }
        return X01State(
            config = config,
            players = players,
            currentPlayerIndex = 0,
            legIndex = 0,
            setIndex = 0,
            turnIndex = 0,
        )
    }

    fun submitTurn(
        state: X01State,
        enteredTotal: Int? = null,
        darts: List<DartInput>? = null,
        timestamp: Instant = Instant.now(),
    ): X01TurnOutcome {
        if (state.isComplete) {
            throw AppError(AppErrorCode.InvalidGameState, "error.match.completed")
        }

        var updated = state
        val dartList = darts ?: emptyList()
        val playerIndex = updated.currentPlayerIndex
        val startRemaining = updated.players[playerIndex].remainingScore
        var checkedIn = updated.players[playerIndex].isCheckedIn
        val checkInMode = updated.config.checkInMode

        val visitScore = if (dartList.isEmpty()) {
            checkedIn = true
            X01VisitScore(enteredTotal ?: 0, checkedIn, emptyList())
        } else {
            scoreVisit(dartList, checkInMode, checkedIn)
        }
        checkedIn = visitScore.checkedInAfter
        val turnTotal = visitScore.appliedTotal

        if (enteredTotal != null && dartList.isNotEmpty() && enteredTotal != turnTotal) {
            throw AppError(AppErrorCode.ValidationFailed, "error.turn.totalMismatch")
        }
        require(turnTotal in 0..180) {
            throw AppError(AppErrorCode.ValidationFailed, "error.turn.invalidTotal")
        }

        var isBust = false
        var didCheckout = false
        var appliedTotal = turnTotal
        var endRemaining = startRemaining - turnTotal
        val requiresDoubleToFinish = updated.config.checkoutMode != X01CheckoutMode.SingleOut

        when {
            endRemaining < 0 -> {
                isBust = true
                appliedTotal = 0
                endRemaining = startRemaining
            }
            endRemaining == 1 && requiresDoubleToFinish -> {
                isBust = true
                appliedTotal = 0
                endRemaining = startRemaining
            }
            endRemaining == 0 -> {
                if (dartList.isEmpty() || isLegalFinishingDart(dartList.lastOrNull(), updated.config.checkoutMode)) {
                    didCheckout = true
                } else {
                    isBust = true
                    appliedTotal = 0
                    endRemaining = startRemaining
                }
            }
        }

        val updatedPlayers = updated.players.toMutableList()
        if (!isBust) {
            updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(remainingScore = endRemaining)
        }
        updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(checkedIn = checkedIn)
        updated = updated.copy(players = updatedPlayers)

        if (didCheckout) {
            val winnerLegs = updatedPlayers[playerIndex].legsWon + 1
            updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(legsWon = winnerLegs)
            updated = updated.copy(players = updatedPlayers, legIndex = updated.legIndex + 1)

            if (winnerLegs >= effectiveLegsToWin(updated.config)) {
                if (updated.config.setsEnabled) {
                    val winnerSets = updatedPlayers[playerIndex].setsWon + 1
                    updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(setsWon = winnerSets)
                    for (i in updatedPlayers.indices) {
                        updatedPlayers[i] = updatedPlayers[i].copy(legsWon = 0)
                    }
                    updated = updated.copy(
                        players = updatedPlayers,
                        setIndex = updated.setIndex + 1,
                    )
                    if (winnerSets >= effectiveSetsToWin(updated.config)) {
                        updated = updated.copy(
                            winnerPlayerId = updatedPlayers[playerIndex].playerId,
                            isComplete = true,
                        )
                    }
                } else {
                    updated = updated.copy(
                        winnerPlayerId = updatedPlayers[playerIndex].playerId,
                        isComplete = true,
                    )
                }
            }
            if (!updated.isComplete) {
                updated = resetLeg(updated)
            }
        }

        if (!updated.isComplete) {
            updated = updated.copy(currentPlayerIndex = (playerIndex + 1) % updated.players.size)
        }
        updated = updated.copy(turnIndex = updated.turnIndex + 1)

        val dartEvents = dartList.mapIndexed { offset, dart ->
            X01DartEvent(
                dartOrder = offset + 1,
                multiplierRaw = dart.multiplier.name.lowercase(),
                segmentRaw = segmentRaw(dart.segment),
                points = dart.points,
                wasMiss = dart.isMiss,
            )
        }

        val event = X01TurnEvent(
            id = UUID.randomUUID().toString(),
            playerId = state.players[playerIndex].playerId,
            turnIndex = state.turnIndex,
            legIndex = state.legIndex,
            setIndex = state.setIndex,
            startRemaining = startRemaining,
            enteredTotal = turnTotal,
            appliedTotal = appliedTotal,
            endRemaining = endRemaining,
            isBust = isBust,
            didCheckout = didCheckout,
            checkoutModeRaw = state.config.checkoutMode.name.lowercase(),
            checkoutDartCount = if (didCheckout) maxOf(1, dartList.size) else null,
            darts = dartEvents,
            timestampEpochMillis = timestamp.toEpochMilli(),
            dartsThrown = if (dartList.isEmpty()) 3 else dartList.size,
        )

        return X01TurnOutcome(updated, event)
    }

    fun replay(config: MatchConfigX01, playerIds: List<String>, events: List<X01TurnEvent>): X01State {
        var state = makeInitialState(config, playerIds)
        for (event in events) {
            state = submitTurn(
                state = state,
                enteredTotal = event.enteredTotal,
                darts = event.reconstructedDarts(),
                timestamp = Instant.ofEpochMilli(event.timestampEpochMillis),
            ).updatedState
        }
        return state
    }

    private fun resetLeg(state: X01State): X01State {
        val opensImmediately = state.config.checkInMode == X01CheckInMode.StraightIn
        val players = state.players.map { player ->
            player.copy(
                remainingScore = state.config.startScore,
                checkedIn = opensImmediately,
            )
        }
        return state.copy(players = players)
    }

    private fun effectiveLegsToWin(config: MatchConfigX01): Int = when (config.legFormat) {
        X01LegFormat.FirstTo -> config.legsToWin
        X01LegFormat.BestOf -> config.legsToWin / 2 + 1
    }

    private fun effectiveSetsToWin(config: MatchConfigX01): Int {
        val target = config.setsToWin ?: 1
        return when (config.legFormat) {
            X01LegFormat.FirstTo -> target
            X01LegFormat.BestOf -> target / 2 + 1
        }
    }

    fun scoreVisit(
        darts: List<DartInput>,
        checkInMode: X01CheckInMode,
        isCheckedIn: Boolean,
    ): X01VisitScore {
        var checkedIn = isCheckedIn
        var scored = 0
        val dartCounts = mutableListOf<Boolean>()
        for (dart in darts) {
            val counts = when {
                checkedIn -> true
                qualifiesForCheckIn(dart, checkInMode) -> {
                    checkedIn = true
                    true
                }
                else -> false
            }
            dartCounts.add(counts)
            if (counts) scored += dart.points
        }
        return X01VisitScore(scored, checkedIn, dartCounts)
    }

    private fun qualifiesForCheckIn(dart: DartInput, mode: X01CheckInMode): Boolean {
        if (dart.isMiss) return false
        return when (mode) {
            X01CheckInMode.StraightIn -> true
            X01CheckInMode.DoubleIn ->
                dart.multiplier == DartMultiplier.Double || dart.segment == DartSegment.InnerBull
            X01CheckInMode.MasterIn ->
                dart.multiplier == DartMultiplier.Double ||
                    dart.multiplier == DartMultiplier.Triple ||
                    dart.segment == DartSegment.InnerBull
        }
    }

    private fun isLegalFinishingDart(dart: DartInput?, mode: X01CheckoutMode): Boolean = when (mode) {
        X01CheckoutMode.SingleOut -> true
        X01CheckoutMode.DoubleOut -> dart != null &&
            (dart.multiplier == DartMultiplier.Double || dart.segment == DartSegment.InnerBull)
        X01CheckoutMode.MasterOut -> dart != null &&
            (dart.multiplier == DartMultiplier.Double ||
                dart.multiplier == DartMultiplier.Triple ||
                dart.segment == DartSegment.InnerBull)
    }
}
