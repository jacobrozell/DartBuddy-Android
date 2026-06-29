package com.jacobrozell.dartbuddy.support.diagnostics

import android.content.Context
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.jacobrozell.dartbuddy.support.MotionPreferences
import java.util.Locale

data class ClientEnvironmentSnapshot(
    val deviceClass: String,
    val isVoiceOverRunning: Boolean,
    val isSwitchControlRunning: Boolean,
    val isBoldTextEnabled: Boolean,
    val isReduceMotionEnabled: Boolean,
    val isScreenCaptured: Boolean,
    val isExternalDisplayConnected: Boolean,
    val interfaceOrientation: String,
    val contentSizeCategory: String,
    val colorScheme: String,
    val isLowPowerModeEnabled: Boolean,
) {
    val analyticsMetadata: Map<String, String> = mapOf(
        "deviceClass" to deviceClass,
        "isVoiceOverRunning" to boolString(isVoiceOverRunning),
        "isSwitchControlRunning" to boolString(isSwitchControlRunning),
        "isBoldTextEnabled" to boolString(isBoldTextEnabled),
        "isReduceMotionEnabled" to boolString(isReduceMotionEnabled),
        "isScreenCaptured" to boolString(isScreenCaptured),
        "isExternalDisplayConnected" to boolString(isExternalDisplayConnected),
        "interfaceOrientation" to interfaceOrientation,
        "contentSizeCategory" to contentSizeCategory,
        "colorScheme" to colorScheme,
        "isLowPowerModeEnabled" to boolString(isLowPowerModeEnabled),
    )

    companion object {
        fun current(context: Context): ClientEnvironmentSnapshot {
            val resources = context.resources
            val configuration = resources.configuration
            val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return ClientEnvironmentSnapshot(
                deviceClass = deviceClass(configuration),
                isVoiceOverRunning = accessibility.isTouchExplorationEnabled,
                isSwitchControlRunning = accessibility.isEnabled && !accessibility.isTouchExplorationEnabled,
                isBoldTextEnabled = isBoldTextEnabled(context),
                isReduceMotionEnabled = MotionPreferences.prefersReducedMotion(context),
                isScreenCaptured = false,
                isExternalDisplayConnected = false,
                interfaceOrientation = orientationLabel(configuration),
                contentSizeCategory = contentSizeBucket(configuration.fontScale),
                colorScheme = colorSchemeLabel(configuration),
                isLowPowerModeEnabled = powerManager.isPowerSaveMode,
            )
        }

        fun changedSignals(from: ClientEnvironmentSnapshot, to: ClientEnvironmentSnapshot): String {
            val changes = mutableListOf<String>()
            if (from.isVoiceOverRunning != to.isVoiceOverRunning) changes += "voiceover"
            if (from.isSwitchControlRunning != to.isSwitchControlRunning) changes += "switchControl"
            if (from.isBoldTextEnabled != to.isBoldTextEnabled) changes += "boldText"
            if (from.isReduceMotionEnabled != to.isReduceMotionEnabled) changes += "reduceMotion"
            if (from.isScreenCaptured != to.isScreenCaptured) changes += "screenCapture"
            if (from.isExternalDisplayConnected != to.isExternalDisplayConnected) changes += "display"
            if (from.deviceClass != to.deviceClass) changes += "deviceClass"
            if (from.interfaceOrientation != to.interfaceOrientation) changes += "orientation"
            if (from.contentSizeCategory != to.contentSizeCategory) changes += "contentSize"
            if (from.colorScheme != to.colorScheme) changes += "colorScheme"
            if (from.isLowPowerModeEnabled != to.isLowPowerModeEnabled) changes += "lowPowerMode"
            return changes.joinToString(",")
        }

        private fun boolString(value: Boolean): String = if (value) "true" else "false"

        private fun deviceClass(configuration: Configuration): String =
            if (configuration.smallestScreenWidthDp >= 600) "tablet" else "phone"

        private fun orientationLabel(configuration: Configuration): String =
            when (configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> "landscape"
                Configuration.ORIENTATION_PORTRAIT -> "portrait"
                else -> "unknown"
            }

        private fun contentSizeBucket(fontScale: Float): String =
            if (fontScale > 1.15f) "accessibility" else "standard"

        private fun colorSchemeLabel(configuration: Configuration): String =
            when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> "dark"
                Configuration.UI_MODE_NIGHT_NO -> "light"
                else -> "unspecified"
            }

        private fun isBoldTextEnabled(context: Context): Boolean =
            runCatching {
                Settings.Secure.getInt(context.contentResolver, "font_weight_adjustment", 0) != 0
            }.getOrDefault(false)
    }
}

object ClientEnvironment {
    fun snapshot(context: Context): ClientEnvironmentSnapshot = ClientEnvironmentSnapshot.current(context)
}
