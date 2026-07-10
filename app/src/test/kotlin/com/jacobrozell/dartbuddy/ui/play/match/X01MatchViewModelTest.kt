package com.jacobrozell.dartbuddy.ui.play.match

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.test.MainDispatcherRule
import com.jacobrozell.dartbuddy.support.test.NoopAppLogger
import com.jacobrozell.dartbuddy.support.test.createTestRepositories
import com.jacobrozell.dartbuddy.support.test.saveSettings
import com.jacobrozell.dartbuddy.support.test.seedHumanPlayer
import com.jacobrozell.dartbuddy.support.test.seedInProgressX01Match
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class X01MatchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var repos: com.jacobrozell.dartbuddy.support.test.TestRepositories
    private lateinit var viewModel: X01MatchViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repos = createTestRepositories(context)
        viewModel = X01MatchViewModel(
            matchSessionService = repos.matchSessionService,
            matchRepository = repos.matchRepository,
            playerRepository = repos.playerRepository,
            settingsRepository = repos.settingsRepository,
            feedback = FeedbackServices(context),
            logger = NoopAppLogger,
            firebaseBootstrap = FirebaseBootstrap(context),
            l10n = repos.l10n,
            json = repos.json,
        )
    }

    @Test
    fun load_setsErrorWhenMatchMissing() = runTest {
        viewModel.load("missing")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertEquals("Match not found", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun load_populatesSessionAndParticipants() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer(id = "human-1", name = "Alice")
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedInProgressX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.errorMessage)
        assertEquals(listOf("Alice", "Bob"), state.participantNames)
        assertTrue(state.instantBotTurnsEnabled)
    }

    @Test
    fun canForfeit_falseUntilEventsExist() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedInProgressX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()
        assertFalse(viewModel.canForfeit())

        val session = repos.matchSessionService.loadX01Session(match.id)!!
        repos.matchSessionService.submitX01Turn(session, enteredTotal = 60, darts = null)
        viewModel.load(match.id)
        advanceUntilIdle()
        assertTrue(viewModel.canForfeit())
    }

    @Test
    fun humanParticipantIds_excludesBots() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer(id = "human-1", name = "Alice")
        val bot = repos.playerRepository.createPresetBot(com.jacobrozell.dartbuddy.domain.bots.BotDifficulty.Easy)
        val match = repos.seedInProgressX01Match(
            playerIds = listOf("human-1", bot.id),
            playerNames = listOf("Alice", bot.name),
        )

        viewModel.load(match.id)
        advanceUntilIdle()

        assertEquals(listOf("human-1"), viewModel.humanParticipantIds())
    }

    @Test
    fun abandonMatch_deletesMatchAndInvokesCallback() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedInProgressX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()

        var done = false
        viewModel.abandonMatch { done = true }
        advanceUntilIdle()

        assertTrue(done)
        assertNull(repos.matchRepository.getMatch(match.id))
    }

    @Test
    fun forfeit_persistsForfeitedStatus() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer(id = "human-1", name = "Alice")
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedInProgressX01Match()
        val session = repos.matchSessionService.loadX01Session(match.id)!!
        repos.matchSessionService.submitX01Turn(session, enteredTotal = 60, darts = null)

        viewModel.load(match.id)
        advanceUntilIdle()

        var completedId: String? = null
        viewModel.forfeit("human-1", "human-2") { completedId = it }
        advanceUntilIdle()

        assertEquals(match.id, completedId)
        assertEquals("Forfeited", repos.matchRepository.getMatch(match.id)?.status)
    }
}
