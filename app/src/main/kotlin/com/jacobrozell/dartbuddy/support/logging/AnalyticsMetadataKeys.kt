package com.jacobrozell.dartbuddy.support.logging

object AnalyticsMetadataKeys {
    val clientEnvironment: Set<String> = setOf(
        "deviceClass",
        "isVoiceOverRunning",
        "isSwitchControlRunning",
        "isBoldTextEnabled",
        "isReduceMotionEnabled",
        "isScreenCaptured",
        "isExternalDisplayConnected",
        "interfaceOrientation",
        "contentSizeCategory",
        "colorScheme",
        "isLowPowerModeEnabled",
        "trigger",
        "changedSignals",
    )

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

    private val gameMode = setOf(
        "gameModeId",
        "gameModeSection",
        "uiTemplate",
        "statKind",
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
        "detectedScore",
        "correctedScore",
        "visionConfidence",
        "visionExact",
        "visionChanged",
        "visionDismissed",
    )

    val firebaseParameters: Set<String> = generalFirebase
        .union(botRoster)
        .union(gameMode)
        .union(matchEntry)
        .union(matchConfig)
        .union(onboarding)
        .union(clientEnvironment)

    val crashlyticsParameters: Set<String> = setOf(
        "matchType",
        "gameModeId",
        "gameModeSection",
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
