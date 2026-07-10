package com.jacobrozell.dartbuddy.ui.play.match

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.test.MainDispatcherRule
import com.jacobrozell.dartbuddy.support.test.NoopAppLogger
import com.jacobrozell.dartbuddy.support.test.createTestRepositories
import com.jacobrozell.dartbuddy.support.test.saveSettings
import com.jacobrozell.dartbuddy.support.test.seedHumanPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CricketMatchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repos: com.jacobrozell.dartbuddy.support.test.TestRepositories
    private lateinit var viewModel: CricketMatchViewModel

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        repos = createTestRepositories(context)
        viewModel = CricketMatchViewModel(
            matchSessionService = repos.matchSessionService,
            matchRepository = repos.matchRepository,
            playerRepository = repos.playerRepository,
            settingsRepository = repos.settingsRepository,
            feedback = FeedbackServices(context),
            logger = NoopAppLogger,
            firebaseBootstrap = FirebaseBootstrap(context),
            json = repos.json,
        )
    }

    @Test
    fun load_setsErrorWhenMatchMissing() = runTest {
        viewModel.load("missing")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertTrue(viewModel.uiState.value.errorMessage != null)
    }

    @Test
    fun canForfeit_falseUntilEventsExist() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer(id = "human-1", name = "Alice")
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = seedCricketMatch()

        viewModel.load(match.id)
        advanceUntilIdle()
        assertFalse(viewModel.canForfeit())

        val session = repos.matchSessionService.loadCricketSession(match.id)!!
        val dart = DartInput(multiplier = DartMultiplier.Triple, segment = DartSegment.OneToTwenty(20))
        repos.matchSessionService.submitCricketTurn(session, listOf(dart, dart, dart))
        viewModel.load(match.id)
        advanceUntilIdle()
        assertTrue(viewModel.canForfeit())
    }

    private suspend fun seedCricketMatch(): com.jacobrozell.dartbuddy.data.local.entity.MatchEntity {
        val playerIds = listOf("human-1", "human-2")
        val config = com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload.Cricket(
            com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket(
                legsToWin = 1,
                setsEnabled = false,
            ),
        )
        val state = com.jacobrozell.dartbuddy.domain.engine.CricketEngine.makeInitialState(config.config, playerIds)
        return repos.matchRepository.startMatch(
            matchType = com.jacobrozell.dartbuddy.domain.model.MatchType.Cricket,
            config = config,
            participantIds = playerIds,
            participantNames = listOf("Alice", "Bob"),
            snapshotJson = repos.json.encodeToString(state),
        )
    }
}
