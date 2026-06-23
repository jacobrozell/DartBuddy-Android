package com.jacobrozell.dartbuddy.support.logging

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap

interface LogSink {
    fun write(entry: LogEntry)
}

class ConsoleLogSink : LogSink {
    override fun write(entry: LogEntry) {
        val tag = "DartBuddy/${entry.category.rawValue}"
        val line = "[${entry.eventName}] ${entry.message}"
        when (entry.level) {
            LogLevel.DEBUG -> Log.d(tag, line)
            LogLevel.INFO -> Log.i(tag, line)
            LogLevel.WARNING -> Log.w(tag, line)
            LogLevel.ERROR -> Log.e(tag, line)
            LogLevel.FAULT -> Log.wtf(tag, line)
        }
    }
}

class FirebaseAnalyticsLogSink(
    private val firebaseBootstrap: FirebaseBootstrap,
    private val appVersion: String?,
) : LogSink {
    override fun write(entry: LogEntry) {
        if (!firebaseBootstrap.isAnalyticsCollectionEnabled) return
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion) ?: return
        val bundle = android.os.Bundle().apply {
            event.parameters.forEach { (key, value) -> putString(key, value) }
        }
        firebaseBootstrap.analyticsInstance()?.logEvent(event.name, bundle)
    }
}

class FirebaseCrashlyticsLogSink(
    private val firebaseBootstrap: FirebaseBootstrap,
    private val appVersion: String?,
) : LogSink {
    override fun write(entry: LogEntry) {
        if (!firebaseBootstrap.isCrashlyticsCollectionEnabled) return
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (entry.level.atLeast(LogLevel.INFO)) {
            crashlytics.log("[${entry.category.rawValue}] ${entry.eventName}")
        }
        FirebaseCrashlyticsEventMapping.nonFatalError(entry, appVersion)?.let { error ->
            crashlytics.recordException(error)
        }
    }
}

class CompositeLogSink(private val sinks: List<LogSink>) : LogSink {
    override fun write(entry: LogEntry) {
        sinks.forEach { it.write(entry) }
    }
}

class FilteredLogSink(
    private val minimumLevel: LogLevel,
    private val wrapped: LogSink,
) : LogSink {
    override fun write(entry: LogEntry) {
        if (entry.level.atLeast(minimumLevel)) {
            wrapped.write(entry)
        }
    }
}
