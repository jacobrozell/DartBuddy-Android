package com.jacobrozell.dartbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isArchived: Boolean = false,
    val isBot: Boolean = false,
    val botDifficultyRaw: String? = null,
    val botKindRaw: String? = null,
    val avatarStyleRaw: String? = null,
    val colorToken: String? = null,
    val notes: String? = null,
    val isPrimary: Boolean = false,
    val linkedPlayerId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val matchType: String,
    val status: String,
    val configJson: String,
    val participantIdsJson: String,
    val participantNamesJson: String,
    val snapshotJson: String? = null,
    val eventsJson: String = "[]",
    val winnerPlayerId: String? = null,
    val forfeitedByPlayerId: String? = null,
    val startedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val appearanceMode: String = "system",
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val defaultX01StartScore: Int = 501,
    val defaultX01CheckoutMode: String = "DoubleOut",
    val defaultX01CheckInMode: String = "StraightIn",
    val defaultLegFormat: String = "FirstTo",
    val defaultLegsToWin: Int = 1,
    val defaultSetsEnabled: Boolean = false,
    val defaultSetsToWin: Int = 1,
    val defaultCricketPointsEnabled: Boolean = true,
    val defaultCricketScoringMode: String = "Standard",
    val defaultMatchType: String = "X01",
    val turnTotalCallerEnabled: Boolean = false,
    val botStaggerEnabled: Boolean = true,
    val botDartHapticsEnabled: Boolean = true,
    val instantBotTurnsEnabled: Boolean = false,
)
