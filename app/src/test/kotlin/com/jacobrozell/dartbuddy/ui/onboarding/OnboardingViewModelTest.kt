package com.jacobrozell.dartbuddy.ui.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.prefs.RematchStore
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingMode
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingStep
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.test.MainDispatcherRule
import com.jacobrozell.dartbuddy.support.test.NoopAppLogger
import com.jacobrozell.dartbuddy.support.test.createTestRepositories
import com.jacobrozell.dartbuddy.support.test.seedHumanPlayer
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
class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var repos: com.jacobrozell.dartbuddy.support.test.TestRepositories
    private lateinit var onboardingStore: OnboardingStore
    private lateinit var rematchStore: RematchStore
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repos = createTestRepositories(context)
        onboardingStore = OnboardingStore(context)
        onboardingStore.clear()
        rematchStore = RematchStore()
        viewModel = OnboardingViewModel(
            playerRepository = repos.playerRepository,
            settingsRepository = repos.settingsRepository,
            onboardingStore = onboardingStore,
            rematchStore = rematchStore,
            logger = NoopAppLogger,
            firebaseBootstrap = FirebaseBootstrap(context),
        )
    }

    @Test
    fun skipFromWelcome_jumpsToReady() = runTest {
        viewModel.initialize(OnboardingMode.FirstLaunch)
        advanceUntilIdle()

        viewModel.skipFromWelcome()

        assertEquals(OnboardingStep.Ready, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.skippedFromWelcome)
    }

    @Test
    fun advanceFromWelcome_skipsRosterWhenPrimaryExists() = runTest {
        repos.seedHumanPlayer()
        viewModel.initialize(OnboardingMode.Replay)
        advanceUntilIdle()

        viewModel.advanceFromWelcome()

        assertFalse(viewModel.uiState.value.step == OnboardingStep.RosterSetup)
        assertTrue(viewModel.uiState.value.skipsRosterSetup)
    }

    @Test
    fun complete_replayDoesNotMarkOnboardingCompleted() = runTest {
        onboardingStore.clear()
        viewModel.initialize(OnboardingMode.Replay)
        advanceUntilIdle()

        var finished = false
        viewModel.complete { finished = true }
        advanceUntilIdle()

        assertTrue(finished)
        assertTrue(onboardingStore.shouldPresentOnLaunch)
    }

    @Test
    fun complete_firstLaunchMarksCompleted() = runTest {
        onboardingStore.clear()
        viewModel.initialize(OnboardingMode.FirstLaunch)
        advanceUntilIdle()
        viewModel.skipFromWelcome()

        viewModel.complete { }
        advanceUntilIdle()

        assertFalse(onboardingStore.shouldPresentOnLaunch)
    }
}
