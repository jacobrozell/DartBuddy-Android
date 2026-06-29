package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import kotlinx.serialization.json.Json

object BotAnalytics {
    fun metadata(players: List<PlayerSummary>): Map<String, String> {
        val bots = players.filter { it.isBot }
        val humanCount = players.size - bots.size
        if (bots.isEmpty()) {
            return mapOf(
                "hasBot" to "false",
                "botCount" to "0",
                "humanCount" to humanCount.toString(),
                "isBot" to "false",
            )
        }
        val result = mutableMapOf(
            "hasBot" to "true",
            "botCount" to bots.size.toString(),
            "humanCount" to humanCount.toString(),
            "isBot" to "true",
        )
        val difficulties = bots.mapNotNull { it.botDifficultyRaw }.distinct().sorted()
        if (difficulties.isNotEmpty()) {
            result["botDifficulties"] = difficulties.joinToString(",")
        }
        val kinds = bots.mapNotNull { it.botKindRaw }.distinct().sorted()
        if (kinds.isNotEmpty()) {
            result["botKinds"] = kinds.joinToString(",")
        }
        if (bots.size == 1) {
            val bot = bots.first()
            bot.botDifficultyRaw?.let { result["botDifficulty"] = it }
            bot.botKindRaw?.let { result["botKind"] = it }
        }
        return result
    }
}

object MatchConfigAnalytics {
    fun metadata(config: MatchConfigPayload): Map<String, String> =
        when (config) {
            is MatchConfigPayload.X01 -> mapOf(
                "configStartScore" to config.config.startScore.toString(),
                "configCheckoutMode" to config.config.checkoutMode.name,
                "configCheckInMode" to config.config.checkInMode.name,
                "configLegFormat" to config.config.legFormat.name,
                "configSetsEnabled" to config.config.setsEnabled.toString(),
            )
            is MatchConfigPayload.Cricket -> mapOf(
                "configPointsEnabled" to config.config.pointsEnabled.toString(),
                "configScoringMode" to config.config.scoringMode.name,
                "configLegFormat" to config.config.legFormat.name,
                "configSetsEnabled" to config.config.setsEnabled.toString(),
            )
        }
}

object MatchAnalytics {
    const val RESUMED_EVENT_NAME = "match_resumed"

    fun metadata(
        matchType: MatchType,
        config: MatchConfigPayload,
        participantCount: Int,
        players: List<PlayerSummary> = emptyList(),
        startSource: String? = null,
        forfeited: Boolean? = null,
        status: String? = null,
        eventCount: Int? = null,
    ): Map<String, String> {
        val result = GameModeAnalytics.metadata(
            matchType = matchType,
            participantCount = participantCount,
            players = players,
            status = status,
        ).toMutableMap()
        startSource?.let { result["startSource"] = it }
        forfeited?.let { result["forfeited"] = it.toString() }
        eventCount?.let { result["eventCount"] = it.toString() }
        result.putAll(MatchConfigAnalytics.metadata(config))
        return result
    }

    fun resumeMetadata(
        entity: MatchEntity,
        json: Json,
        players: List<PlayerSummary>,
        startSource: String,
    ): Map<String, String>? {
        val matchType = runCatching { MatchType.valueOf(entity.matchType) }.getOrNull() ?: return null
        val config = runCatching { json.decodeFromString<MatchConfigPayload>(entity.configJson) }.getOrNull() ?: return null
        val participantIds = runCatching {
            json.decodeFromString<List<String>>(entity.participantIdsJson)
        }.getOrDefault(emptyList())
        val eventCount = runCatching {
            json.decodeFromString<List<*>>(entity.eventsJson).size
        }.getOrDefault(0)
        return metadata(
            matchType = matchType,
            config = config,
            participantCount = participantIds.size,
            players = players,
            startSource = startSource,
            status = entity.status,
            eventCount = eventCount.takeIf { it > 0 },
        )
    }

    fun logResumed(
        logger: AppLogger,
        firebaseBootstrap: FirebaseBootstrap,
        entity: MatchEntity,
        json: Json,
        players: List<PlayerSummary>,
        startSource: String,
    ) {
        val metadata = resumeMetadata(entity, json, players, startSource) ?: return
        logger.info(
            LogCategory.UI,
            eventName = RESUMED_EVENT_NAME,
            message = "User resumed an in-progress match.",
            metadata = metadata,
            correlationId = entity.id,
        )
        logger.info(
            LogCategory.SCORING,
            eventName = GameModeAnalytics.PLAYED_EVENT_NAME,
            message = "User resumed playing a game mode.",
            metadata = metadata,
            correlationId = entity.id,
        )
        runCatching { MatchType.valueOf(entity.matchType) }.getOrNull()?.let {
            AnalyticsUserIdentity.syncLastGameMode(firebaseBootstrap, it)
        }
    }

    fun logGameModePlayed(
        logger: AppLogger,
        firebaseBootstrap: FirebaseBootstrap,
        matchType: MatchType,
        metadata: Map<String, String>,
        correlationId: String,
    ) {
        logger.info(
            LogCategory.SCORING,
            eventName = GameModeAnalytics.PLAYED_EVENT_NAME,
            message = "User started playing a game mode.",
            metadata = metadata,
            correlationId = correlationId,
        )
        AnalyticsUserIdentity.syncLastGameMode(firebaseBootstrap, matchType)
    }
}

object AnalyticsUserIdentity {
    fun sync(firebaseBootstrap: FirebaseBootstrap, userId: String?) {
        if (!firebaseBootstrap.shouldConfigure) return
        val normalized = userId?.lowercase()
        if (firebaseBootstrap.isAnalyticsCollectionEnabled) {
            firebaseBootstrap.analyticsInstance()?.setUserId(normalized)
        }
        if (firebaseBootstrap.isCrashlyticsCollectionEnabled) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(normalized ?: "")
        }
    }

    fun syncLastGameMode(firebaseBootstrap: FirebaseBootstrap, matchType: MatchType) {
        if (!firebaseBootstrap.shouldConfigure || !firebaseBootstrap.isAnalyticsCollectionEnabled) return
        val analytics = firebaseBootstrap.analyticsInstance() ?: return
        val metadata = GameModeAnalytics.metadata(matchType)
        analytics.setUserProperty(matchType.name.lowercase(), "last_match_type")
        metadata["gameModeId"]?.let { analytics.setUserProperty(it, "last_game_mode_id") }
        metadata["gameModeSection"]?.let { analytics.setUserProperty(it, "last_game_mode_section") }
    }
}
