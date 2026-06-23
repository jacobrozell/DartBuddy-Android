package com.jacobrozell.dartbuddy.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.data.repository.AppSettings
import com.jacobrozell.dartbuddy.data.repository.LocalDataResetService
import com.jacobrozell.dartbuddy.data.repository.SettingsRepository
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01StartScores
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import androidx.compose.foundation.layout.Row
import com.jacobrozell.dartbuddy.ui.designsystem.BrandRootScreenTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.rememberOptionLabelMaps
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.designsystem.TabRootScaffold
import com.jacobrozell.dartbuddy.ui.play.rules.GameRulesSheet
import com.jacobrozell.dartbuddy.support.state.AppAppearancePolicy
import com.jacobrozell.dartbuddy.ui.theme.Brand
import com.jacobrozell.dartbuddy.ui.theme.DartBuddyTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class AppearanceOption(val raw: String, val labelRes: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localDataResetService: LocalDataResetService,
    private val onboardingStore: OnboardingStore,
) : ViewModel() {
    val settings = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.save(transform(settingsRepository.get()))
        }
    }

    fun resetAll(onComplete: () -> Unit) {
        viewModelScope.launch {
            localDataResetService.resetAllLocalData()
            onComplete()
        }
    }

    fun replayOnboarding() {
        onboardingStore.requestReplay()
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showResetConfirm by remember { mutableStateOf(false) }
    var showReplayConfirm by remember { mutableStateOf(false) }
    var showRulesSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val labels = rememberOptionLabelMaps()
    val appearanceOptions = listOf(
        AppearanceOption("system", R.string.settings_appearance_system),
        AppearanceOption("light", R.string.settings_appearance_light),
        AppearanceOption("dark", R.string.settings_appearance_dark),
    )
    val appearanceLabels = appearanceOptions.associate { it.raw to stringResource(it.labelRes) }
    val usesBrandSettingsChrome = AppAppearancePolicy.settingsUsesBrandPalette(settings.appearanceMode)

    DartBuddyTheme(darkTheme = usesBrandSettingsChrome || isSystemInDarkTheme()) {
    TabRootScaffold(forceBrandDarkBackground = usesBrandSettingsChrome) {
        Column(Modifier.a11yId(AccessibilityIds.SETTINGS_FORM)) {
        BrandRootScreenTitle(title = stringResource(R.string.settings_title))

        Text(stringResource(R.string.settings_appearance), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            HorizontalOptionChips(
                label = stringResource(R.string.settings_appearance),
                options = appearanceOptions,
                selected = appearanceOptions.first { it.raw == settings.appearanceMode },
                onSelect = { viewModel.update { s -> s.copy(appearanceMode = it.raw) } },
                labelFor = { appearanceLabels[it.raw] ?: it.raw },
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_starting_mode),
                options = MatchType.entries,
                selected = settings.defaultMatchType,
                onSelect = { viewModel.update { s -> s.copy(defaultMatchType = it) } },
                labelFor = { labels.matchType(it) },
            )
        }
        SettingsSectionFooter(stringResource(R.string.settings_theme_footer))
        SettingsSectionFooter(stringResource(R.string.settings_starting_mode_footer))

        Text(stringResource(R.string.settings_match_defaults), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_leg_format),
                options = X01LegFormat.entries,
                selected = settings.defaultLegFormat,
                onSelect = { viewModel.update { s -> s.copy(defaultLegFormat = it) } },
                labelFor = labels.legFormat,
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_legs_to_win),
                options = listOf(1, 2, 3, 5),
                selected = settings.defaultLegsToWin,
                onSelect = { viewModel.update { s -> s.copy(defaultLegsToWin = it) } },
            )
            ToggleSetting(
                label = stringResource(R.string.settings_sets_enabled),
                checked = settings.defaultSetsEnabled,
                onCheckedChange = { viewModel.update { s -> s.copy(defaultSetsEnabled = it) } },
            )
            if (settings.defaultSetsEnabled) {
                HorizontalOptionChips(
                    label = stringResource(R.string.settings_sets_enabled),
                    options = listOf(1, 2, 3),
                    selected = settings.defaultSetsToWin,
                    onSelect = { viewModel.update { s -> s.copy(defaultSetsToWin = it) } },
                )
            }
        }
        SettingsSectionFooter(stringResource(R.string.settings_match_defaults_footer))

        Text(stringResource(R.string.settings_defaults_x01), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_start_score),
                options = X01StartScores.all,
                selected = settings.defaultX01StartScore,
                onSelect = { viewModel.update { s -> s.copy(defaultX01StartScore = it) } },
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_checkout),
                options = X01CheckoutMode.entries,
                selected = settings.defaultX01CheckoutMode,
                onSelect = { viewModel.update { s -> s.copy(defaultX01CheckoutMode = it) } },
                labelFor = labels.checkout,
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_checkin),
                options = X01CheckInMode.entries,
                selected = settings.defaultX01CheckInMode,
                onSelect = { viewModel.update { s -> s.copy(defaultX01CheckInMode = it) } },
                labelFor = labels.checkIn,
            )
        }
        SettingsSectionFooter(stringResource(R.string.settings_x01_defaults_footer))

        Text(stringResource(R.string.settings_defaults_cricket), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            ToggleSetting(
                label = stringResource(R.string.settings_points_scoring),
                checked = settings.defaultCricketPointsEnabled,
                onCheckedChange = { viewModel.update { s -> s.copy(defaultCricketPointsEnabled = it) } },
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_scoring_mode),
                options = CricketScoringMode.entries,
                selected = settings.defaultCricketScoringMode,
                onSelect = { viewModel.update { s -> s.copy(defaultCricketScoringMode = it) } },
                labelFor = labels.cricketScoring,
            )
        }

        Text(stringResource(R.string.settings_bots), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            ToggleSetting(
                label = stringResource(R.string.settings_bot_stagger),
                checked = settings.botStaggerEnabled,
                testTag = AccessibilityIds.SETTINGS_BOT_STAGGER,
                onCheckedChange = { viewModel.update { s -> s.copy(botStaggerEnabled = it) } },
            )
            ToggleSetting(
                label = stringResource(R.string.settings_bot_haptics),
                checked = settings.botDartHapticsEnabled,
                enabled = settings.hapticsEnabled,
                testTag = AccessibilityIds.SETTINGS_BOT_HAPTICS,
                onCheckedChange = { viewModel.update { s -> s.copy(botDartHapticsEnabled = it) } },
            )
        }
        SettingsSectionFooter(stringResource(R.string.settings_bots_footer))

        Text(stringResource(R.string.settings_feedback), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            ToggleSetting(
                label = stringResource(R.string.settings_haptics),
                checked = settings.hapticsEnabled,
                testTag = AccessibilityIds.SETTINGS_HAPTICS,
                onCheckedChange = { viewModel.update { s -> s.copy(hapticsEnabled = it) } },
            )
            ToggleSetting(
                label = stringResource(R.string.settings_sound),
                checked = settings.soundEnabled,
                testTag = AccessibilityIds.SETTINGS_SOUND,
                onCheckedChange = { viewModel.update { s -> s.copy(soundEnabled = it) } },
            )
            ToggleSetting(
                label = stringResource(R.string.settings_turn_caller),
                checked = settings.turnTotalCallerEnabled,
                enabled = settings.soundEnabled,
                testTag = AccessibilityIds.SETTINGS_TURN_CALLER,
                onCheckedChange = { viewModel.update { s -> s.copy(turnTotalCallerEnabled = it) } },
            )
            if (!settings.soundEnabled) {
                SettingsSectionFooter(stringResource(R.string.settings_turn_caller_footer))
            }
        }
        SettingsSectionFooter(stringResource(R.string.settings_feedback_footer))

        Text(stringResource(R.string.settings_help), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        SetupSectionCard {
            TextButton(onClick = { showRulesSheet = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_help_rules), color = Brand.Green)
            }
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(com.jacobrozell.dartbuddy.support.AppLinks.SUPPORT))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_support_help), color = Brand.Green)
            }
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${com.jacobrozell.dartbuddy.support.AppLinks.FEEDBACK_EMAIL}"))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_support_feedback), color = Brand.Green)
            }
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(com.jacobrozell.dartbuddy.support.AppLinks.ACCESSIBILITY))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_support_accessibility), color = Brand.Green)
            }
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(com.jacobrozell.dartbuddy.support.AppLinks.PRIVACY))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_support_privacy), color = Brand.Green)
            }
            TextButton(onClick = { showReplayConfirm = true }, modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.SETTINGS_REPLAY_ONBOARDING)) {
                Text(stringResource(R.string.settings_replay_onboarding), color = Brand.Green)
            }
        }

        Text(stringResource(R.string.settings_data), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { showResetConfirm = true }, modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.SETTINGS_RESET_ALL)) {
            Text(stringResource(R.string.settings_resetAll), color = Brand.Red)
        }

        Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showRulesSheet) {
        GameRulesSheet(initialMode = settings.defaultMatchType, onDismiss = { showRulesSheet = false })
    }

    if (showReplayConfirm) {
        AlertDialog(
            onDismissRequest = { showReplayConfirm = false },
            title = { Text(stringResource(R.string.settings_replay_onboarding)) },
            text = { Text(stringResource(R.string.settings_replay_onboarding_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.replayOnboarding()
                    showReplayConfirm = false
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showReplayConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.settings_resetAll)) },
            text = { Text(stringResource(R.string.settings_resetConfirm)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAll { showResetConfirm = false } }) {
                    Text(stringResource(R.string.common_confirm), color = Brand.Red)
                }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    }
}

@Composable
private fun SettingsSectionFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = DS.s1, end = DS.s1, bottom = DS.s2),
    )
}

@Composable
private fun ToggleSetting(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.a11yId(testTag) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
