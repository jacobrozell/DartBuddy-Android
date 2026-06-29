package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.CricketSession
import com.jacobrozell.dartbuddy.domain.service.X01Session
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import kotlinx.serialization.json.Json

object MatchLifecycleAnalytics {
    fun logForfeited(
        logger: AppLogger,
        firebaseBootstrap: FirebaseBootstrap,
        matchType: MatchType,
        matchId: String,
        metadata: Map<String, String>,
    ) {
        logger.info(
            LogCategory.APP_LIFECYCLE,
            eventName = "match_forfeited",
            message = "Match forfeited by user.",
            metadata = metadata,
            correlationId = matchId,
        )
        logger.info(
            LogCategory.APP_LIFECYCLE,
            eventName = GameModeAnalytics.FORFEITED_EVENT_NAME,
            message = "User forfeited a game mode match.",
            metadata = metadata,
            correlationId = matchId,
        )
        AnalyticsUserIdentity.syncLastGameMode(firebaseBootstrap, matchType)
    }

    fun logForfeitFailed(
        logger: AppLogger,
        matchType: MatchType,
        matchId: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        logger.error(
            LogCategory.APP_LIFECYCLE,
            eventName = "match_forfeit_failed",
            message = message,
            metadata = metadata + mapOf("matchType" to matchType.name.lowercase()),
            correlationId = matchId,
        )
    }

    fun logAbandoned(
        logger: AppLogger,
        matchType: MatchType,
        matchId: String,
        metadata: Map<String, String>,
    ) {
        logger.info(
            LogCategory.APP_LIFECYCLE,
            eventName = "match_abandoned",
            message = "Match abandoned by user.",
            metadata = metadata,
            correlationId = matchId,
        )
    }

    fun logAbandonFailed(
        logger: AppLogger,
        matchType: MatchType,
        matchId: String,
        eventName: String,
        message: String,
    ) {
        logger.error(
            LogCategory.APP_LIFECYCLE,
            eventName = eventName,
            message = message,
            metadata = mapOf("matchType" to matchType.name.lowercase()),
            correlationId = matchId,
        )
    }

    fun forfeitMetadata(
        matchType: MatchType,
        config: MatchConfigPayload,
        participantCount: Int,
        players: List<PlayerSummary>,
        resolution: String,
        durationSeconds: Int,
        startSource: String? = null,
    ): Map<String, String> {
        val metadata = MatchAnalytics.metadata(
            matchType = matchType,
            config = config,
            participantCount = participantCount,
            players = players,
            startSource = startSource,
        ).toMutableMap()
        metadata["durationSeconds"] = durationSeconds.toString()
        metadata["resolution"] = resolution
        return metadata
    }

    fun metadataFromEntity(
        entity: MatchEntity,
        json: Json,
        players: List<PlayerSummary>,
        startSource: String? = null,
    ): Map<String, String>? {
        val matchType = runCatching { MatchType.valueOf(entity.matchType) }.getOrNull() ?: return null
        val config = runCatching { json.decodeFromString<MatchConfigPayload>(entity.configJson) }.getOrNull() ?: return null
        val participantIds = runCatching {
            json.decodeFromString<List<String>>(entity.participantIdsJson)
        }.getOrDefault(emptyList())
        val eventCount = runCatching {
            json.decodeFromString<List<*>>(entity.eventsJson).size
        }.getOrDefault(0)
        return MatchAnalytics.metadata(
            matchType = matchType,
            config = config,
            participantCount = participantIds.size,
            players = players,
            startSource = startSource,
            status = entity.status,
            eventCount = eventCount,
        )
    }

    fun metadataFromX01Session(
        session: X01Session,
        players: List<PlayerSummary>,
        resolution: String? = null,
    ): Map<String, String> {
        val endMillis = session.entity.completedAtEpochMillis ?: System.currentTimeMillis()
        val durationSeconds = ((endMillis - session.entity.startedAtEpochMillis) / 1_000).toInt()
        val metadata = forfeitMetadata(
            matchType = MatchType.X01,
            config = MatchConfigPayload.X01(session.config),
            participantCount = session.state.players.size,
            players = players,
            resolution = resolution ?: "automatic",
            durationSeconds = durationSeconds,
        ).toMutableMap()
        metadata["eventCount"] = session.events.size.toString()
        metadata["status"] = session.entity.status
        return metadata
    }

    fun metadataFromCricketSession(
        session: CricketSession,
        players: List<PlayerSummary>,
        resolution: String? = null,
    ): Map<String, String> {
        val endMillis = session.entity.completedAtEpochMillis ?: System.currentTimeMillis()
        val durationSeconds = ((endMillis - session.entity.startedAtEpochMillis) / 1_000).toInt()
        val metadata = forfeitMetadata(
            matchType = MatchType.Cricket,
            config = MatchConfigPayload.Cricket(session.config),
            participantCount = session.state.players.size,
            players = players,
            resolution = resolution ?: "automatic",
            durationSeconds = durationSeconds,
        ).toMutableMap()
        metadata["eventCount"] = session.events.size.toString()
        metadata["status"] = session.entity.status
        return metadata
    }
}
