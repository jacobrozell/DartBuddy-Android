package com.jacobrozell.dartbuddy.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.People
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.displayProfile
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.domain.model.X01StartScores
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingMode
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingRosterDraft
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingStep
import com.jacobrozell.dartbuddy.domain.onboarding.fromOnboardingSliderIndex
import com.jacobrozell.dartbuddy.domain.onboarding.onboardingOrder
import com.jacobrozell.dartbuddy.domain.onboarding.onboardingSliderIndex
import com.jacobrozell.dartbuddy.domain.onboarding.showsOnboardingRulesIntro
import com.jacobrozell.dartbuddy.support.AppLinks
import com.jacobrozell.dartbuddy.domain.surface.ProductSurface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jacobrozell.dartbuddy.support.MotionPreferences
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.ui.layout.formImeInsets
import com.jacobrozell.dartbuddy.ui.layout.stickyFooterInsets
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.rememberOptionLabelMaps
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.players.AvatarStylePicker
import com.jacobrozell.dartbuddy.ui.players.BotDifficultyStatsSection
import com.jacobrozell.dartbuddy.ui.players.displayNameComposable
import com.jacobrozell.dartbuddy.ui.play.rules.GameRulesGuideContent
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun OnboardingFlow(
    mode: OnboardingMode,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(mode) { viewModel.initialize(mode) }

    val dark = isSystemInDarkTheme()
    val background = if (dark) Brand.BackgroundDark else Brand.BackgroundLight
    val context = LocalContext.current
    val reduceMotion = MotionPreferences.prefersReducedMotion(context)
    var lastStepIndex by remember { mutableStateOf(onboardingStepIndex(state.step)) }

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = onboardingStepIndex(targetState) >= lastStepIndex
                lastStepIndex = onboardingStepIndex(targetState)
                if (reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    onboardingStepTransition(forward = forward, reduceMotion = false)
                }
            },
            label = "onboarding_step",
        ) { step ->
            when (step) {
                OnboardingStep.Welcome -> OnboardingWelcomeStep(
                    onNext = viewModel::advanceFromWelcome,
                    onSkip = viewModel::skipFromWelcome,
                )
                OnboardingStep.RosterSetup -> OnboardingRosterStep(
                    progressIndex = step.progressIndex!!,
                    onBack = viewModel::goBack,
                    onContinue = viewModel::setRosterDraft,
                )
                OnboardingStep.Preferences -> OnboardingPreferencesStep(
                    progressIndex = step.progressIndex!!,
                    settings = state.settings,
                    onBack = viewModel::goBack,
                    onUpdate = viewModel::updateSettings,
                    onContinue = viewModel::goToAppTour,
                )
                OnboardingStep.LearnToPlay -> OnboardingLearnStep(
                    progressIndex = step.progressIndex!!,
                    onBack = viewModel::goBack,
                    onContinue = viewModel::goToAppTour,
                )
                OnboardingStep.AppTour -> OnboardingAppTourStep(
                    progressIndex = step.progressIndex!!,
                    onBack = viewModel::goBack,
                    onContinue = viewModel::goToSupport,
                )
                OnboardingStep.Support -> OnboardingSupportStep(
                    progressIndex = step.progressIndex!!,
                    onBack = viewModel::goBack,
                    onContinue = viewModel::goToReady,
                )
                OnboardingStep.Ready -> OnboardingReadyStep(
                    progressIndex = step.progressIndex!!,
                    rosterDraft = if (!state.skippedFromWelcome) state.rosterDraft else null,
                    showsBack = step.backStep(
                        showsRulesIntro = state.rosterDraft?.botDifficulty?.showsOnboardingRulesIntro,
                        skipsRosterSetup = state.skipsRosterSetup,
                    ) != null,
                    onBack = viewModel::goBack,
                    onGetStarted = { viewModel.complete(onFinished) },
                    isFinishing = state.isFinishing,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingStepScaffold(
    progressIndex: Int? = null,
    showsBack: Boolean = false,
    showsSkip: Boolean = false,
    onBack: () -> Unit = {},
    onSkip: () -> Unit = {},
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val background = if (dark) Brand.BackgroundDark else Brand.BackgroundLight

    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    progressIndex?.let {
                        Text(
                            stringResource(R.string.onboarding_step_progress, it, OnboardingStep.PROGRESS_TOTAL),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (showsBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    }
                },
                actions = {
                    if (showsSkip) {
                        TextButton(onClick = onSkip) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .stickyFooterInsets()
                    .padding(horizontal = DS.s4, vertical = DS.s4),
            ) {
                footer()
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .formImeInsets()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DS.s4),
            verticalArrangement = Arrangement.spacedBy(DS.s4),
        ) {
            content()
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    OnboardingStepScaffold(
        showsSkip = true,
        onSkip = onSkip,
        footer = {
            PrimaryActionButton(title = stringResource(R.string.onboarding_next), onClick = onNext)
        },
    ) {
        OnboardingHero(
            title = stringResource(R.string.onboarding_welcome_title),
            body = stringResource(R.string.onboarding_welcome_body),
        )
    }
}

@Composable
private fun OnboardingRosterStep(
    progressIndex: Int,
    onBack: () -> Unit,
    onContinue: (OnboardingRosterDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var avatarStyle by remember { mutableStateOf(PlayerAvatarStyle.Dart) }
    var colorToken by remember { mutableStateOf(PlayerColorToken.Green) }
    var tierIndex by remember { mutableFloatStateOf(BotDifficulty.Easy.onboardingSliderIndex.toFloat()) }
    val selectedDifficulty = BotDifficulty.fromOnboardingSliderIndex(tierIndex.toInt())

    OnboardingStepScaffold(
        progressIndex = progressIndex,
        showsBack = true,
        onBack = onBack,
        footer = {
            PrimaryActionButton(
                title = stringResource(R.string.onboarding_continue),
                onClick = {
                    onContinue(
                        OnboardingRosterDraft(
                            name = name.trim(),
                            avatarStyle = avatarStyle,
                            colorToken = colorToken,
                            botDifficulty = selectedDifficulty,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
            )
        },
    ) {
        OnboardingHero(
            title = stringResource(R.string.onboarding_roster_title),
            body = stringResource(R.string.onboarding_roster_body),
        )
        SetupSectionCard {
            Text(stringResource(R.string.onboarding_roster_you_section), fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.onboarding_roster_name)) },
                modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.ONBOARDING_PLAYER_NAME),
            )
        }
        SetupSectionCard {
            AvatarStylePicker(
                selected = avatarStyle,
                onSelect = { avatarStyle = it },
                showsTitle = true,
            )
            HorizontalOptionChips(
                label = stringResource(R.string.onboarding_roster_color),
                options = PlayerColorToken.all.take(8),
                selected = colorToken,
                onSelect = { colorToken = it },
                labelFor = { it.raw },
            )
        }
        SetupSectionCard {
            Text(stringResource(R.string.onboarding_roster_experience_section), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.onboarding_roster_experience_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.onboarding_roster_experience_min), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.onboarding_roster_experience_max), style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = tierIndex,
                onValueChange = { tierIndex = it },
                valueRange = 0f..(BotDifficulty.onboardingOrder.lastIndex).toFloat(),
                steps = BotDifficulty.onboardingOrder.lastIndex - 1,
            )
            Text(
                stringResource(R.string.onboarding_roster_experience_selected, selectedDifficulty.displayNameComposable()),
                fontWeight = FontWeight.SemiBold,
            )
        }
        SetupSectionCard {
            Text(stringResource(R.string.onboarding_roster_opponent_section), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.onboarding_roster_opponent_summary, selectedDifficulty.rosterNameComposable()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(selectedDifficulty.displayNameComposable(), color = Brand.ProBot, fontWeight = FontWeight.Bold)
            BotDifficultyStatsSection(
                profile = selectedDifficulty.skillProfile.displayProfile,
                showsHeader = false,
            )
        }
    }
}

@Composable
private fun BotDifficulty.rosterNameComposable(): String = stringResource(R.string.bot_rosterNameFormat, displayNameComposable())

@Composable
private fun OnboardingPreferencesStep(
    progressIndex: Int,
    settings: com.jacobrozell.dartbuddy.data.repository.AppSettings,
    onBack: () -> Unit,
    onUpdate: ((com.jacobrozell.dartbuddy.data.repository.AppSettings) -> com.jacobrozell.dartbuddy.data.repository.AppSettings) -> Unit,
    onContinue: () -> Unit,
) {
    val labels = rememberOptionLabelMaps()
    val appearanceOptions = listOf("system", "light", "dark")
    val appearanceLabels = mapOf(
        "system" to stringResource(R.string.settings_appearance_system),
        "light" to stringResource(R.string.settings_appearance_light),
        "dark" to stringResource(R.string.settings_appearance_dark),
    )
    val modeLabels = mapOf(
        MatchType.X01 to stringResource(R.string.play_mode_x01),
        MatchType.Cricket to stringResource(R.string.play_mode_cricket),
    )

    OnboardingStepScaffold(
        progressIndex = progressIndex,
        showsBack = true,
        onBack = onBack,
        footer = {
            PrimaryActionButton(title = stringResource(R.string.onboarding_continue), onClick = onContinue)
        },
    ) {
        Text(stringResource(R.string.onboarding_preferences_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SetupSectionCard {
            HorizontalOptionChips(
                label = stringResource(R.string.settings_appearance),
                options = appearanceOptions,
                selected = settings.appearanceMode,
                onSelect = { onUpdate { s -> s.copy(appearanceMode = it) } },
                labelFor = { appearanceLabels[it] ?: it },
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_starting_mode),
                options = MatchType.entries,
                selected = settings.defaultMatchType,
                onSelect = { onUpdate { s -> s.copy(defaultMatchType = it) } },
                labelFor = { modeLabels[it] ?: labels.matchType(it) },
            )
        }
        SetupSectionCard {
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_start_score),
                options = X01StartScores.all,
                selected = settings.defaultX01StartScore,
                onSelect = { onUpdate { s -> s.copy(defaultX01StartScore = it) } },
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_checkout),
                options = X01CheckoutMode.entries,
                selected = settings.defaultX01CheckoutMode,
                onSelect = { onUpdate { s -> s.copy(defaultX01CheckoutMode = it) } },
                labelFor = labels.checkout,
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_checkin),
                options = X01CheckInMode.entries,
                selected = settings.defaultX01CheckInMode,
                onSelect = { onUpdate { s -> s.copy(defaultX01CheckInMode = it) } },
                labelFor = labels.checkIn,
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_leg_format),
                options = X01LegFormat.entries,
                selected = settings.defaultLegFormat,
                onSelect = { onUpdate { s -> s.copy(defaultLegFormat = it) } },
                labelFor = labels.legFormat,
            )
            HorizontalOptionChips(
                label = stringResource(R.string.settings_label_legs_to_win),
                options = listOf(1, 2, 3, 5),
                selected = settings.defaultLegsToWin,
                onSelect = { onUpdate { s -> s.copy(defaultLegsToWin = it) } },
            )
        }
        SetupSectionCard {
            OnboardingToggle(stringResource(R.string.settings_haptics), settings.hapticsEnabled) {
                onUpdate { s -> s.copy(hapticsEnabled = it) }
            }
            OnboardingToggle(stringResource(R.string.settings_sound), settings.soundEnabled) {
                onUpdate { s -> s.copy(soundEnabled = it) }
            }
            OnboardingToggle(stringResource(R.string.settings_turn_caller), settings.turnTotalCallerEnabled) {
                onUpdate { s -> s.copy(turnTotalCallerEnabled = it) }
            }
        }
        Text(
            stringResource(R.string.onboarding_preferences_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OnboardingLearnStep(progressIndex: Int, onBack: () -> Unit, onContinue: () -> Unit) {
    OnboardingStepScaffold(
        progressIndex = progressIndex,
        showsBack = true,
        onBack = onBack,
        footer = {
            PrimaryActionButton(title = stringResource(R.string.onboarding_continue), onClick = onContinue)
        },
    ) {
        Text(
            stringResource(R.string.onboarding_learn_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.onboarding_learn_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        GameRulesGuideContent(mode = MatchType.X01, showsModePicker = true)
    }
}

@Composable
private fun OnboardingAppTourStep(progressIndex: Int, onBack: () -> Unit, onContinue: () -> Unit) {
    OnboardingStepScaffold(
        progressIndex = progressIndex,
        showsBack = true,
        onBack = onBack,
        footer = {
            PrimaryActionButton(title = stringResource(R.string.onboarding_continue), onClick = onContinue)
        },
    ) {
        Text(
            stringResource(R.string.onboarding_tour_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.onboarding_tour_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        TourCard(Icons.Default.PlayArrow, stringResource(R.string.onboarding_tour_play_title), stringResource(R.string.onboarding_tour_play_body))
        TourCard(Icons.Default.People, stringResource(R.string.onboarding_tour_players_title), stringResource(R.string.onboarding_tour_players_body))
        TourCard(Icons.Default.BarChart, stringResource(R.string.onboarding_tour_activity_title), stringResource(R.string.onboarding_tour_activity_body))
        TourCard(Icons.Default.Settings, stringResource(R.string.onboarding_tour_settings_title), stringResource(R.string.onboarding_tour_settings_body))
        Text(
            stringResource(R.string.onboarding_tour_roadmap),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TourCard(icon: ImageVector, title: String, body: String) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Row(
        Modifier
            .fillMaxWidth()
            .background(cardBg, androidx.compose.foundation.shape.RoundedCornerShape(DS.radiusMd))
            .padding(DS.s4),
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        Icon(icon, contentDescription = null, tint = Brand.Green, modifier = Modifier.padding(top = 4.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OnboardingSupportStep(progressIndex: Int, onBack: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    OnboardingStepScaffold(
        progressIndex = progressIndex,
        showsBack = true,
        onBack = onBack,
        footer = {
            PrimaryActionButton(title = stringResource(R.string.onboarding_continue), onClick = onContinue)
        },
    ) {
        OnboardingHero(
            title = stringResource(R.string.onboarding_support_title),
            body = stringResource(R.string.onboarding_support_body),
        )
        SupportLink(
            title = stringResource(R.string.onboarding_support_help),
            accessibilityLabel = stringResource(R.string.onboarding_support_help_a11y),
        ) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.SUPPORT)))
        }
        SupportLink(
            title = stringResource(R.string.onboarding_support_feedback),
            accessibilityLabel = stringResource(R.string.onboarding_support_feedback_a11y),
        ) {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppLinks.FEEDBACK_EMAIL}")))
        }
        if (ProductSurface.showsAccessibilityMarketing) {
            SupportLink(
                title = stringResource(R.string.onboarding_support_accessibility),
                accessibilityLabel = stringResource(R.string.onboarding_support_accessibility_a11y),
            ) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.ACCESSIBILITY)))
            }
        }
        SupportLink(
            title = stringResource(R.string.onboarding_support_privacy),
            accessibilityLabel = stringResource(R.string.onboarding_support_privacy_a11y),
        ) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PRIVACY)))
        }
    }
}

@Composable
private fun SupportLink(title: String, accessibilityLabel: String, onClick: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, androidx.compose.foundation.shape.RoundedCornerShape(DS.radiusMd))
            .semantics { contentDescription = accessibilityLabel },
    ) {
        Text(title, color = Brand.Green, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OnboardingReadyStep(
    progressIndex: Int,
    rosterDraft: OnboardingRosterDraft?,
    showsBack: Boolean,
    onBack: () -> Unit,
    onGetStarted: () -> Unit,
    isFinishing: Boolean,
) {
    OnboardingStepScaffold(
        progressIndex = progressIndex,
        showsBack = showsBack,
        onBack = onBack,
        footer = {
            PrimaryActionButton(
                title = stringResource(R.string.onboarding_start_match),
                onClick = onGetStarted,
                enabled = !isFinishing,
            )
        },
    ) {
        OnboardingHero(
            title = stringResource(R.string.onboarding_ready_title),
            body = stringResource(
                if (rosterDraft != null) R.string.onboarding_ready_body_with_roster else R.string.onboarding_ready_body,
            ),
        )
        rosterDraft?.let { draft ->
            SetupSectionCard {
                Text(stringResource(R.string.onboarding_ready_roster_title), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(DS.s3), verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(
                        name = draft.name,
                        colorToken = draft.colorToken.raw,
                        playerId = "onboarding",
                        size = 36.dp,
                        avatarStyleRaw = draft.avatarStyle.raw,
                    )
                    Text(draft.name, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DS.s3), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", modifier = Modifier.padding(horizontal = 8.dp))
                    Text(draft.botDifficulty.rosterNameComposable(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(
            stringResource(R.string.onboarding_ready_replay_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingHero(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        Text("🎯", style = MaterialTheme.typography.displaySmall)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
