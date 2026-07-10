package com.jacobrozell.dartbuddy.support.test

import com.jacobrozell.dartbuddy.data.local.dao.PlayerDao
import com.jacobrozell.dartbuddy.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePlayerDao : PlayerDao {
    private val store = MutableStateFlow<List<PlayerEntity>>(emptyList())

    override suspend fun getTrainingBotFor(humanId: String): PlayerEntity? =
        store.value.firstOrNull { it.linkedPlayerId == humanId && it.botKindRaw == "training" }

    override fun observeActivePlayers(): Flow<List<PlayerEntity>> =
        store.map { list -> list.filter { !it.isArchived }.sortedBy { it.name.lowercase() } }

    override fun observeAllPlayers(): Flow<List<PlayerEntity>> =
        store.map { list -> list.sortedBy { it.name.lowercase() } }

    override suspend fun getById(id: String): PlayerEntity? = store.value.firstOrNull { it.id == id }

    override suspend fun getPrimaryPlayer(): PlayerEntity? =
        store.value.firstOrNull { it.isPrimary && !it.isArchived }

    override suspend fun upsert(player: PlayerEntity) {
        store.update { current ->
            val without = current.filterNot { it.id == player.id }
            without + player
        }
    }

    override suspend fun archive(id: String, updatedAt: Long) {
        store.update { list ->
            list.map { if (it.id == id) it.copy(isArchived = true, updatedAtEpochMillis = updatedAt) else it }
        }
    }

    override suspend fun unarchive(id: String, updatedAt: Long) {
        store.update { list ->
            list.map { if (it.id == id) it.copy(isArchived = false, updatedAtEpochMillis = updatedAt) else it }
        }
    }

    override suspend fun deleteById(id: String) {
        store.update { it.filterNot { player -> player.id == id } }
    }

    override suspend fun deleteAll() {
        store.value = emptyList()
    }
}
