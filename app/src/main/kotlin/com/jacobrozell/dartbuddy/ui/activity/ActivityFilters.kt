package com.jacobrozell.dartbuddy.ui.activity

import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.support.HistoryListRow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ActivityPeriod {
    Today,
    Days7,
    Days30,
    All,
    ;

    fun cutoff(now: Instant = Instant.now()): Instant? = when (this) {
        All -> null
        Today -> {
            val zone = ZoneId.systemDefault()
            now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        }
        Days7 -> now.minus(7, ChronoUnit.DAYS)
        Days30 -> now.minus(30, ChronoUnit.DAYS)
    }
}

object ActivityFilters {
    fun filterMatches(
        matches: List<MatchSummary>,
        mode: ActivityFilter,
        period: ActivityPeriod,
        playerId: String?,
    ): List<MatchSummary> {
        val cutoff = period.cutoff()
        return matches.filter { match ->
            val modeOk = when (mode) {
                ActivityFilter.All -> true
                ActivityFilter.X01 -> match.matchType == com.jacobrozell.dartbuddy.domain.model.MatchType.X01
                ActivityFilter.Cricket -> match.matchType == com.jacobrozell.dartbuddy.domain.model.MatchType.Cricket
            }
            val periodOk = cutoff == null || !match.startedAt.isBefore(cutoff)
            val playerOk = playerId == null || match.participantIds.contains(playerId)
            modeOk && periodOk && playerOk
        }
    }

    fun filterHistoryRows(
        rows: List<HistoryListRow>,
        mode: ActivityFilter,
        period: ActivityPeriod,
        playerId: String?,
    ): List<HistoryListRow> {
        val cutoff = period.cutoff()
        return rows.filter { row ->
            val modeOk = when (mode) {
                ActivityFilter.All -> true
                ActivityFilter.X01 -> row.matchType == com.jacobrozell.dartbuddy.domain.model.MatchType.X01
                ActivityFilter.Cricket -> row.matchType == com.jacobrozell.dartbuddy.domain.model.MatchType.Cricket
            }
            val instant = Instant.ofEpochMilli(row.updatedAtEpochMillis)
            val periodOk = cutoff == null || !instant.isBefore(cutoff)
            val playerOk = playerId == null || row.standings.any { it.playerId == playerId }
            modeOk && periodOk && playerOk
        }
    }

    fun matchesFilter(
        match: MatchSummary,
        mode: ActivityFilter,
        period: ActivityPeriod,
        playerId: String?,
    ): Boolean = filterMatches(listOf(match), mode, period, playerId).isNotEmpty()
}
