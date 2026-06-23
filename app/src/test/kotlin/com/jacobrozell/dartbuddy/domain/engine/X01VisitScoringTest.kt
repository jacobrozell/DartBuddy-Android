package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class X01VisitScoringTest {
    private fun single(value: Int) = DartInput(DartMultiplier.Single, DartSegment.OneToTwenty(value))
    private fun dbl(value: Int) = DartInput(DartMultiplier.Double, DartSegment.OneToTwenty(value))

    @Test
    fun doubleIn_ignoresSinglesUntilOpeningDouble() {
        val darts = listOf(single(20), single(20), dbl(20))
        val scored = X01Engine.scoreVisit(darts, X01CheckInMode.DoubleIn, isCheckedIn = false)

        assertEquals(40, scored.appliedTotal)
        assertTrue(scored.checkedInAfter)
        assertEquals(listOf(false, false, true), scored.dartCounts)
    }

    @Test
    fun doubleIn_innerBullOpensScoring() {
        val darts = listOf(DartInput(DartMultiplier.Single, DartSegment.InnerBull))
        val scored = X01Engine.scoreVisit(darts, X01CheckInMode.DoubleIn, isCheckedIn = false)

        assertEquals(50, scored.appliedTotal)
        assertTrue(scored.checkedInAfter)
        assertEquals(listOf(true), scored.dartCounts)
    }

    @Test
    fun masterIn_requiresDoubleOrTriple() {
        val darts = listOf(single(20), DartInput(DartMultiplier.Triple, DartSegment.OneToTwenty(20)))
        val scored = X01Engine.scoreVisit(darts, X01CheckInMode.MasterIn, isCheckedIn = false)

        assertEquals(60, scored.appliedTotal)
        assertTrue(scored.checkedInAfter)
        assertEquals(listOf(false, true), scored.dartCounts)
    }

    @Test
    fun straightIn_countsEveryDart() {
        val darts = listOf(single(20), single(5))
        val scored = X01Engine.scoreVisit(darts, X01CheckInMode.StraightIn, isCheckedIn = false)

        assertEquals(25, scored.appliedTotal)
        assertTrue(scored.checkedInAfter)
        assertEquals(listOf(true, true), scored.dartCounts)
    }

    @Test
    fun alreadyCheckedIn_countsAllDarts() {
        val darts = listOf(single(20), single(20))
        val scored = X01Engine.scoreVisit(darts, X01CheckInMode.DoubleIn, isCheckedIn = true)

        assertEquals(40, scored.appliedTotal)
        assertTrue(scored.checkedInAfter)
        assertFalse(scored.dartCounts.any { !it })
    }
}
