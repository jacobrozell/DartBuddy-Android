package com.jacobrozell.dartbuddy.support.test

import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.LogLevel

object NoopAppLogger : AppLogger {
    override fun log(
        level: LogLevel,
        category: LogCategory,
        eventName: String,
        message: String,
        metadata: Map<String, String>,
        correlationId: String?,
    ) = Unit
}
