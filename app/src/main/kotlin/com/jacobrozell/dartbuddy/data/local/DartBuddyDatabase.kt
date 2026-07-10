package com.jacobrozell.dartbuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jacobrozell.dartbuddy.data.local.dao.MatchDao
import com.jacobrozell.dartbuddy.data.local.dao.PlayerDao
import com.jacobrozell.dartbuddy.data.local.dao.SettingsDao
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.local.entity.PlayerEntity
import com.jacobrozell.dartbuddy.data.local.entity.SettingsEntity

@Database(
    entities = [PlayerEntity::class, MatchEntity::class, SettingsEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class DartBuddyDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun settingsDao(): SettingsDao
}
