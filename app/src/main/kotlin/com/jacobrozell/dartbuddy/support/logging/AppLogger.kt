package com.jacobrozell.dartbuddy.support.logging

import com.jacobrozell.dartbuddy.support.FirebaseBootstrap

interface AppLogger {
    fun log(
        level: LogLevel,
        category: LogCategory,
        eventName: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        correlationId: String? = null,
    )
}

fun AppLogger.debug(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
    correlationId: String? = null,
) = log(LogLevel.DEBUG, category, eventName, message, metadata, correlationId)

fun AppLogger.info(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
    correlationId: String? = null,
) = log(LogLevel.INFO, category, eventName, message, metadata, correlationId)

fun AppLogger.warning(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
    correlationId: String? = null,
) = log(LogLevel.WARNING, category, eventName, message, metadata, correlationId)

fun AppLogger.error(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
    correlationId: String? = null,
) = log(LogLevel.ERROR, category, eventName, message, metadata, correlationId)

class DefaultAppLogger(
    private val minimumLevel: LogLevel,
    private val sink: LogSink,
) : AppLogger {
    override fun log(
        level: LogLevel,
        category: LogCategory,
        eventName: String,
        message: String,
        metadata: Map<String, String>,
        correlationId: String?,
    ) {
        if (!level.atLeast(minimumLevel)) return
        val sanitized = AnalyticsMetadataKeys.withoutPersonalData(metadata)
        sink.write(
            LogEntry(
                timestamp = java.util.Date(),
                level = level,
                category = category,
                eventName = eventName,
                message = message,
                metadata = sanitized,
                correlationId = correlationId,
            ),
        )
    }

    companion object {
        fun create(firebaseBootstrap: FirebaseBootstrap, appVersion: String?): DefaultAppLogger {
            val remote = FilteredLogSink(
                minimumLevel = LogLevel.INFO,
                wrapped = FirebaseAnalyticsLogSink(firebaseBootstrap, appVersion),
            )
            val sink = CompositeLogSink(
                listOf(
                    ConsoleLogSink(),
                    remote,
                    FirebaseCrashlyticsLogSink(firebaseBootstrap, appVersion),
                ),
            )
            val minimum = if (com.jacobrozell.dartbuddy.BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
            return DefaultAppLogger(minimum, sink)
        }
    }
}
