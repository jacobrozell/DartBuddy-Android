package com.jacobrozell.dartbuddy.support.logging

object AnalyticsMetadataKeys {
    private val blockedPersonalDataKeys = setOf(
        "playerId",
        "playerName",
        "displayName",
        "displayNameAtMatchStart",
        "botName",
        "userName",
        "linkedPlayerId",
        "forfeited_by_player_id",
        "winner_player_id",
        "notes",
    )

    private val blockedPersonalDataKeyFragments = listOf(
        "playername",
        "displayname",
        "botname",
        "username",
        "profilename",
        "linkedplayer",
        "participantname",
        "rostername",
    )

    private val botRoster = setOf(
        "hasBot",
        "botCount",
        "humanCount",
        "botDifficulty",
        "botDifficulties",
        "botKind",
        "botKinds",
        "botEffectiveTier",
        "botEffectiveTiers",
    )

    private val matchEntry = setOf("startSource")

    private val matchConfig = setOf(
        "configStartScore",
        "configCheckoutMode",
        "configCheckInMode",
        "configLegFormat",
        "configSetsEnabled",
        "configPointsEnabled",
        "configScoringMode",
    )

    private val onboarding = setOf("skipped", "bot_tier", "created_player")

    private val generalFirebase = setOf(
        "matchType",
        "errorCode",
        "layer",
        "status",
        "participantCount",
        "eventCount",
        "durationSeconds",
        "resolution",
        "operation",
        "schemaVersion",
        "fromSchema",
        "toSchema",
        "legIndex",
        "setIndex",
        "source",
        "isBot",
        "path",
        "version",
        "intentName",
        "tab",
        "forfeited",
    )

    val firebaseParameters: Set<String> = generalFirebase
        .union(botRoster)
        .union(matchEntry)
        .union(matchConfig)
        .union(onboarding)

    val crashlyticsParameters: Set<String> = setOf(
        "matchType",
        "errorCode",
        "layer",
        "status",
        "participantCount",
        "operation",
        "schemaVersion",
        "fromSchema",
        "toSchema",
        "legIndex",
        "setIndex",
        "source",
        "isBot",
        "tab",
    )

    fun isBlockedPersonalDataKey(key: String): Boolean {
        if (blockedPersonalDataKeys.contains(key)) return true
        val lower = key.lowercase()
        return blockedPersonalDataKeyFragments.any { lower.contains(it) }
    }

    fun withoutPersonalData(metadata: Map<String, String>): Map<String, String> =
        metadata.filterKeys { !isBlockedPersonalDataKey(it) }
}
