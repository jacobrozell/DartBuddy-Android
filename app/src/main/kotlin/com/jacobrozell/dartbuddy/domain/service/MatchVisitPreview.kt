package com.jacobrozell.dartbuddy.domain.service

object MatchVisitPreview {
    fun includesActiveVisit(
        isActive: Boolean,
        canHumanInput: Boolean,
        isBotPlaying: Boolean,
        isCurrentPlayerBot: Boolean,
    ): Boolean {
        if (!isActive) return false
        return canHumanInput || isBotPlaying || isCurrentPlayerBot
    }
}
