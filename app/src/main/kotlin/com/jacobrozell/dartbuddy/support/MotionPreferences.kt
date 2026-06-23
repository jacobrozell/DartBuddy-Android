package com.jacobrozell.dartbuddy.support

import android.content.Context
import android.provider.Settings

object MotionPreferences {
    fun prefersReducedMotion(context: Context): Boolean {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}
