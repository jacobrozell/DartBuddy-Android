package com.jacobrozell.dartbuddy.domain.export

import kotlinx.serialization.Serializable

@Serializable
data class PlayerExportBundle(
    val dbpeVersion: Int = 1,
    val producer: String,
    val producerVersion: String,
    val exportedAtEpochMillis: Long,
    val persistenceSchemaVersion: String = "2.0.0",
    val anchorPlayerId: String,
    val player: PlayerExportRecord,
    val referencedPlayers: List<PlayerExportRecord> = emptyList(),
    val matches: List<MatchExportBundle> = emptyList(),
)

@Serializable
data class PlayerExportRecord(
    val id: String,
    val name: String,
    val isArchived: Boolean,
    val isBot: Boolean,
    val botDifficultyRaw: String? = null,
    val botKindRaw: String? = null,
    val linkedPlayerId: String? = null,
    val avatarStyleRaw: String? = null,
    val preferredColorToken: String? = null,
    val notes: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class MatchExportBundle(
    val match: MatchExportRecord,
    val configJson: String,
    val participantIdsJson: String,
    val participantNamesJson: String,
    val eventsJson: String,
    val snapshotJson: String? = null,
)

@Serializable
data class MatchExportRecord(
    val id: String,
    val matchType: String,
    val status: String,
    val winnerPlayerId: String? = null,
    val forfeitedByPlayerId: String? = null,
    val startedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)
