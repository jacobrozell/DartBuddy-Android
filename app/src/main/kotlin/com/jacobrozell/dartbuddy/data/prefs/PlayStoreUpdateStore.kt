package com.jacobrozell.dartbuddy.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayStoreUpdateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun dismissedStoreVersion(): String? = prefs.getString(KEY_DISMISSED_VERSION, null)

    fun recordDismissal(storeVersion: String) {
        prefs.edit().putString(KEY_DISMISSED_VERSION, storeVersion).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_DISMISSED_VERSION).apply()
    }

    companion object {
        private const val PREFS_NAME = "dart_buddy_play_store_update"
        private const val KEY_DISMISSED_VERSION = "play_store_update_dismissed_version"
    }
}
