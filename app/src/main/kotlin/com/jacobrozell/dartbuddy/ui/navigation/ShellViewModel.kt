package com.jacobrozell.dartbuddy.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    matchRepository: MatchRepository,
) : ViewModel() {
    val activeMatch: StateFlow<MatchEntity?> = matchRepository.observeActiveMatch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
