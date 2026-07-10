package com.jacobrozell.dartbuddy.support.test

import com.jacobrozell.dartbuddy.data.local.dao.MatchDao
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMatchDao : MatchDao {
    private val store = MutableStateFlow<List<MatchEntity>>(emptyList())

    override fun observeAll(): Flow<List<MatchEntity>> = store

    override fun observeActiveMatch(): Flow<MatchEntity?> =
        store.map { matches ->
            matches.filter { it.status == "InProgress" }
                .maxByOrNull { it.updatedAtEpochMillis }
        }

    override suspend fun getById(id: String): MatchEntity? = store.value.firstOrNull { it.id == id }

    override suspend fun upsert(match: MatchEntity) {
        store.update { current ->
            val without = current.filterNot { it.id == match.id }
            (without + match).sortedByDescending { it.updatedAtEpochMillis }
        }
    }

    override suspend fun deleteById(id: String) {
        store.update { it.filterNot { entity -> entity.id == id } }
    }

    override suspend fun deleteAll() {
        store.value = emptyList()
    }
}
