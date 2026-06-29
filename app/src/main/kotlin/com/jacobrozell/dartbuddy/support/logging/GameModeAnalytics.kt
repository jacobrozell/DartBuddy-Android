package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.MatchType

/// Catalog-backed metadata for game-mode product-health telemetry.
object GameModeAnalytics {
    const val PLAYED_EVENT_NAME = "game_mode_played"
    const val COMPLETED_EVENT_NAME = "game_mode_completed"
    const val FORFEITED_EVENT_NAME = "game_mode_forfeited"

    private data class CatalogEntry(
        val gameModeId: String,
        val gameModeSection: String,
        val uiTemplate: String,
        val statKind: String,
    )

    private val catalog = mapOf(
        MatchType.X01 to CatalogEntry(
            gameModeId = "standard.x01",
            gameModeSection = "standard",
            uiTemplate = "checkoutScore",
            statKind = "checkout",
        ),
        MatchType.Cricket to CatalogEntry(
            gameModeId = "standard.cricket",
            gameModeSection = "standard",
            uiTemplate = "markBoard",
            statKind = "marks",
        ),
    )

    fun metadata(
        matchType: MatchType,
        participantCount: Int? = null,
        players: List<PlayerSummary>? = null,
        status: String? = null,
        extra: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        val result = extra.toMutableMap()
        result["matchType"] = matchType.name.lowercase()
        participantCount?.let { result["participantCount"] = it.toString() }
        catalog[matchType]?.let { entry ->
            result["gameModeId"] = entry.gameModeId
            result["gameModeSection"] = entry.gameModeSection
            result["uiTemplate"] = entry.uiTemplate
            result["statKind"] = entry.statKind
        } ?: run {
            result["gameModeId"] = matchType.name.lowercase()
        }
        status?.let { result["status"] = it }
        players?.let { result.putAll(BotAnalytics.metadata(it)) }
        return AnalyticsMetadataKeys.withoutPersonalData(result)
    }
}
