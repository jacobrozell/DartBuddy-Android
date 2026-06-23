package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutSuggesterTest {
    @Test
    fun suggestsStandardMarqueeFinishes() {
        assertEquals(listOf("T20", "T20", "Bull"), CheckoutSuggester.suggestion(170, X01CheckoutMode.DoubleOut))
        assertEquals(listOf("T20", "T19", "Bull"), CheckoutSuggester.suggestion(167, X01CheckoutMode.DoubleOut))
        assertEquals(listOf("T20", "D20"), CheckoutSuggester.suggestion(100, X01CheckoutMode.DoubleOut))
        assertEquals(listOf("20", "D20"), CheckoutSuggester.suggestion(60, X01CheckoutMode.DoubleOut))
        assertEquals(listOf("D20"), CheckoutSuggester.suggestion(40, X01CheckoutMode.DoubleOut))
        assertEquals(listOf("Bull"), CheckoutSuggester.suggestion(50, X01CheckoutMode.DoubleOut))
    }

    @Test
    fun rejectsImpossibleScores() {
        listOf(169, 168, 166, 165, 163, 162, 159, 171, 200, 1).forEach { remaining ->
            assertNull(CheckoutSuggester.suggestion(remaining, X01CheckoutMode.DoubleOut))
        }
    }

    @Test
    fun allSuggestionsMatchesPrimary() {
        for (remaining in 2..170) {
            val primary = CheckoutSuggester.suggestion(remaining, X01CheckoutMode.DoubleOut)
            val all = CheckoutSuggester.allSuggestions(remaining, X01CheckoutMode.DoubleOut)
            if (primary != null) {
                assertEquals(primary, all.first())
                assertTrue(all.isNotEmpty())
            } else {
                assertTrue(all.isEmpty())
            }
        }
    }
}
