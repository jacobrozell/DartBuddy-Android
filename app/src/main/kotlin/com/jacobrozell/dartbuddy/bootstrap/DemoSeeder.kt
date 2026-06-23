package com.jacobrozell.dartbuddy.bootstrap

import android.content.Intent
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.prefs.PracticeSetupStore
import com.jacobrozell.dartbuddy.data.repository.LocalDataResetService
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.error
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoSeeder @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val practiceSetupStore: PracticeSetupStore,
    private val localDataResetService: LocalDataResetService,
    private val json: Json,
    private val logger: AppLogger,
) {
    suspend fun seedIfRequested(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(DemoSeedExtras.RESET, false)) {
            runCatching { localDataResetService.resetAllLocalData() }
                .onFailure {
                    logger.error(
                        LogCategory.APP_LIFECYCLE,
                        eventName = "ui_test_reset_failed",
                        message = "Demo reset failed: ${it.message}",
                    )
                }
        }
        if (intent.getBooleanExtra(DemoSeedExtras.SEED_PLAYERS, false)) {
            seedPlayersOnly()
        }
        if (intent.getBooleanExtra(DemoSeedExtras.SEED_TRAINING_LOCKED, false)) {
            seedTrainingPartnerState(completedX01Games = 3, createBot = false)
        }
        if (intent.getBooleanExtra(DemoSeedExtras.SEED_TRAINING_ELIGIBLE, false)) {
            seedTrainingPartnerState(completedX01Games = 5, createBot = false)
        }
        if (intent.getBooleanExtra(DemoSeedExtras.SEED_TRAINING_PARTNER, false)) {
            seedTrainingPartnerState(completedX01Games = 5, createBot = true)
        }
    }

    private suspend fun seedPlayersOnly() {
        runCatching {
            if (playerRepository.observeActivePlayers().first().isNotEmpty()) return
            playerRepository.create("Alice")
            playerRepository.create("Bob")
            playerRepository.create("Carol")
        }.onFailure {
            logger.error(
                LogCategory.APP_LIFECYCLE,
                eventName = "seed_players_failed",
                message = "Seed players failed: ${it.message}",
            )
        }
    }

    private suspend fun seedTrainingPartnerState(completedX01Games: Int, createBot: Boolean) {
        runCatching {
            seedPlayersOnly()
            val players = playerRepository.observeActivePlayers().first()
            val alice = humanPlayer("Alice", players) ?: playerRepository.create("Alice")
            val bob = humanPlayer("Bob", players) ?: playerRepository.create("Bob")
            repeat(completedX01Games) {
                seedCompletedX01Match(alice, bob)
            }
            if (createBot) {
                playerRepository.fetchTrainingBot(alice.id)
                    ?: playerRepository.createTrainingBot(alice.id)
            }
        }.onFailure {
            logger.error(
                LogCategory.APP_LIFECYCLE,
                eventName = "seed_training_partner_failed",
                message = "Training partner seed failed: ${it.message}",
            )
        }
    }

    suspend fun enqueueTrainingPracticeIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra(DemoSeedExtras.ENQUEUE_TRAINING_MATCH, false) != true) return
        runCatching {
            val players = playerRepository.observeActivePlayers().first()
            val alice = humanPlayer("Alice", players) ?: return
            val bot = playerRepository.fetchTrainingBot(alice.id) ?: return
            practiceSetupStore.enqueuePractice(alice.id, bot.id, MatchType.X01)
        }
    }

    private suspend fun seedCompletedX01Match(alice: PlayerSummary, bob: PlayerSummary) {
        val now = Instant.now().toEpochMilli()
        val config = MatchConfigPayload.X01(
            MatchConfigX01(
                startScore = 101,
                legsToWin = 1,
                setsEnabled = false,
                setsToWin = null,
                checkoutMode = X01CheckoutMode.SingleOut,
            ),
        )
        val entity = MatchEntity(
            id = UUID.randomUUID().toString(),
            matchType = MatchType.X01.name,
            status = MatchStatus.Completed.name,
            configJson = json.encodeToString(config),
            participantIdsJson = json.encodeToString(listOf(alice.id, bob.id)),
            participantNamesJson = json.encodeToString(listOf(alice.name, bob.name)),
            winnerPlayerId = alice.id,
            startedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = now,
        )
        matchRepository.saveMatch(entity)
    }

    private fun humanPlayer(name: String, players: List<PlayerSummary>): PlayerSummary? =
        players.firstOrNull { !it.isBot && it.name.equals(name, ignoreCase = true) }
}
