package com.jacobrozell.dartbuddy.ui.layout

import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayLayoutTest {
    private fun ctx(
        smallest: Int,
        width: Int,
        height: Int,
        fontScale: Float = 1f,
        playerCount: Int = 0,
    ) = LayoutContext(smallest, width, height, fontScale, playerCount)

    @Test
    fun sideBySide_trueForPhoneLandscape() {
        assertTrue(usesSideBySideMatchScoringLayout(ctx(smallest = 412, width = 915, height = 400)))
    }

    @Test
    fun sideBySide_falseForTabletPortrait() {
        assertFalse(usesSideBySideMatchScoringLayout(ctx(smallest = 720, width = 800, height = 1200)))
    }

    @Test
    fun sideBySide_falseForPhonePortrait() {
        assertFalse(usesSideBySideMatchScoringLayout(ctx(smallest = 390, width = 390, height = 844)))
    }

    @Test
    fun sideBySide_falseForAccessibilityFont() {
        assertFalse(
            usesSideBySideMatchScoringLayout(
                ctx(smallest = 412, width = 915, height = 400, fontScale = 1.5f),
            ),
        )
    }

    @Test
    fun wideScoreCards_tabletLandscapeOnly() {
        assertTrue(usesWidePlayerScoreCardLayout(ctx(smallest = 720, width = 1280, height = 800)))
        assertFalse(usesWidePlayerScoreCardLayout(ctx(smallest = 412, width = 915, height = 400)))
        assertFalse(usesWidePlayerScoreCardLayout(ctx(smallest = 720, width = 700, height = 1100)))
    }

    @Test
    fun summaryGrid_twoPlayersPortraitPhone() {
        assertTrue(usesMatchSummarySideBySidePlayerGrid(ctx(390, 390, 844), playerCount = 2))
        assertFalse(usesMatchSummarySideBySidePlayerGrid(ctx(390, 390, 844), playerCount = 3))
    }

    @Test
    fun summaryGrid_tabletAlwaysWhenTwoPlus() {
        assertTrue(usesMatchSummarySideBySidePlayerGrid(ctx(720, 800, 1200), playerCount = 3))
    }

    @Test
    fun summaryLandscapeSplit_phoneLandscapeOnly() {
        assertTrue(usesLandscapeMatchSummaryLayout(ctx(412, 915, 400)))
        assertFalse(usesLandscapeMatchSummaryLayout(ctx(720, 1280, 800)))
    }

    @Test
    fun contentMaxWidth_usesSmallestWidth() {
        assertEquals(920, contentMaxWidth(800).value.toInt())
        assertEquals(Dp.Unspecified, contentMaxWidth(400))
    }

    @Test
    fun scoringPadColumnCount_reducesForAccessibilityAndXs() {
        assertEquals(4, scoringPadColumnCount(ctx(320, 320, 640, fontScale = 1f)))
        assertEquals(4, scoringPadColumnCount(ctx(400, 400, 800, fontScale = 1.4f)))
        assertEquals(7, scoringPadColumnCount(ctx(400, 400, 800)))
    }

    @Test
    fun transposedCricket_phoneLandscapeOnly() {
        assertTrue(usesTransposedCricketBoardLayout(ctx(412, 915, 400)))
        assertFalse(usesTransposedCricketBoardLayout(ctx(720, 800, 1200)))
        assertFalse(usesTransposedCricketBoardLayout(ctx(412, 915, 400, fontScale = 1.5f)))
    }

    @Test
    fun pinnedActiveCard_threePlusPlayersOrLandscapeOrAx() {
        assertTrue(usesPinnedActiveX01PlayerCard(ctx(400, 400, 800, playerCount = 3)))
        assertTrue(usesPinnedActiveX01PlayerCard(ctx(412, 915, 400, playerCount = 2)))
        assertFalse(usesPinnedActiveX01PlayerCard(ctx(400, 400, 800, playerCount = 2)))
        assertTrue(usesPinnedActiveX01PlayerCard(ctx(400, 400, 800, fontScale = 1.5f, playerCount = 2)))
    }

    @Test
    fun wideSetup_tabletOnly() {
        assertTrue(usesWideSetupHomeLayout(ctx(720, 800, 1200)))
        assertFalse(usesWideSetupHomeLayout(ctx(412, 915, 400)))
    }

    @Test
    fun iPadPortraitTwoPlayerRow() {
        assertTrue(usesIPadPortraitTwoPlayerScoreRow(ctx(720, 800, 1200), playerCount = 2))
        assertFalse(usesIPadPortraitTwoPlayerScoreRow(ctx(720, 800, 1200), playerCount = 3))
    }
}
