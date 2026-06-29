package com.jacobrozell.dartbuddy.support.diagnostics

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.view.accessibility.AccessibilityManager
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.logging.AnalyticsAccessibilityContext
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.info
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientEnvironmentMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseBootstrap: FirebaseBootstrap,
) {
    private var lastSnapshot: ClientEnvironmentSnapshot? = null
    private var started = false
    private var touchExplorationListener: AccessibilityManager.TouchExplorationStateChangeListener? = null
    private var componentCallbacks: ComponentCallbacks? = null

    fun startReportingChanges(logger: AppLogger) {
        if (started) return
        started = true

        lastSnapshot = ClientEnvironment.snapshot(context)
        AnalyticsAccessibilityContext.sync(firebaseBootstrap, lastSnapshot!!)

        val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        touchExplorationListener = AccessibilityManager.TouchExplorationStateChangeListener {
            reportChangeIfNeeded(logger, "voiceover")
        }
        accessibility.addTouchExplorationStateChangeListener(touchExplorationListener!!)

        componentCallbacks = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                reportChangeIfNeeded(logger, "configuration")
            }

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = Unit
        }
        context.registerComponentCallbacks(componentCallbacks!!)
    }

    private fun reportChangeIfNeeded(logger: AppLogger, trigger: String) {
        val current = ClientEnvironment.snapshot(context)
        val previous = lastSnapshot
        if (previous != null && previous == current) return

        val metadata = current.analyticsMetadata.toMutableMap()
        metadata["trigger"] = trigger
        if (previous != null) {
            metadata["changedSignals"] = ClientEnvironmentSnapshot.changedSignals(previous, current)
        }

        logger.info(
            LogCategory.APP_LIFECYCLE,
            eventName = "client_environment_changed",
            message = "Client environment context changed.",
            metadata = metadata,
        )
        AnalyticsAccessibilityContext.sync(firebaseBootstrap, current)
        lastSnapshot = current
    }
}
