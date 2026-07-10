package com.jacobrozell.dartbuddy.support.test

import android.content.Context
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.local.entity.PlayerEntity
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.support.L10n
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

data class TestRepositories(
    val matchRepository: MatchRepository,
    val playerRepository: PlayerRepository,
    val settingsRepository: SettingsRepository,
    val matchSessionService: MatchSessionService,
    val matchDao: FakeMatchDao,
    val playerDao: FakePlayerDao,
    val settingsDao: FakeSettingsDao,
    val json: Json,
    val l10n: L10n,
)

fun createTestRepositories(context: Context): TestRepositories {
    val json = TestJson.instance
    val matchDao = FakeMatchDao()
    val playerDao = FakePlayerDao()
    val settingsDao = FakeSettingsDao()
    val l10n = L10n(context)
    val matchRepository = MatchRepository(matchDao, json, l10n)
    val settingsRepository = SettingsRepository(settingsDao)
    val matchSessionService = MatchSessionService(matchRepository, json)
    val playerRepository = PlayerRepository(playerDao, matchRepository, matchSessionService, l10n)
    return TestRepositories(
        matchRepository = matchRepository,
        playerRepository = playerRepository,
        settingsRepository = settingsRepository,
        matchSessionService = matchSessionService,
        matchDao = matchDao,
        playerDao = playerDao,
        settingsDao = settingsDao,
        json = json,
        l10n = l10n,
    )
}

suspend fun TestRepositories.seedHumanPlayer(
    id: String = "human-1",
    name: String = "Alice",
    isPrimary: Boolean = true,
): PlayerEntity {
    val now = Instant.now().toEpochMilli()
    val entity = PlayerEntity(
        id = id,
        name = name,
        isPrimary = isPrimary,
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now,
    )
    playerDao.upsert(entity)
    return entity
}

suspend fun TestRepositories.seedInProgressX01Match(
    playerIds: List<String> = listOf("human-1", "human-2"),
    playerNames: List<String> = listOf("Alice", "Bob"),
): MatchEntity {
    val config = MatchConfigPayload.X01(
        MatchConfigX01(
            startScore = 501,
            legsToWin = 1,
            setsEnabled = false,
            checkoutMode = X01CheckoutMode.DoubleOut,
        ),
    )
    val state = X01Engine.makeInitialState(config.config, playerIds)
    return matchRepository.startMatch(
        matchType = MatchType.X01,
        config = config,
        participantIds = playerIds,
        participantNames = playerNames,
        snapshotJson = json.encodeToString(state),
    )
}

suspend fun TestRepositories.seedCompletedX01Match(
    playerIds: List<String> = listOf("human-1", "human-2"),
    playerNames: List<String> = listOf("Alice", "Bob"),
): MatchEntity {
    val entity = seedInProgressX01Match(playerIds, playerNames)
    val session = matchSessionService.loadX01Session(entity.id)!!
    val withTurn = matchSessionService.submitX01Turn(session, enteredTotal = 60, darts = null)
    val completed = withTurn.entity.copy(
        status = MatchStatus.Completed.name,
        winnerPlayerId = playerIds.first(),
        completedAtEpochMillis = Instant.now().toEpochMilli(),
    )
    matchRepository.saveMatch(completed)
    return completed
}

suspend fun TestRepositories.saveSettings(settings: AppSettings = AppSettings(instantBotTurnsEnabled = true)) {
    settingsRepository.save(settings)
}
