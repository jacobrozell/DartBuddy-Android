package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.BuildConfig
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.domain.surface.ProductSurface
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import java.util.Locale

/// Privacy-safe user properties for retention and product-health segmentation.
object AnalyticsUserContext {
    fun userPropertyValues(
        settings: AppSettings?,
        onboardingComplete: Boolean,
    ): Map<String, String> {
        val values = mutableMapOf(
            "onboarding_complete" to boolString(onboardingComplete),
            "app_locale" to appLocaleCode(),
            "product_surface" to ProductSurface.analyticsLabel,
            "build_number" to BuildConfig.VERSION_CODE.toString(),
        )
        settings?.let {
            values["appearance_mode"] = it.appearanceMode
            values["haptics_enabled"] = boolString(it.hapticsEnabled)
            values["sound_enabled"] = boolString(it.soundEnabled)
            values["turn_caller_enabled"] = boolString(it.turnTotalCallerEnabled)
            values["bot_stagger_enabled"] = boolString(it.botStaggerEnabled)
            values["bot_dart_haptics_enabled"] = boolString(it.botDartHapticsEnabled)
            values["default_match_type"] = it.defaultMatchType.name.lowercase()
        }
        return values
    }

    fun syncFromBootstrap(
        firebaseBootstrap: FirebaseBootstrap,
        settings: AppSettings?,
        onboardingComplete: Boolean,
    ) {
        sync(firebaseBootstrap, settings, onboardingComplete)
    }

    fun syncOnboardingCompleted(
        firebaseBootstrap: FirebaseBootstrap,
        settings: AppSettings?,
    ) {
        sync(firebaseBootstrap, settings, onboardingComplete = true)
    }

    fun syncAfterSettingsApply(
        firebaseBootstrap: FirebaseBootstrap,
        settings: AppSettings,
        onboardingComplete: Boolean,
    ) {
        sync(firebaseBootstrap, settings, onboardingComplete)
    }

    fun sync(
        firebaseBootstrap: FirebaseBootstrap,
        settings: AppSettings?,
        onboardingComplete: Boolean,
    ) {
        if (!firebaseBootstrap.shouldConfigure || !firebaseBootstrap.isAnalyticsCollectionEnabled) return
        val analytics = firebaseBootstrap.analyticsInstance() ?: return
        userPropertyValues(settings, onboardingComplete).forEach { (name, value) ->
            analytics.setUserProperty(value, name)
        }
    }

    private fun appLocaleCode(): String {
        val language = Locale.getDefault().language
        return language.ifBlank { "unknown" }
    }

    private fun boolString(value: Boolean): String = if (value) "true" else "false"
}
