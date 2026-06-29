package com.jacobrozell.dartbuddy.ui.activity

import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ActivityFiltersTest {
    @Test
    fun periodFilterExcludesOlderMatches() {
        // Use wall-clock "now" so cutoff() and match timestamps stay aligned (fixed dates drift after 7 days).
        val now = Instant.now()
        val recent = sampleMatch(id = "1", startedAt = now.minusSeconds(3600))
        val old = sampleMatch(id = "2", startedAt = now.minusSeconds(10 * 24 * 3600))
        val filtered = ActivityFilters.filterMatches(
            matches = listOf(recent, old),
            mode = ActivityFilter.All,
            period = ActivityPeriod.Days7,
            playerId = null,
        )
        assertEquals(listOf("1"), filtered.map { it.id })
    }

    @Test
    fun playerFilterKeepsMatchesWithParticipant() {
        val match = sampleMatch(id = "1", participantIds = listOf("a", "b"))
        val filtered = ActivityFilters.filterMatches(
            matches = listOf(match),
            mode = ActivityFilter.All,
            period = ActivityPeriod.All,
            playerId = "b",
        )
        assertEquals(1, filtered.size)
    }

    private fun sampleMatch(
        id: String,
        startedAt: Instant = Instant.now(),
        participantIds: List<String> = listOf("a"),
    ) = MatchSummary(
        id = id,
        matchType = MatchType.X01,
        status = MatchStatus.Completed,
        participantNames = participantIds,
        participantIds = participantIds,
        winnerPlayerId = participantIds.first(),
        forfeitedByPlayerId = null,
        startedAt = startedAt,
        updatedAt = startedAt,
    )
}
