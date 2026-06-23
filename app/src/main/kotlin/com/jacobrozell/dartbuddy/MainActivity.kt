package com.jacobrozell.dartbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingMode
import com.jacobrozell.dartbuddy.ui.navigation.DartBuddyNavHost
import com.jacobrozell.dartbuddy.ui.onboarding.OnboardingFlow
import com.jacobrozell.dartbuddy.ui.theme.DartBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rootViewModel: AppRootViewModel = hiltViewModel()
            val settings by rootViewModel.settings.collectAsStateWithLifecycle()
            var showOnboarding by remember { mutableStateOf(onboardingStore.shouldPresentOnLaunch) }
            var onboardingMode by remember {
                mutableStateOf(
                    if (onboardingStore.shouldPresentOnLaunch) OnboardingMode.FirstLaunch else OnboardingMode.Replay,
                )
            }
            var pendingDeepLink by remember { mutableStateOf(intent?.data?.path) }

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
}
