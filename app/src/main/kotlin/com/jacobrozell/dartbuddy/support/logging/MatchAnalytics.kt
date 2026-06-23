package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat

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
    fun metadata(
        matchType: MatchType,
        config: MatchConfigPayload,
        participantCount: Int,
        players: List<PlayerSummary> = emptyList(),
        startSource: String? = null,
        forfeited: Boolean? = null,
    ): Map<String, String> {
        val result = mutableMapOf(
            "matchType" to matchType.name.lowercase(),
            "participantCount" to participantCount.toString(),
        )
        startSource?.let { result["startSource"] = it }
        forfeited?.let { result["forfeited"] = it.toString() }
        result.putAll(MatchConfigAnalytics.metadata(config))
        result.putAll(BotAnalytics.metadata(players))
        return result
    }
}

object AnalyticsUserIdentity {
    fun sync(firebaseBootstrap: com.jacobrozell.dartbuddy.support.FirebaseBootstrap, userId: String?) {
        if (!firebaseBootstrap.shouldConfigure) return
        val normalized = userId?.lowercase()
        if (firebaseBootstrap.isAnalyticsCollectionEnabled) {
            firebaseBootstrap.analyticsInstance()?.setUserId(normalized)
        }
        if (firebaseBootstrap.isCrashlyticsCollectionEnabled) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(normalized ?: "")
        }
    }
}
