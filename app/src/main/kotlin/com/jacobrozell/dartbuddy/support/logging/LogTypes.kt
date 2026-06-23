package com.jacobrozell.dartbuddy.support.logging

import java.util.Date

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FAULT,
    ;

    fun atLeast(minimum: LogLevel): Boolean = ordinal >= minimum.ordinal
}

enum class LogCategory(val rawValue: String) {
    UI("ui"),
    SCORING("scoring"),
    PERSISTENCE("persistence"),
    MIGRATION("migration"),
    NETWORK("network"),
    VISION("vision"),
    WATCH("watch"),
    SECURITY("security"),
    SETTINGS("settings"),
    APP_LIFECYCLE("appLifecycle"),
}

data class LogEntry(
    val timestamp: Date,
    val level: LogLevel,
    val category: LogCategory,
    val eventName: String,
    val message: String,
    val metadata: Map<String, String>,
    val correlationId: String? = null,
)
