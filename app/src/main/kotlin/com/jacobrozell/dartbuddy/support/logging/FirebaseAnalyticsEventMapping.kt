package com.jacobrozell.dartbuddy.support.logging

data class FirebaseAnalyticsEvent(
    val name: String,
    val parameters: Map<String, String>,
)

object FirebaseAnalyticsEventMapping {
    private val allowlistedLogEvents = setOf(
        "app_bootstrap_ready",
        "main_tab_presented",
        "play_home_ready",
        "play_home_active_match",
        "match_setup_start",
        "match_started",
        "game_mode_played",
        "game_mode_completed",
        "game_mode_forfeited",
        "match_resumed",
        "match_setup_baseball",
        "match_screen_appeared",
        "match_completed",
        "turn_submitted",
        "turn_undone",
        "dart_undone",
        "match_abandoned",
        "match_forfeited",
        "match_forfeit_failed",
        "match_start_failed",
        "turn_persist_failed",
        "bootstrap_store_open_failed",
        "deep_link_received",
        "deep_link_applied",
        "deep_link_deferred",
        "deep_link_failed",
        "intent_performed",
        "intent_failed",
        "client_environment_changed",
        "onboarding_completed",
    )

    private val firebaseNameOverrides = mapOf(
        "app_bootstrap_ready" to "app_open",
        "turn_undone" to "undo_used",
        "dart_undone" to "undo_used",
    )

    fun map(entry: LogEntry, appVersion: String?): FirebaseAnalyticsEvent? {
        if (!allowlistedLogEvents.contains(entry.eventName)) return null
        val parameters = FirebaseMetadataSanitizer
            .sanitize(entry.metadata, AnalyticsMetadataKeys.firebaseParameters)
            .toMutableMap()
        if (!appVersion.isNullOrBlank()) {
            parameters["app_version"] = appVersion
        }
        parameters["log_category"] = entry.category.rawValue
        val firebaseName = firebaseNameOverrides[entry.eventName] ?: entry.eventName
        return FirebaseAnalyticsEvent(firebaseName, parameters)
    }
}
