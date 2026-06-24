package com.jacobrozell.dartbuddy.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchVisitPreviewTest {
    @Test
    fun includesActiveVisit_whenHumanCanInput() {
        assertTrue(
            MatchVisitPreview.includesActiveVisit(
                isActive = true,
                canHumanInput = true,
                isBotPlaying = false,
                isCurrentPlayerBot = false,
            ),
        )
    }

    @Test
    fun includesActiveVisit_whenBotIsPlaying() {
        assertTrue(
            MatchVisitPreview.includesActiveVisit(
                isActive = true,
                canHumanInput = false,
                isBotPlaying = true,
                isCurrentPlayerBot = true,
            ),
        )
    }

    @Test
    fun excludesInactivePlayer() {
        assertFalse(
            MatchVisitPreview.includesActiveVisit(
                isActive = false,
                canHumanInput = true,
                isBotPlaying = false,
                isCurrentPlayerBot = false,
            ),
        )
    }
}
