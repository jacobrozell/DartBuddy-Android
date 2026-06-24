package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.engine.CricketTarget
import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import kotlin.math.max
import kotlin.math.min

object StatsService {
    fun x01Average3Dart(totalPointsScored: Int, totalDartsThrown: Int): Double {
        if (totalDartsThrown <= 0) return 0.0
        return (totalPointsScored.toDouble() / totalDartsThrown) * 3
    }

    /** Every recorded cricket touch, including misses. */
    fun cricketDartsThrown(events: List<CricketTurnEvent>): Int =
        events.sumOf { turn -> turn.targetsTouched.size }

    /**
     * Live X01 scorecard average while a visit may still be in progress.
     * For the opening visit's first two darts, show per-dart average (e.g. 11 → 11.00, not 33.00).
     */
    fun x01LiveScorecardAverage(
        committedPoints: Int,
        committedDarts: Int,
        previewPoints: Int,
        previewDarts: Int,
    ): Double {
        val totalPoints = committedPoints + previewPoints
        val totalDarts = committedDarts + previewDarts
        if (totalDarts <= 0) return 0.0
        if (committedDarts == 0 && previewDarts in 1..2) {
            return previewPoints.toDouble() / previewDarts
        }
        return x01Average3Dart(totalPoints, totalDarts)
    }

    fun x01CommittedDarts(events: List<X01TurnEvent>): Int =
        events.sumOf { it.effectiveDartsThrown }

    fun x01CommittedPoints(events: List<X01TurnEvent>): Int =
        events.sumOf { it.appliedTotal }

    fun cricketCommittedMarks(events: List<CricketTurnEvent>): Int =
        events.sumOf { turn -> turn.targetsTouched.sumOf { it.marksAdded } }

    fun cricketPreviewMarks(darts: List<DartInput>, priorMarks: Map<String, Int>): Int {
        val preview = priorMarks.toMutableMap()
        var added = 0
        darts.forEach { dart ->
            val target = cricketTargetForDart(dart) ?: return@forEach
            val prior = preview[target.raw] ?: 0
            val incoming = if (dart.isMiss) 0 else marksForDart(dart)
            val next = min(3, prior + incoming)
            added += max(0, next - prior)
            preview[target.raw] = next
        }
        return added
    }

    fun cricketMarksPerRound(
        events: List<CricketTurnEvent>,
        previewMarks: Int,
        includesPreviewRound: Boolean,
    ): Double {
        val marks = cricketCommittedMarks(events) + previewMarks
        val rounds = events.size + if (includesPreviewRound) 1 else 0
        if (rounds <= 0) return 0.0
        return marks.toDouble() / rounds
    }

    private fun cricketTargetForDart(dart: DartInput): CricketTarget? = when (val segment = dart.segment) {
        DartSegment.InnerBull, DartSegment.OuterBull -> CricketTarget.Bull
        is DartSegment.OneToTwenty -> CricketTarget.fromRaw(segment.value.toString())
        else -> null
    }

    private fun marksForDart(dart: DartInput): Int = when (dart.segment) {
        DartSegment.InnerBull -> 2
        DartSegment.OuterBull -> 1
        is DartSegment.OneToTwenty -> dart.multiplier.markValue
        DartSegment.Miss -> 0
    }
}
