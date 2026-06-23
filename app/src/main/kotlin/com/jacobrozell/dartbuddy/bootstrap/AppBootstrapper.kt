package com.jacobrozell.dartbuddy.bootstrap

import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.logging.AnalyticsUserIdentity
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.info
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBootstrapper @Inject constructor(
    private val logger: AppLogger,
    private val playerRepository: PlayerRepository,
    private val firebaseBootstrap: FirebaseBootstrap,
) {
    private var didLogReady = false

    suspend fun onLaunch() {
        AnalyticsUserIdentity.sync(
            firebaseBootstrap,
            playerRepository.getPrimaryPlayer()?.id,
        )
        if (!didLogReady) {
            didLogReady = true
            logger.info(
                LogCategory.APP_LIFECYCLE,
                eventName = "app_bootstrap_ready",
                message = "Dart Buddy bootstrap ready.",
            )
        }
    }
}
