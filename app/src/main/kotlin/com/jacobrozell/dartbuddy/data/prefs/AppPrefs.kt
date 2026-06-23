package com.jacobrozell.dartbuddy.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val shouldPresentOnLaunch: Boolean
        get() = !prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun savedExperienceTierRaw(): String? = prefs.getString(KEY_EXPERIENCE_TIER, null)

    fun saveExperienceTier(tier: com.jacobrozell.dartbuddy.domain.bots.BotDifficulty) {
        prefs.edit().putString(KEY_EXPERIENCE_TIER, tier.rawValue).apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_COMPLETED)
            .remove(KEY_EXPERIENCE_TIER)
            .apply()
    }

    private val replayListeners = mutableListOf<() -> Unit>()

    fun addReplayListener(listener: () -> Unit) {
        replayListeners.add(listener)
    }

    fun removeReplayListener(listener: () -> Unit) {
        replayListeners.remove(listener)
    }

    fun requestReplay() {
        replayListeners.forEach { it.invoke() }
    }

    companion object {
        private const val PREFS_NAME = "dart_buddy_onboarding"
        private const val KEY_COMPLETED = "onboarding_completed"
        private const val KEY_EXPERIENCE_TIER = "onboarding_experience_tier"
    }
}

@Singleton
class RematchStore @Inject constructor() {
    var pendingPlayerIds: List<String>? = null
    var pendingMatchType: String? = null

    fun clear() {
        pendingPlayerIds = null
        pendingMatchType = null
    }
}
