package com.jacobrozell.dartbuddy.support.test

import com.jacobrozell.dartbuddy.data.local.dao.SettingsDao
import com.jacobrozell.dartbuddy.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSettingsDao : SettingsDao {
    private val store = MutableStateFlow<SettingsEntity?>(null)

    override fun observe(): Flow<SettingsEntity?> = store

    override suspend fun get(): SettingsEntity? = store.value

    override suspend fun upsert(settings: SettingsEntity) {
        store.value = settings
    }

    override suspend fun deleteAll() {
        store.value = null
    }

    fun count(): Int = if (store.value == null) 0 else 1
}
