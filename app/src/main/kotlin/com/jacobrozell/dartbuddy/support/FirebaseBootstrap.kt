package com.jacobrozell.dartbuddy.support

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jacobrozell.dartbuddy.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseBootstrap @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var analytics: FirebaseAnalytics? = null

    var isAnalyticsCollectionEnabled: Boolean = false
        private set
    var isCrashlyticsCollectionEnabled: Boolean = false
        private set

    val shouldConfigure: Boolean
        get() = analytics != null || isCrashlyticsCollectionEnabled

    fun initialize(app: Application) {
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }
        val enabled = !BuildConfig.DEBUG
        isAnalyticsCollectionEnabled = enabled
        isCrashlyticsCollectionEnabled = enabled
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
        analytics = if (enabled) FirebaseAnalytics.getInstance(app) else null
    }

    fun analyticsInstance(): FirebaseAnalytics? = analytics
}
