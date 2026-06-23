package com.jacobrozell.dartbuddy.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.prefs.RematchStore
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingMode
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingRosterDraft
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingStep
import com.jacobrozell.dartbuddy.domain.onboarding.showsOnboardingRulesIntro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val mode: OnboardingMode = OnboardingMode.FirstLaunch,
    val rosterDraft: OnboardingRosterDraft? = null,
    val skippedFromWelcome: Boolean = false,
    val skipsRosterSetup: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isFinishing: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val onboardingStore: OnboardingStore,
    private val rematchStore: RematchStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun initialize(mode: OnboardingMode) {
        _uiState.update {
            OnboardingUiState(step = OnboardingStep.Welcome, mode = mode)
        }
        viewModelScope.launch {
            val settings = settingsRepository.get()
            _uiState.update { it.copy(settings = settings) }
            val primary = playerRepository.getPrimaryPlayer()
            if (primary != null) {
                val tier = onboardingStore.savedExperienceTierRaw()
                    ?.let { BotDifficulty.fromRawValue(it) }
                    ?: BotDifficulty.Medium
                _uiState.update {
                    it.copy(
                        skipsRosterSetup = true,
                        rosterDraft = OnboardingRosterDraft(
                            name = primary.name,
                            colorToken = PlayerColorToken.resolve(primary.colorToken),
                            botDifficulty = tier,
                        ),
                    )
                }
            }
        }
    }

    fun advanceFromWelcome() {
        _uiState.update { state ->
            val next = if (state.skipsRosterSetup && state.rosterDraft != null) {
                nextStepAfterRoster(state.rosterDraft!!)
            } else {
                OnboardingStep.RosterSetup
            }
            state.copy(step = next)
        }
    }

    fun skipFromWelcome() {
        _uiState.update { it.copy(skippedFromWelcome = true, step = OnboardingStep.Ready) }
    }

    fun setRosterDraft(draft: OnboardingRosterDraft) {
        _uiState.update {
            it.copy(
                rosterDraft = draft,
                step = if (draft.botDifficulty.showsOnboardingRulesIntro) {
                    OnboardingStep.LearnToPlay
                } else {
                    OnboardingStep.Preferences
                },
            )
        }
    }

    fun goToAppTour() {
        _uiState.update { it.copy(step = OnboardingStep.AppTour) }
    }

    fun goToSupport() {
        _uiState.update { it.copy(step = OnboardingStep.Support) }
    }

    fun goToReady() {
        _uiState.update { it.copy(step = OnboardingStep.Ready) }
    }

    fun goBack() {
        _uiState.update { state ->
            val previous = state.step.backStep(
                showsRulesIntro = state.rosterDraft?.botDifficulty?.showsOnboardingRulesIntro,
                skipsRosterSetup = state.skipsRosterSetup,
            ) ?: return@update state
            state.copy(step = previous)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = transform(settingsRepository.get())
            settingsRepository.save(updated)
            _uiState.update { it.copy(settings = updated) }
        }
    }

    fun complete(onFinished: () -> Unit) {
        val state = _uiState.value
        if (state.isFinishing) return
        _uiState.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            if (state.mode == OnboardingMode.FirstLaunch) {
                if (!state.skippedFromWelcome) {
                    state.rosterDraft?.let { persistRoster(it) }
                }
                onboardingStore.markCompleted()
            }
            _uiState.update { it.copy(isFinishing = false) }
            onFinished()
        }
    }

    private suspend fun persistRoster(draft: OnboardingRosterDraft) {
        val human = playerRepository.create(
            name = draft.name,
            colorToken = draft.colorToken.raw,
            isPrimary = true,
        )
        val bot = playerRepository.createPresetBot(draft.botDifficulty)
        onboardingStore.saveExperienceTier(draft.botDifficulty)
        rematchStore.pendingPlayerIds = listOf(human.id, bot.id)
        if (draft.botDifficulty.showsOnboardingRulesIntro) {
            applyBeginnerGameplayDefaults()
        }
    }

    private suspend fun applyBeginnerGameplayDefaults() {
        val current = settingsRepository.get()
        if (current.defaultMatchType == MatchType.X01) return
        settingsRepository.save(current.copy(defaultMatchType = MatchType.X01))
        _uiState.update { it.copy(settings = settingsRepository.get()) }
    }

    private fun nextStepAfterRoster(draft: OnboardingRosterDraft): OnboardingStep =
        if (draft.botDifficulty.showsOnboardingRulesIntro) OnboardingStep.LearnToPlay else OnboardingStep.Preferences
}
