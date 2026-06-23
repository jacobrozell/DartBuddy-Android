package com.jacobrozell.dartbuddy.domain.error

class AppError(
    val code: AppErrorCode,
    val userMessageKey: String,
    val isRecoverable: Boolean = true,
) : Exception(userMessageKey)

enum class AppErrorCode {
    ValidationFailed,
    InvalidGameState,
}
