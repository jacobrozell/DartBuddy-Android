package com.jacobrozell.dartbuddy.data.prefs

import com.jacobrozell.dartbuddy.domain.model.MatchType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeSetupStore @Inject constructor() {
    var pendingHumanId: String? = null
    var pendingTrainingBotId: String? = null
    var pendingMatchType: MatchType? = null

    fun enqueuePractice(humanId: String, trainingBotId: String, mode: MatchType) {
        pendingHumanId = humanId
        pendingTrainingBotId = trainingBotId
        pendingMatchType = mode
    }

    fun consume(): PracticeSetup? {
        val human = pendingHumanId ?: return null
        val bot = pendingTrainingBotId ?: return null
        val mode = pendingMatchType ?: return null
        clear()
        return PracticeSetup(human, bot, mode)
    }

    fun clear() {
        pendingHumanId = null
        pendingTrainingBotId = null
        pendingMatchType = null
    }
}

data class PracticeSetup(
    val humanId: String,
    val trainingBotId: String,
    val mode: MatchType,
)
