package com.jacobrozell.dartbuddy.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.prefs.PracticeSetupStore
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
import com.jacobrozell.dartbuddy.support.logging.MatchAnalytics
import com.jacobrozell.dartbuddy.support.logging.info
import com.jacobrozell.dartbuddy.support.store.PlayStoreUpdateChecker
import com.jacobrozell.dartbuddy.support.store.PlayStoreUpdateOffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    val practiceSetupStore: PracticeSetupStore,
    private val playStoreUpdateChecker: PlayStoreUpdateChecker,
    private val logger: AppLogger,
    private val firebaseBootstrap: FirebaseBootstrap,
    private val json: Json,
) : ViewModel() {
    val activeMatch: StateFlow<MatchEntity?> = matchRepository.observeActiveMatch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _updateOffer = MutableStateFlow<PlayStoreUpdateOffer?>(null)
    val updateOffer: StateFlow<PlayStoreUpdateOffer?> = _updateOffer.asStateFlow()

    fun logTabPresented(tab: String) {
        logger.info(
            LogCategory.UI,
            eventName = "main_tab_presented",
            message = "Main tab presented.",
            metadata = mapOf("tab" to tab),
        )
    }

    fun checkForPlayStoreUpdate() {
        viewModelScope.launch {
            if (_updateOffer.value != null) return@launch
            _updateOffer.value = playStoreUpdateChecker.checkForUpdate()
        }
    }

    fun dismissPlayStoreUpdate() {
        _updateOffer.value?.let(playStoreUpdateChecker::recordDismissal)
        _updateOffer.value = null
    }

    fun playStoreUpdateIntent(): Intent? {
        val url = _updateOffer.value?.storeUrl ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    fun logDeepLinkReceived() {
        logger.info(
            LogCategory.UI,
            eventName = "deep_link_received",
            message = "Applying deep link.",
            metadata = mapOf("version" to "v1"),
        )
    }

    fun logDeepLinkApplied() {
        logger.info(
            LogCategory.UI,
            eventName = "deep_link_applied",
            message = "Deep link routed.",
            metadata = mapOf("version" to "v1"),
        )
    }

    fun logMatchResumed(match: MatchEntity, startSource: String) {
        viewModelScope.launch {
            val ids = json.decodeFromString<List<String>>(match.participantIdsJson)
            val players = ids.mapNotNull { playerRepository.getById(it) }
            MatchAnalytics.logResumed(
                logger = logger,
                firebaseBootstrap = firebaseBootstrap,
                entity = match,
                json = json,
                players = players,
                startSource = startSource,
            )
        }
    }

    fun logMatchResumedById(matchId: String, startSource: String) {
        viewModelScope.launch {
            val match = matchRepository.getMatch(matchId) ?: return@launch
            logMatchResumed(match, startSource)
        }
    }

    suspend fun resolveActiveMatchRoute(matchId: String): String? {
        val entity = matchRepository.getMatch(matchId) ?: return null
        if (MatchStatus.valueOf(entity.status) != MatchStatus.InProgress) return null
        return "match/${entity.matchType.lowercase()}/$matchId"
    }

    suspend fun playerDetailRoute(playerId: String): String? =
        if (playerRepository.getById(playerId) != null) "players/$playerId" else null
}
