package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import com.jacobrozell.dartbuddy.domain.engine.X01DartEvent
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.support.L10n

data class PlayerMatchStats(
    val playerId: String,
    val name: String,
    val darts: Int,
    val points: Int,
    val highestScore: Int,
    val highestCheckout: Int,
    val checkouts: Int,
    val legsWon: Int,
    val setsWon: Int,
    val score: Int,
    val marksPerRound: Double,
    val doubles: Int,
    val triples: Int,
    val hitsBySector: Map<String, Int> = emptyMap(),
) {
    val average3Dart: Double
        get() = if (darts > 0) (points.toDouble() / darts) * 3 else 0.0

    val doublePercent: Double
        get() = if (darts > 0) doubles.toDouble() / darts * 100 else 0.0

    val triplePercent: Double
        get() = if (darts > 0) triples.toDouble() / darts * 100 else 0.0
}

object HitsBySectorKeys {
    const val MISS = "0"

    fun key(segmentRaw: String, wasMiss: Boolean): String =
        if (wasMiss || segmentRaw == "miss") MISS else segmentRaw
}

object MatchStatsCalculator {
    fun x01Stats(
        participantIds: List<String>,
        names: List<String>,
        events: List<X01TurnEvent>,
        finalLegsWon: Map<String, Int>,
        finalSetsWon: Map<String, Int>,
        winnerId: String?,
    ): List<PlayerMatchStats> {
        val buckets = mutableMapOf<String, MutableBucket>()
        participantIds.forEachIndexed { index, id ->
            buckets[id] = MutableBucket(id, names.getOrElse(index) { "Player ${index + 1}" })
        }

        events.forEach { turn ->
            val bucket = buckets[turn.playerId] ?: return@forEach
            bucket.darts += turn.effectiveDartsThrown
            bucket.points += turn.appliedTotal
            if (turn.appliedTotal > bucket.highestScore) bucket.highestScore = turn.appliedTotal
            if (turn.didCheckout) {
                bucket.checkouts += 1
                if (turn.startRemaining > bucket.highestCheckout) bucket.highestCheckout = turn.startRemaining
            }
            turn.darts.forEach { dart ->
                recordSectorHit(bucket, dart.segmentRaw, dart.wasMiss)
                recordMultiplier(bucket, dart.multiplierRaw, dart.wasMiss)
            }
        }

        return buckets.values.map { bucket ->
            PlayerMatchStats(
                playerId = bucket.playerId,
                name = bucket.name,
                darts = bucket.darts,
                points = bucket.points,
                highestScore = bucket.highestScore,
                highestCheckout = bucket.highestCheckout,
                checkouts = bucket.checkouts,
                legsWon = finalLegsWon[bucket.playerId] ?: 0,
                setsWon = finalSetsWon[bucket.playerId] ?: 0,
                score = 0,
                marksPerRound = 0.0,
                doubles = bucket.doubles,
                triples = bucket.triples,
                hitsBySector = bucket.hitsBySector.toMap(),
            )
        }.sortedWith(
            compareByDescending<PlayerMatchStats> { it.playerId == winnerId }
                .thenByDescending { it.average3Dart },
        )
    }

    fun cricketStats(
        participantIds: List<String>,
        names: List<String>,
        events: List<CricketTurnEvent>,
        finalScores: Map<String, Int>,
        finalLegsWon: Map<String, Int>,
        finalSetsWon: Map<String, Int>,
        winnerId: String?,
    ): List<PlayerMatchStats> {
        val buckets = mutableMapOf<String, MutableBucket>()
        participantIds.forEachIndexed { index, id ->
            buckets[id] = MutableBucket(id, names.getOrElse(index) { "Player ${index + 1}" })
        }

        events.forEach { turn ->
            val bucket = buckets[turn.playerId] ?: return@forEach
            bucket.points += turn.totalPointsAdded
            turn.targetsTouched.forEach { touch ->
                bucket.darts += 1
                val sector = touch.segmentRaw ?: touch.targetRaw
                recordSectorHit(bucket, sector, touch.wasMiss)
                if (!touch.wasMiss) {
                    recordMultiplier(bucket, touch.multiplierRaw, false)
                }
            }
        }

        return buckets.values.map { bucket ->
            val playerEvents = events.filter { it.playerId == bucket.playerId }
            val marks = playerEvents.sumOf { turn ->
                turn.targetsTouched.sumOf { it.marksAdded + it.overflowMarks }
            }
            val rounds = playerEvents.map { it.roundIndex }.distinct().size.coerceAtLeast(1)
            PlayerMatchStats(
                playerId = bucket.playerId,
                name = bucket.name,
                darts = bucket.darts,
                points = bucket.points,
                highestScore = 0,
                highestCheckout = 0,
                checkouts = 0,
                legsWon = finalLegsWon[bucket.playerId] ?: 0,
                setsWon = finalSetsWon[bucket.playerId] ?: 0,
                score = finalScores[bucket.playerId] ?: 0,
                marksPerRound = marks.toDouble() / rounds,
                doubles = bucket.doubles,
                triples = bucket.triples,
                hitsBySector = bucket.hitsBySector.toMap(),
            )
        }.sortedWith(
            compareByDescending<PlayerMatchStats> { it.playerId == winnerId }
                .thenByDescending { it.score },
        )
    }

    fun x01Timeline(events: List<X01TurnEvent>, names: Map<String, String>, l10n: L10n): List<String> =
        events.sortedBy { it.turnIndex }.map { turn ->
            val name = names[turn.playerId] ?: turn.playerId.take(6)
            l10n.format(R.string.history_timeline_x01_turn, turn.turnIndex + 1, name, turn.appliedTotal)
        }

    fun cricketTimeline(events: List<CricketTurnEvent>, names: Map<String, String>, l10n: L10n): List<String> =
        events.sortedBy { it.turnIndex }.map { turn ->
            val name = names[turn.playerId] ?: turn.playerId.take(6)
            l10n.format(R.string.history_timeline_cricket_turn, turn.turnIndex + 1, name, turn.totalPointsAdded)
        }

    fun mergedSectorHits(rows: List<PlayerMatchStats>): Map<String, Int> {
        val merged = mutableMapOf<String, Int>()
        rows.forEach { row ->
            row.hitsBySector.forEach { (sector, count) ->
                merged[sector] = (merged[sector] ?: 0) + count
            }
        }
        return merged
    }

    fun modeLabel(type: MatchType): String = when (type) {
        MatchType.X01 -> "X01"
        MatchType.Cricket -> "Cricket"
    }

    private class MutableBucket(val playerId: String, val name: String) {
        var darts: Int = 0
        var points: Int = 0
        var highestScore: Int = 0
        var highestCheckout: Int = 0
        var checkouts: Int = 0
        var doubles: Int = 0
        var triples: Int = 0
        val hitsBySector = mutableMapOf<String, Int>()
    }

    private fun recordSectorHit(bucket: MutableBucket, segmentRaw: String, wasMiss: Boolean) {
        val key = HitsBySectorKeys.key(segmentRaw, wasMiss)
        bucket.hitsBySector[key] = (bucket.hitsBySector[key] ?: 0) + 1
    }

    private fun recordMultiplier(bucket: MutableBucket, multiplierRaw: String, wasMiss: Boolean) {
        if (wasMiss) return
        when (multiplierRaw.lowercase()) {
            DartMultiplier.Double.name.lowercase() -> bucket.doubles += 1
            DartMultiplier.Triple.name.lowercase() -> bucket.triples += 1
        }
    }
}
