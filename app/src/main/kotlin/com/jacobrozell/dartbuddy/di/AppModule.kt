package com.jacobrozell.dartbuddy.di

import android.content.Context
import androidx.room.Room
import com.jacobrozell.dartbuddy.data.local.DartBuddyDatabase
import com.jacobrozell.dartbuddy.data.local.dao.MatchDao
import com.jacobrozell.dartbuddy.data.local.dao.PlayerDao
import com.jacobrozell.dartbuddy.data.local.dao.SettingsDao
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.DefaultAppLogger
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.store.HttpPlayStoreVersionFetcher
import com.jacobrozell.dartbuddy.support.store.PlayStoreVersionFetcher
import com.jacobrozell.dartbuddy.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DartBuddyDatabase =
        Room.databaseBuilder(context, DartBuddyDatabase::class.java, "dart_buddy.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePlayerDao(db: DartBuddyDatabase): PlayerDao = db.playerDao()
    @Provides fun provideMatchDao(db: DartBuddyDatabase): MatchDao = db.matchDao()
    @Provides fun provideSettingsDao(db: DartBuddyDatabase): SettingsDao = db.settingsDao()

    @Provides
    @Singleton
    fun provideAppLogger(firebaseBootstrap: FirebaseBootstrap): AppLogger =
        DefaultAppLogger.create(firebaseBootstrap, BuildConfig.VERSION_NAME)

    @Provides
    @Singleton
    fun providePlayStoreVersionFetcher(
        impl: HttpPlayStoreVersionFetcher,
    ): PlayStoreVersionFetcher = impl
}
