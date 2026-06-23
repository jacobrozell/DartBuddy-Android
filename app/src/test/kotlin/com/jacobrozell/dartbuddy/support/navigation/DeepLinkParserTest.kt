package com.jacobrozell.dartbuddy.support.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkParserTest {
    @Test
    fun parsesPlayResume() {
        val destination = DeepLinkParser.parseSegments(listOf("v1", "play", "resume"))
        assertTrue(destination is DeepLinkDestination.PlayResume)
    }

    @Test
    fun parsesPlayHome() {
        val destination = DeepLinkParser.parseSegments(listOf("v1", "play"))
        assertTrue(destination is DeepLinkDestination.PlayHome)
    }

    @Test
    fun parsesPlaySetup() {
        val destination = DeepLinkParser.parseSegments(listOf("v1", "play", "setup"))
        assertTrue(destination is DeepLinkDestination.PlaySetup)
    }

    @Test
    fun parsesPlayMatch() {
        val destination = DeepLinkParser.parseSegments(listOf("v1", "play", "match", "abc-123"))
        assertEquals(DeepLinkDestination.PlayMatch("abc-123"), destination)
    }

    @Test
    fun parsesHistoryMatch() {
        val destination = DeepLinkParser.parseSegments(listOf("v1", "activity", "history", "match", "abc-123"))
        assertEquals(DeepLinkDestination.HistoryMatch("abc-123"), destination)
    }

    @Test
    fun parsesPlayerDetail() {
        val destination = DeepLinkParser.parseSegments(listOf("v1", "players", "player-1"))
        assertEquals(DeepLinkDestination.PlayerDetail("player-1"), destination)
    }

    @Test
    fun parsesTabRoutes() {
        assertEquals(
            DeepLinkDestination.Tab("players"),
            DeepLinkParser.parseSegments(listOf("v1", "tab", "players")),
        )
        assertEquals(
            DeepLinkDestination.Tab("activity"),
            DeepLinkParser.parseSegments(listOf("v1", "activity")),
        )
    }

    @Test
    fun pathFromRoundTrips() {
        assertEquals("play/setup", DeepLinkParser.pathFrom(DeepLinkDestination.PlaySetup))
        assertEquals("play/match/id-1", DeepLinkParser.pathFrom(DeepLinkDestination.PlayMatch("id-1")))
        assertEquals(
            "activity/history/match/id-1",
            DeepLinkParser.pathFrom(DeepLinkDestination.HistoryMatch("id-1")),
        )
        assertEquals("players/id-1", DeepLinkParser.pathFrom(DeepLinkDestination.PlayerDetail("id-1")))
    }

    @Test
    fun rejectsUnknownRoutes() {
        assertNull(DeepLinkParser.parseSegments(listOf("v1", "journey")))
    }
}
