package com.jacobrozell.dartbuddy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacobrozell.dartbuddy.data.local.DartBuddyDatabase
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.local.entity.PlayerEntity
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.prefs.PracticeSetupStore
import com.jacobrozell.dartbuddy.data.prefs.RematchStore
import com.jacobrozell.dartbuddy.data.prefs.PlayStoreUpdateStore
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.support.L10n
import com.jacobrozell.dartbuddy.support.test.InstrumentedTestJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RepositoryContractTest {
    private lateinit var db: DartBuddyDatabase
    private lateinit var matchRepository: MatchRepository
    private lateinit var playerRepository: PlayerRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var resetService: LocalDataResetService
    private val json = InstrumentedTestJson.instance

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DartBuddyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val l10n = L10n(context)
        matchRepository = MatchRepository(db.matchDao(), json, l10n)
        val sessionService = MatchSessionService(matchRepository, json)
        playerRepository = PlayerRepository(db.playerDao(), matchRepository, sessionService, l10n)
        settingsRepository = SettingsRepository(db.settingsDao())
        resetService = LocalDataResetService(
            playerDao = db.playerDao(),
            matchDao = db.matchDao(),
            settingsDao = db.settingsDao(),
            settingsRepository = settingsRepository,
            onboardingStore = OnboardingStore(context),
            rematchStore = RematchStore(),
            practiceSetupStore = PracticeSetupStore(),
            playStoreUpdateStore = PlayStoreUpdateStore(context),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeHistoryRows_excludesInProgress() = runBlocking {
        insertMatch(status = MatchStatus.InProgress, id = "active")
        insertMatch(status = MatchStatus.Completed, id = "done")

        val rows = matchRepository.observeHistoryRows().first()

        assertEquals(1, rows.size)
        assertEquals("done", rows.first().id)
    }

    @Test
    fun observeActiveMatch_returnsLatestInProgress() = runBlocking {
        insertMatch(status = MatchStatus.InProgress, id = "older", updatedAt = 1)
        insertMatch(status = MatchStatus.InProgress, id = "newer", updatedAt = 2)

        val active = matchRepository.observeActiveMatch().first()

        assertEquals("newer", active?.id)
    }

    @Test
    fun historyRows_includeForfeited_excludeAbandoned() = runBlocking {
        insertMatch(status = MatchStatus.Forfeited, id = "forfeit")
        insertMatch(status = MatchStatus.Abandoned, id = "abandoned")

        val rows = matchRepository.observeHistoryRows().first()

        assertEquals(1, rows.size)
        assertEquals("forfeit", rows.first().id)
    }

    @Test
    fun settingsRepository_get_seedsDefaultsWhenEmpty() = runBlocking {
        val settings = settingsRepository.get()

        assertTrue(settings.hapticsEnabled)
        assertEquals(MatchType.X01, settings.defaultMatchType)
        assertNotNull(db.settingsDao().get())
    }

    @Test
    fun settingsRepository_save_roundTripsBotToggles() = runBlocking {
        val updated = AppSettings(
            hapticsEnabled = false,
            soundEnabled = false,
            botStaggerEnabled = false,
            botDartHapticsEnabled = false,
            instantBotTurnsEnabled = true,
        )
        settingsRepository.save(updated)

        assertEquals(updated, settingsRepository.get())
    }

    @Test
    fun playerRepository_observeActivePlayers_hidesArchived() = runBlocking {
        val now = Instant.now().toEpochMilli()
        db.playerDao().upsert(
            PlayerEntity(
                id = "active",
                name = "Active",
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        db.playerDao().upsert(
            PlayerEntity(
                id = "archived",
                name = "Archived",
                isArchived = true,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )

        val active = playerRepository.observeActivePlayers().first()
        val all = playerRepository.observeAllPlayers().first()

        assertEquals(1, active.size)
        assertEquals(2, all.size)
    }

    @Test
    fun localDataResetService_clearsTablesAndRestoresDefaults() = runBlocking {
        insertMatch(status = MatchStatus.Completed, id = "m1")
        val now = Instant.now().toEpochMilli()
        db.playerDao().upsert(
            PlayerEntity(id = "p1", name = "P", createdAtEpochMillis = now, updatedAtEpochMillis = now),
        )
        settingsRepository.save(AppSettings(soundEnabled = false))

        resetService.resetAllLocalData()

        assertTrue(db.matchDao().observeAll().first().isEmpty())
        assertTrue(db.playerDao().observeAllPlayers().first().isEmpty())
        assertTrue(settingsRepository.get().soundEnabled)
    }

    private suspend fun insertMatch(
        status: MatchStatus,
        id: String,
        updatedAt: Long = Instant.now().toEpochMilli(),
    ) {
        val config = MatchConfigPayload.X01(
            MatchConfigX01(
                startScore = 501,
                legsToWin = 1,
                setsEnabled = false,
                checkoutMode = X01CheckoutMode.DoubleOut,
            ),
        )
        val state = X01Engine.makeInitialState(config.config, listOf("p1", "p2"))
        val now = Instant.now().toEpochMilli()
        db.matchDao().upsert(
            MatchEntity(
                id = id,
                matchType = MatchType.X01.name,
                status = status.name,
                configJson = json.encodeToString(config),
                participantIdsJson = json.encodeToString(listOf("p1", "p2")),
                participantNamesJson = json.encodeToString(listOf("A", "B")),
                snapshotJson = json.encodeToString(state),
                startedAtEpochMillis = now,
                updatedAtEpochMillis = updatedAt,
            ),
        )
    }
}
