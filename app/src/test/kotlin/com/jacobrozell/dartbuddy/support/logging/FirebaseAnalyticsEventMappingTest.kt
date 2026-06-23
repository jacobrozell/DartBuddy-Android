package com.jacobrozell.dartbuddy.support.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class FirebaseAnalyticsEventMappingTest {
    @Test
    fun mapsAllowlistedEventToFirebaseName() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.APP_LIFECYCLE,
            eventName = "app_bootstrap_ready",
            message = "ready",
            metadata = mapOf("matchType" to "x01"),
        )
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion = "1.0.0")
        assertEquals("app_open", event?.name)
        assertEquals("1.0.0", event?.parameters?.get("app_version"))
        assertEquals("x01", event?.parameters?.get("matchType"))
    }

    @Test
    fun blocksPersonalMetadataKeys() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.SCORING,
            eventName = "match_started",
            message = "started",
            metadata = mapOf(
                "matchType" to "x01",
                "playerId" to "secret",
                "playerName" to "Alice",
            ),
        )
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion = null)
        assertEquals("match_started", event?.name)
        assertNull(event?.parameters?.get("playerId"))
        assertNull(event?.parameters?.get("playerName"))
        assertEquals("x01", event?.parameters?.get("matchType"))
    }

    @Test
    fun ignoresUnmappedEvents() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.DEBUG,
            category = LogCategory.UI,
            eventName = "debug_only_event",
            message = "noop",
            metadata = emptyMap(),
        )
        assertNull(FirebaseAnalyticsEventMapping.map(entry, appVersion = null))
    }
}

class BotAnalyticsTest {
    @Test
    fun trainingBotReportsKindWithoutDifficulty() {
        val metadata = BotAnalytics.metadata(
            listOf(
                com.jacobrozell.dartbuddy.data.repository.PlayerSummary(
                    id = "h1",
                    name = "Alice",
                    isArchived = false,
                ),
                com.jacobrozell.dartbuddy.data.repository.PlayerSummary(
                    id = "b1",
                    name = "Partner",
                    isArchived = false,
                    isBot = true,
                    botKindRaw = "training",
                ),
            ),
        )
        assertEquals("true", metadata["hasBot"])
        assertEquals("training", metadata["botKind"])
        assertTrue(!metadata.containsKey("botDifficulty"))
    }
}
