package com.jacobrozell.dartbuddy.domain.scoring

import com.jacobrozell.dartbuddy.R

object DartAccessibility {
    fun spokenName(dart: DartInput, l10n: (Int, Array<Any>) -> String, l10nRaw: (Int) -> String): String {
        if (dart.isMiss) return l10nRaw(R.string.scoring_segment_miss_accessibility)
        return when (val segment = dart.segment) {
            DartSegment.InnerBull -> l10nRaw(R.string.scoring_segment_innerBull_accessibility)
            DartSegment.OuterBull -> l10nRaw(R.string.scoring_segment_outerBull_accessibility)
            DartSegment.Miss -> l10nRaw(R.string.scoring_segment_miss_accessibility)
            is DartSegment.OneToTwenty -> when (dart.multiplier) {
                DartMultiplier.Single -> l10n(R.string.scoring_dart_single_accessibility, arrayOf(segment.value))
                DartMultiplier.Double -> l10n(R.string.scoring_dart_double_accessibility, arrayOf(segment.value))
                DartMultiplier.Triple -> l10n(R.string.scoring_dart_triple_accessibility, arrayOf(segment.value))
            }
        }
    }

    fun padKeyLabel(segmentValue: Int, armedMultiplier: DartMultiplier, l10n: (Int, Array<Any>) -> String, l10nRaw: (Int) -> String): String {
        if (segmentValue == 0) return l10nRaw(R.string.scoring_segment_miss_accessibility)
        if (segmentValue == 25) {
            if (armedMultiplier == DartMultiplier.Double) {
                return l10nRaw(R.string.scoring_dart_doubleBull_accessibility)
            }
            return l10nRaw(R.string.scoring_segment_outerBull_accessibility)
        }
        return when (armedMultiplier) {
            DartMultiplier.Single -> l10n(R.string.scoring_dart_single_accessibility, arrayOf(segmentValue))
            DartMultiplier.Double -> l10n(R.string.scoring_dart_double_accessibility, arrayOf(segmentValue))
            DartMultiplier.Triple -> l10n(R.string.scoring_dart_triple_accessibility, arrayOf(segmentValue))
        }
    }

    fun checkoutDisplayLabel(token: String, l10n: (Int, Array<Any>) -> String, l10nRaw: (Int) -> String): String {
        if (token == "Bull") return l10nRaw(R.string.scoring_checkout_bull)
        if (token.startsWith("T")) {
            token.drop(1).toIntOrNull()?.let { return l10n(R.string.scoring_checkout_tripleFormat, arrayOf(it)) }
        }
        if (token.startsWith("D")) {
            token.drop(1).toIntOrNull()?.let { return l10n(R.string.scoring_checkout_doubleFormat, arrayOf(it)) }
        }
        return token
    }
}
