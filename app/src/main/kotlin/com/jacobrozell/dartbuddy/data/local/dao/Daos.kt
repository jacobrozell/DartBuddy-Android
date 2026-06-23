package com.jacobrozell.dartbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.local.entity.PlayerEntity
import com.jacobrozell.dartbuddy.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE linkedPlayerId = :humanId AND botKindRaw = 'training' LIMIT 1")
    suspend fun getTrainingBotFor(humanId: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActivePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    fun observeAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE isPrimary = 1 AND isArchived = 0 LIMIT 1")
    suspend fun getPrimaryPlayer(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(player: PlayerEntity)

    @Query("UPDATE players SET isArchived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long)

    @Query("UPDATE players SET isArchived = 0, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun unarchive(id: String, updatedAt: Long)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM players")
    suspend fun deleteAll()
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status = 'InProgress' ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    fun observeActiveMatch(): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
