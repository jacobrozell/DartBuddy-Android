package com.jacobrozell.dartbuddy

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.bootstrap.AppBootstrapper
import com.jacobrozell.dartbuddy.bootstrap.DemoSeedExtras
import com.jacobrozell.dartbuddy.bootstrap.DemoSeeder
import com.jacobrozell.dartbuddy.bootstrap.hasSeedExtras
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingMode
import com.jacobrozell.dartbuddy.support.navigation.DeepLinkParser
import com.jacobrozell.dartbuddy.ui.navigation.DartBuddyNavHost
import com.jacobrozell.dartbuddy.ui.onboarding.OnboardingFlow
import com.jacobrozell.dartbuddy.ui.layout.SyncSystemBarAppearance
import com.jacobrozell.dartbuddy.ui.theme.DartBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var onboardingStore: OnboardingStore
    @Inject lateinit var demoSeeder: DemoSeeder
    @Inject lateinit var appBootstrapper: AppBootstrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasSeedExtras()) {
            runBlocking {
                demoSeeder.seedIfRequested(intent)
                demoSeeder.enqueueTrainingPracticeIfRequested(intent)
            }
        } else {
            lifecycleScope.launch {
                demoSeeder.seedIfRequested(intent)
                demoSeeder.enqueueTrainingPracticeIfRequested(intent)
            }
        }
        if (intent.getBooleanExtra(DemoSeedExtras.SKIP_ONBOARDING, false)) {
            onboardingStore.markCompleted()
        }
        lifecycleScope.launch {
            appBootstrapper.onLaunch()
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val rootViewModel: AppRootViewModel = hiltViewModel()
            val settings by rootViewModel.settings.collectAsStateWithLifecycle()
            var showOnboarding by remember { mutableStateOf(onboardingStore.shouldPresentOnLaunch) }
            var onboardingMode by remember {
                mutableStateOf(
                    if (onboardingStore.shouldPresentOnLaunch) OnboardingMode.FirstLaunch else OnboardingMode.Replay,
                )
            }
            var pendingDeepLink by remember { mutableStateOf(deepLinkPathFrom(intent)) }

            DisposableEffect(onboardingStore) {
                val listener = {
                    onboardingMode = OnboardingMode.Replay
                    showOnboarding = true
                }
                onboardingStore.addReplayListener(listener)
                onDispose { onboardingStore.removeReplayListener(listener) }
            }

            val darkTheme = when (settings.appearanceMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            DartBuddyTheme(darkTheme = darkTheme) {
                SyncSystemBarAppearance(darkTheme = darkTheme)
                Surface(modifier = Modifier.fillMaxSize()) {
                    DartBuddyNavHost(
                        deepLinkPath = if (showOnboarding) null else pendingDeepLink,
                        onDeepLinkConsumed = { pendingDeepLink = null },
                    )
                }
                if (showOnboarding) {
                    OnboardingFlow(
                        mode = onboardingMode,
                        onFinished = { showOnboarding = false },
                    )
                }
            }
        }
        setTheme(R.style.Theme_DartBuddy)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun deepLinkPathFrom(intent: Intent?): String? =
        DeepLinkParser.pathFrom(intent?.data)
}
