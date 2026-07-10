package com.jacobrozell.dartbuddy.ui.activity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.support.L10n
import com.jacobrozell.dartbuddy.support.test.MainDispatcherRule
import com.jacobrozell.dartbuddy.support.test.createTestRepositories
import com.jacobrozell.dartbuddy.support.test.seedCompletedX01Match
import com.jacobrozell.dartbuddy.support.test.seedHumanPlayer
import com.jacobrozell.dartbuddy.support.test.seedInProgressX01Match
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repos: com.jacobrozell.dartbuddy.support.test.TestRepositories
    private lateinit var viewModel: StatisticsViewModel

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        repos = createTestRepositories(context)
        viewModel = StatisticsViewModel(
            matchRepository = repos.matchRepository,
            matchSessionService = repos.matchSessionService,
            json = repos.json,
        )
    }

    @Test
    fun load_aggregatesCompletedMatches() = runTest {
        val collectJob = launch { viewModel.rows.collect { } }
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        repos.seedCompletedX01Match()

        viewModel.load(ActivityFilter.All, ActivityPeriod.All, playerId = null)
        advanceUntilIdle()

        val rows = viewModel.rows.dropWhile { it.isEmpty() }.first()
        collectJob.cancel()
        assertEquals(2, rows.size)
        assertFalse(viewModel.includesPartial.value)
    }

    @Test
    fun load_setsIncludesPartialWhenInProgressPresent() = runTest {
        val collectJob = launch {
            viewModel.includesPartial.collect { }
            viewModel.rows.collect { }
        }
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        repos.seedInProgressX01Match()

        viewModel.load(ActivityFilter.All, ActivityPeriod.All, playerId = null)
        advanceUntilIdle()

        collectJob.cancel()
        assertTrue(viewModel.includesPartial.value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repos: com.jacobrozell.dartbuddy.support.test.TestRepositories
    private lateinit var viewModel: HistoryDetailViewModel

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        repos = createTestRepositories(context)
        viewModel = HistoryDetailViewModel(
            matchSessionService = repos.matchSessionService,
            matchRepository = repos.matchRepository,
            l10n = L10n(context),
            json = repos.json,
        )
    }

    @Test
    fun load_populatesDetailForCompletedMatch() = runTest {
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedCompletedX01Match()

        viewModel.load(match.id)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertNotNull(state.detail)
        assertEquals(MatchStatus.Completed, state.detail?.status)
        assertNotNull(state.detail?.resultRow)
    }

    @Test
    fun load_setsLoadFailedWhenMissing() = runTest {
        viewModel.load("missing")
        advanceUntilIdle()

        assertFalse(viewModel.state.value.loading)
        assertTrue(viewModel.state.value.loadFailed)
        assertNull(viewModel.state.value.detail)
    }

    @Test
    fun deleteMatch_removesEntity() = runTest {
        repos.seedHumanPlayer()
        repos.seedHumanPlayer(id = "human-2", name = "Bob", isPrimary = false)
        val match = repos.seedCompletedX01Match()

        viewModel.deleteMatch(match.id) { }
        advanceUntilIdle()

        assertNull(repos.matchRepository.getMatch(match.id))
    }
}
