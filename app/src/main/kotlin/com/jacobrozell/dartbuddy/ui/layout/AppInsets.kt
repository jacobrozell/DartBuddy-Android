package com.jacobrozell.dartbuddy.ui.layout

import android.app.Activity
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Top + bottom safe areas for full-screen routes without a bottom tab bar (match, summary). */
fun Modifier.matchScreenInsets(): Modifier = statusBarsPadding().navigationBarsPadding()

/** Horizontal display-cutout padding for match headers under punch-hole cameras. */
fun Modifier.matchHeaderCutoutPadding(): Modifier = displayCutoutPadding()

/** Bottom inset for sticky footers above gesture navigation (onboarding, sheets). */
fun Modifier.stickyFooterInsets(): Modifier = navigationBarsPadding()

/** Keyboard inset for text fields in bottom sheets and scrollable forms. */
fun Modifier.formImeInsets(): Modifier = imePadding()

/**
 * Keeps status-bar and navigation-bar icon contrast aligned with the active app theme
 * (including user-selected light/dark override).
 */
@Composable
fun SyncSystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
