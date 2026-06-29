package com.jacobrozell.dartbuddy.bootstrap

import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.diagnostics.ClientEnvironment
import com.jacobrozell.dartbuddy.support.diagnostics.ClientEnvironmentMonitor
import com.jacobrozell.dartbuddy.support.logging.AnalyticsAccessibilityContext
import com.jacobrozell.dartbuddy.support.logging.AnalyticsUserContext
import com.jacobrozell.dartbuddy.support.logging.AnalyticsUserIdentity
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.info
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBootstrapper @Inject constructor(
    private val logger: AppLogger,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val onboardingStore: OnboardingStore,
    private val firebaseBootstrap: FirebaseBootstrap,
    private val clientEnvironmentMonitor: ClientEnvironmentMonitor,
    @ApplicationContext private val context: Context,
) {
    private var didLogReady = false

    suspend fun onLaunch() {
        AnalyticsUserIdentity.sync(
            firebaseBootstrap,
            playerRepository.getPrimaryPlayer()?.id,
        )
        val settings = runCatching { settingsRepository.get() }.getOrNull()
        AnalyticsUserContext.syncFromBootstrap(
            firebaseBootstrap,
            settings,
            onboardingComplete = !onboardingStore.shouldPresentOnLaunch,
        )
        val environmentSnapshot = ClientEnvironment.snapshot(context)
        AnalyticsAccessibilityContext.sync(firebaseBootstrap, environmentSnapshot)
        clientEnvironmentMonitor.startReportingChanges(logger)
        if (!didLogReady) {
            didLogReady = true
            logger.info(
                LogCategory.APP_LIFECYCLE,
                eventName = "app_bootstrap_ready",
                message = "Dart Buddy bootstrap ready.",
                metadata = environmentSnapshot.analyticsMetadata,
            )
        }
    }
}
