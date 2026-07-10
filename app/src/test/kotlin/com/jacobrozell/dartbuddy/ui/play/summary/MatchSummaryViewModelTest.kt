package com.jacobrozell.dartbuddy.ui.play.summary

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jacobrozell.dartbuddy.data.prefs.RematchStore
import com.jacobrozell.dartbuddy.domain.feedback.FeedbackServices
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.test.MainDispatcherRule
import com.jacobrozell.dartbuddy.support.test.NoopAppLogger
import com.jacobrozell.dartbuddy.support.test.createTestRepositories
import com.jacobrozell.dartbuddy.support.test.saveSettings
import com.jacobrozell.dartbuddy.support.test.seedCompletedX01Match
import com.jacobrozell.dartbuddy.support.test.seedHumanPlayer
import com.jacobrozell.dartbuddy.support.test.seedInProgressX01Match
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class MatchSummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repos: com.jacobrozell.dartbuddy.support.test.TestRepositories
    private lateinit var rematchStore: RematchStore
    private lateinit var viewModel: MatchSummaryViewModel

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        repos = createTestRepositories(context)
        rematchStore = RematchStore()
        viewModel = MatchSummaryViewModel(
            matchSessionService = repos.matchSessionService,
            rematchStore = rematchStore,
            playerRepository = repos.playerRepository,
            settingsRepository = repos.settingsRepository,
            feedback = FeedbackServices(context),
            logger = NoopAppLogger,
            firebaseBootstrap = FirebaseBootstrap(context),
            json = repos.json,
        )
    }

    @Test
    fun load_populatesWinnerAndStats() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedCompletedX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals("Alice", state.winnerName)
        assertEquals(2, state.playerStats.size)
        assertEquals(MatchType.X01, state.matchType)
    }

    @Test
    fun canUndoLastThrow_trueForCompletedMatchWithEvents() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedCompletedX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()

        assertTrue(viewModel.canUndoLastThrow())
    }

    @Test
    fun canUndoLastThrow_falseForInProgressMatch() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedInProgressX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()

        assertFalse(viewModel.canUndoLastThrow())
    }

    @Test
    fun prepareRematch_storesParticipantIds() = runTest {
        repos.saveSettings()
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedCompletedX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()
        viewModel.prepareRematch()

        assertEquals(listOf("human-1", "human-2"), rematchStore.pendingPlayerIds)
        assertEquals(MatchType.X01.name, rematchStore.pendingMatchType)
    }
}
