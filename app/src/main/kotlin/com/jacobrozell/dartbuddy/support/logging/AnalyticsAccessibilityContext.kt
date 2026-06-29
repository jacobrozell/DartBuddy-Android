package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.diagnostics.ClientEnvironmentSnapshot

/// User-scoped accessibility signals for product-health segmentation.
object AnalyticsAccessibilityContext {
    fun userPropertyValues(snapshot: ClientEnvironmentSnapshot): Map<String, String> = mapOf(
        "voiceover_enabled" to boolString(snapshot.isVoiceOverRunning),
        "switch_control_enabled" to boolString(snapshot.isSwitchControlRunning),
        "content_size_category" to snapshot.contentSizeCategory,
        "reduce_motion_enabled" to boolString(snapshot.isReduceMotionEnabled),
        "bold_text_enabled" to boolString(snapshot.isBoldTextEnabled),
    )

    fun sync(firebaseBootstrap: FirebaseBootstrap, snapshot: ClientEnvironmentSnapshot) {
        if (!firebaseBootstrap.shouldConfigure || !firebaseBootstrap.isAnalyticsCollectionEnabled) return
        val analytics = firebaseBootstrap.analyticsInstance() ?: return
        userPropertyValues(snapshot).forEach { (name, value) ->
            analytics.setUserProperty(value, name)
        }
    }

    private fun boolString(value: Boolean): String = if (value) "true" else "false"
}
