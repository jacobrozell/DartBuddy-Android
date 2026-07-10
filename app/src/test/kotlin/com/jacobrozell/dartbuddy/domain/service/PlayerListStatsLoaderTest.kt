package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PlayerListStatsLoaderTest {
    @Test
    fun build_countsGamesAndWinsPerParticipant() {
        val matches = listOf(
            match(
                id = "m1",
                winnerId = "p1",
                participants = listOf("p1", "p2"),
            ),
            match(
                id = "m2",
                winnerId = "p2",
                participants = listOf("p1", "p2"),
            ),
        )

        val summaries = PlayerListStatsLoader.build(matches)

        assertEquals(PlayerListSummary(games = 2, wins = 1), summaries["p1"])
        assertEquals(PlayerListSummary(games = 2, wins = 1), summaries["p2"])
    }

    @Test
    fun build_ignoresInProgressMatches() {
        val summaries = PlayerListStatsLoader.build(
            listOf(
                match(
                    id = "m1",
                    winnerId = "p1",
                    participants = listOf("p1"),
                    status = MatchStatus.InProgress,
                ),
            ),
        )

        assertEquals(emptyMap<String, PlayerListSummary>(), summaries)
    }

    private fun match(
        id: String,
        winnerId: String?,
        participants: List<String>,
        status: MatchStatus = MatchStatus.Completed,
    ) = MatchSummary(
        id = id,
        matchType = MatchType.X01,
        status = status,
        participantNames = participants,
        participantIds = participants,
        winnerPlayerId = winnerId,
        forfeitedByPlayerId = null,
        startedAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
