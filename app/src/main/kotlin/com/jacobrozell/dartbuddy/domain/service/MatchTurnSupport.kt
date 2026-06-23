package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.error
import com.jacobrozell.dartbuddy.support.logging.info

object MatchTurnSupport {
    fun progressMetadata(
        matchType: MatchType,
        eventCount: Int,
        legIndex: Int,
        setIndex: Int,
        status: String,
    ): Map<String, String> = mapOf(
        "matchType" to matchType.name.lowercase(),
        "eventCount" to eventCount.toString(),
        "legIndex" to legIndex.toString(),
        "setIndex" to setIndex.toString(),
        "status" to status,
    )

    fun logTurnSubmitted(
        logger: AppLogger,
        matchId: String,
        session: X01Session,
    ) {
        logger.info(
            LogCategory.SCORING,
            eventName = "turn_submitted",
            message = "Turn accepted and persisted.",
            metadata = progressMetadata(
                matchType = MatchType.X01,
                eventCount = session.events.size,
                legIndex = session.state.legIndex,
                setIndex = session.state.setIndex,
                status = session.entity.status,
            ),
            correlationId = matchId,
        )
    }

    fun logTurnSubmitted(
        logger: AppLogger,
        matchId: String,
        session: CricketSession,
    ) {
        logger.info(
            LogCategory.SCORING,
            eventName = "turn_submitted",
            message = "Turn accepted and persisted.",
            metadata = progressMetadata(
                matchType = MatchType.Cricket,
                eventCount = session.events.size,
                legIndex = session.state.legIndex,
                setIndex = session.state.setIndex,
                status = session.entity.status,
            ),
            correlationId = matchId,
        )
    }

    fun logTurnUndone(logger: AppLogger, matchId: String, matchType: MatchType, metadata: Map<String, String>) {
        logger.info(
            LogCategory.SCORING,
            eventName = "turn_undone",
            message = "Last turn undone.",
            metadata = metadata,
            correlationId = matchId,
        )
    }

    fun logDartUndone(logger: AppLogger, matchId: String, matchType: MatchType, metadata: Map<String, String>) {
        logger.info(
            LogCategory.SCORING,
            eventName = "dart_undone",
            message = "In-progress throw undone.",
            metadata = metadata,
            correlationId = matchId,
        )
    }

    fun logTurnPersistFailed(logger: AppLogger, matchId: String, matchType: MatchType, message: String) {
        logger.error(
            LogCategory.PERSISTENCE,
            eventName = "turn_persist_failed",
            message = message,
            metadata = mapOf("matchType" to matchType.name.lowercase()),
            correlationId = matchId,
        )
    }

    fun logTurnUndoFailed(logger: AppLogger, matchId: String, matchType: MatchType, message: String) {
        logger.error(
            LogCategory.SCORING,
            eventName = "turn_undo_failed",
            message = message,
            metadata = mapOf("matchType" to matchType.name.lowercase()),
            correlationId = matchId,
        )
    }
}
