package com.jacobrozell.dartbuddy.domain.scoring

import kotlinx.serialization.Serializable

@Serializable
enum class DartMultiplier {
    Single,
    Double,
    Triple,
    ;

    val markValue: Int
        get() = when (this) {
            Single -> 1
            Double -> 2
            Triple -> 3
        }
}

@Serializable
sealed class DartSegment {
    @Serializable
    data class OneToTwenty(val value: Int) : DartSegment()

    @Serializable
    data object OuterBull : DartSegment()

    @Serializable
    data object InnerBull : DartSegment()

    @Serializable
    data object Miss : DartSegment()

    val baseValue: Int
        get() = when (this) {
            is OneToTwenty -> value
            OuterBull -> 25
            InnerBull -> 25
            Miss -> 0
        }

    val cricketTargetRaw: String?
        get() = when (this) {
            is OneToTwenty -> if (value in 15..20) value.toString() else null
            OuterBull, InnerBull -> "bull"
            Miss -> null
        }
}

@Serializable
data class DartInput(
    val multiplier: DartMultiplier,
    val segment: DartSegment,
    val isMiss: Boolean = false,
) {
    val points: Int
        get() {
            if (isMiss) return 0
            return when (segment) {
                DartSegment.InnerBull -> 50
                DartSegment.OuterBull -> 25
                DartSegment.Miss -> 0
                is DartSegment.OneToTwenty -> segment.baseValue * multiplier.markValue
            }
        }

    val compactDisplayLabel: String
        get() {
            if (isMiss) return "0"
            return when (segment) {
                is DartSegment.OneToTwenty -> when (multiplier) {
                    DartMultiplier.Single -> "${segment.value}"
                    DartMultiplier.Double -> "D${segment.value}"
                    DartMultiplier.Triple -> "T${segment.value}"
                }
                DartSegment.OuterBull -> "25"
                DartSegment.InnerBull -> "50"
                DartSegment.Miss -> "0"
            }
        }
}

fun parseSegmentRaw(raw: String): DartSegment {
    raw.toIntOrNull()?.let { value ->
        if (value in 1..20) return DartSegment.OneToTwenty(value)
    }
    return when (raw) {
        "outerBull" -> DartSegment.OuterBull
        "innerBull" -> DartSegment.InnerBull
        else -> DartSegment.Miss
    }
}

fun segmentRaw(segment: DartSegment): String = when (segment) {
    is DartSegment.OneToTwenty -> segment.value.toString()
    DartSegment.OuterBull -> "outerBull"
    DartSegment.InnerBull -> "innerBull"
    DartSegment.Miss -> "miss"
}
