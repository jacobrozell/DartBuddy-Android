package com.jacobrozell.dartbuddy.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

object GameplayLayoutMetrics {
    val landscapeScoringPadWidth = 252.dp
}

fun usesLandscapeMatchScoringLayout(isLandscape: Boolean): Boolean = isLandscape

fun usesPinnedActiveX01PlayerCard(playerCount: Int, isLandscape: Boolean): Boolean =
    isLandscape || playerCount >= 3

@Composable
fun isLandscapeOrientation(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp < configuration.screenWidthDp
}
