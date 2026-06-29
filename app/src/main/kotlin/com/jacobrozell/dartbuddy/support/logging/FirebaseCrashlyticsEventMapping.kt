package com.jacobrozell.dartbuddy.support.logging

object FirebaseCrashlyticsEventMapping {
    private const val ERROR_DOMAIN = "com.jacobrozell.dartbuddy.logger"

    private val allowlistedLogEvents = setOf(
        "bootstrap_store_open_failed",
        "match_start_failed",
        "turn_persist_failed",
        "match_session_load_failed",
        "play_home_load_failed",
        "active_match_lookup_failed",
        "active_match_replace_failed",
        "turn_undo_failed",
        "x01_abandon_failed",
        "cricket_abandon_failed",
        "match_forfeit_failed",
        "settings_reset_failed",
        "seed_training_partner_failed",
    )

    val eventCodes = mapOf(
        "bootstrap_store_open_failed" to 1001,
        "match_start_failed" to 1002,
        "turn_persist_failed" to 1003,
        "match_session_load_failed" to 1004,
        "play_home_load_failed" to 1005,
        "active_match_lookup_failed" to 1006,
        "active_match_replace_failed" to 1007,
        "turn_undo_failed" to 1008,
        "x01_abandon_failed" to 1009,
        "cricket_abandon_failed" to 1010,
        "settings_reset_failed" to 1011,
        "match_forfeit_failed" to 1012,
        "seed_training_partner_failed" to 1013,
    )

    fun nonFatalError(entry: LogEntry, appVersion: String?): Throwable? {
        if (!entry.level.atLeast(LogLevel.ERROR)) return null
        if (!allowlistedLogEvents.contains(entry.eventName)) return null
        val code = eventCodes[entry.eventName] ?: return null
        val userInfo = FirebaseMetadataSanitizer
            .sanitize(entry.metadata, AnalyticsMetadataKeys.crashlyticsParameters)
            .toMutableMap()
        userInfo["log_category"] = entry.category.rawValue
        userInfo["event_name"] = entry.eventName
        if (!appVersion.isNullOrBlank()) {
            userInfo["app_version"] = appVersion
        }
        return LoggerNonFatalException(code, userInfo)
    }
}

class LoggerNonFatalException(
    code: Int,
    val userInfo: Map<String, String>,
) : Exception("DartBuddy logger error $code") {
    val errorCode: Int = code
}
