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
import com.jacobrozell.dartbuddy.support.logging.AppLogger
import com.jacobrozell.dartbuddy.support.logging.LogCategory
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
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    val practiceSetupStore: PracticeSetupStore,
    private val playStoreUpdateChecker: PlayStoreUpdateChecker,
    private val logger: AppLogger,
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

    suspend fun resolveActiveMatchRoute(matchId: String): String? {
        val entity = matchRepository.getMatch(matchId) ?: return null
        if (MatchStatus.valueOf(entity.status) != MatchStatus.InProgress) return null
        return "match/${entity.matchType.lowercase()}/$matchId"
    }

    suspend fun playerDetailRoute(playerId: String): String? =
        if (playerRepository.getById(playerId) != null) "players/$playerId" else null
}
