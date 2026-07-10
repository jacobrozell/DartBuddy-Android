package com.jacobrozell.dartbuddy.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.ui.designsystem.DS

object GameplayLayoutMetrics {
    val landscapeScoringPadWidth = 252.dp
    val tabletLandscapeScoringPadWidth = 300.dp
    val tabletPortraitScoringPadWidth = 420.dp
    val tabletContentMaxWidth = 920.dp
    val onboardingContentMaxWidth = 720.dp
    val setupWideBreakpoint = 600.dp
    val wideTabletBreakpoint = 840.dp
    val compactWidthBreakpoint = 360.dp
    val extraCompactPadBreakpoint = 340.dp
    const val sideBySidePlayerCountThreshold = 3
    const val accessibilityFontScaleThreshold = 1.3f
}

data class LayoutContext(
    val smallestWidthDp: Int,
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
    val playerCount: Int = 0,
) {
    val isLandscape: Boolean get() = heightDp < widthDp
    val isTablet: Boolean get() = smallestWidthDp >= GameplayLayoutMetrics.setupWideBreakpoint.value.toInt()
    val isCompactWidth: Boolean get() = smallestWidthDp < GameplayLayoutMetrics.compactWidthBreakpoint.value.toInt()
    val isAccessibilityFont: Boolean get() = fontScale >= GameplayLayoutMetrics.accessibilityFontScaleThreshold
}

@Composable
fun rememberLayoutContext(playerCount: Int = 0): LayoutContext {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    return remember(
        configuration.smallestScreenWidthDp,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        fontScale,
        playerCount,
    ) {
        LayoutContext(
            smallestWidthDp = configuration.smallestScreenWidthDp,
            widthDp = configuration.screenWidthDp,
            heightDp = configuration.screenHeightDp,
            fontScale = fontScale,
            playerCount = playerCount,
        )
    }
}

fun usesAccessibilityMatchScoringLayout(ctx: LayoutContext): Boolean = ctx.isAccessibilityFont

fun usesAccessibilitySetupHomeLayout(ctx: LayoutContext): Boolean = ctx.isAccessibilityFont

fun usesAccessibilityTabListLayout(ctx: LayoutContext): Boolean = ctx.isAccessibilityFont

fun usesWideSetupHomeLayout(ctx: LayoutContext): Boolean =
    ctx.isTablet && !usesAccessibilitySetupHomeLayout(ctx)

fun usesLandscapeMatchScoringLayout(ctx: LayoutContext): Boolean = ctx.isLandscape

/** Phone landscape only — large phones must not be treated as tablets via width alone. */
fun usesSideBySideMatchScoringLayout(ctx: LayoutContext): Boolean {
    if (usesAccessibilityMatchScoringLayout(ctx)) return false
    return ctx.isLandscape && !ctx.isTablet
}

fun usesLandscapePhoneMatchScoringLayout(ctx: LayoutContext): Boolean =
    usesSideBySideMatchScoringLayout(ctx)

fun usesTabletPortraitMatchScoringLayout(ctx: LayoutContext): Boolean =
    ctx.isTablet && !ctx.isLandscape

fun usesSideBySideBottomScoringRegion(ctx: LayoutContext): Boolean = false

fun usesPinnedActiveX01PlayerCard(ctx: LayoutContext): Boolean {
    if (usesAccessibilityMatchScoringLayout(ctx)) return true
    if (ctx.isLandscape) return true
    return ctx.playerCount >= GameplayLayoutMetrics.sideBySidePlayerCountThreshold
}

fun usesWidePlayerScoreCardLayout(ctx: LayoutContext): Boolean =
    ctx.isTablet &&
        ctx.widthDp >= GameplayLayoutMetrics.wideTabletBreakpoint.value.toInt() &&
        !usesAccessibilityMatchScoringLayout(ctx)

fun usesStackedPlayerScoreCardLayout(ctx: LayoutContext): Boolean =
    usesAccessibilityMatchScoringLayout(ctx) || ctx.fontScale >= 1.5f

fun usesIPadPortraitTwoPlayerScoreRow(ctx: LayoutContext, playerCount: Int): Boolean =
    usesTabletPortraitMatchScoringLayout(ctx) && playerCount == 2

fun usesLandscapeMatchSummaryLayout(ctx: LayoutContext): Boolean =
    !usesAccessibilityMatchScoringLayout(ctx) && ctx.isLandscape

fun onboardingContentMaxWidth(ctx: LayoutContext): Dp =
    if (ctx.isTablet) GameplayLayoutMetrics.onboardingContentMaxWidth else Dp.Unspecified

fun usesMatchSummarySideBySidePlayerGrid(ctx: LayoutContext, playerCount: Int): Boolean {
    if (playerCount < 2) return false
    if (usesAccessibilityMatchScoringLayout(ctx)) return false
    if (ctx.isTablet) return true
    return !ctx.isLandscape && playerCount == 2
}

fun contentMaxWidth(smallestWidthDp: Int): Dp =
    if (smallestWidthDp >= GameplayLayoutMetrics.setupWideBreakpoint.value.toInt()) {
        GameplayLayoutMetrics.tabletContentMaxWidth
    } else {
        Dp.Unspecified
    }

fun contentMaxWidth(ctx: LayoutContext): Dp = contentMaxWidth(ctx.smallestWidthDp)

fun scoringPadColumnCount(ctx: LayoutContext): Int =
    when {
        usesAccessibilityMatchScoringLayout(ctx) -> 4
        ctx.smallestWidthDp < GameplayLayoutMetrics.extraCompactPadBreakpoint.value.toInt() -> 4
        else -> 7
    }

fun scoringPadWidth(ctx: LayoutContext): Dp =
    when {
        !usesSideBySideMatchScoringLayout(ctx) -> Dp.Unspecified
        usesTabletPortraitMatchScoringLayout(ctx) -> GameplayLayoutMetrics.tabletPortraitScoringPadWidth
        ctx.isTablet && ctx.isLandscape -> GameplayLayoutMetrics.tabletLandscapeScoringPadWidth
        else -> GameplayLayoutMetrics.landscapeScoringPadWidth
    }

fun usesTransposedCricketBoardLayout(ctx: LayoutContext): Boolean {
    if (usesAccessibilityMatchScoringLayout(ctx)) return false
    if (!ctx.isLandscape) return false
    return !usesSideBySideBottomScoringRegion(ctx)
}

fun usesCricketBoardFillsAvailableHeight(ctx: LayoutContext): Boolean =
    ctx.isTablet &&
        ctx.isLandscape &&
        usesSideBySideBottomScoringRegion(ctx)

fun tabScrollBottomPadding(ctx: LayoutContext): Dp =
    if (usesAccessibilityTabListLayout(ctx)) {
        DS.s6 + (88.dp * ctx.fontScale.coerceAtMost(2f))
    } else {
        DS.s6
    }

fun setupScrollBottomPadding(ctx: LayoutContext): Dp =
    if (usesAccessibilitySetupHomeLayout(ctx)) {
        120.dp
    } else {
        DS.s4
    }

@Composable
fun isLandscapeOrientation(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp < configuration.screenWidthDp
}

@Composable
fun currentScreenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

@Composable
fun currentSmallestScreenWidthDp(): Int = LocalConfiguration.current.smallestScreenWidthDp

// Legacy overloads — prefer LayoutContext at call sites.

fun usesSideBySideMatchScoringLayout(isLandscape: Boolean, screenWidthDp: Int): Boolean =
    usesSideBySideMatchScoringLayout(
        LayoutContext(
            smallestWidthDp = screenWidthDp,
            widthDp = screenWidthDp,
            heightDp = if (isLandscape) 400 else 900,
            fontScale = 1f,
        ),
    )

fun usesWidePlayerScoreCardLayout(screenWidthDp: Int): Boolean =
    usesWidePlayerScoreCardLayout(
        LayoutContext(
            smallestWidthDp = screenWidthDp,
            widthDp = screenWidthDp,
            heightDp = 900,
            fontScale = 1f,
        ),
    )

fun usesPinnedActiveX01PlayerCard(playerCount: Int, isLandscape: Boolean): Boolean =
    usesPinnedActiveX01PlayerCard(
        LayoutContext(
            smallestWidthDp = 400,
            widthDp = if (isLandscape) 900 else 400,
            heightDp = if (isLandscape) 400 else 900,
            fontScale = 1f,
            playerCount = playerCount,
        ),
    )

fun usesLandscapeMatchSummaryLayout(isLandscape: Boolean): Boolean =
    usesLandscapeMatchSummaryLayout(
        LayoutContext(
            smallestWidthDp = 400,
            widthDp = if (isLandscape) 900 else 400,
            heightDp = if (isLandscape) 400 else 900,
            fontScale = 1f,
        ),
    )

fun usesMatchSummarySideBySidePlayerGrid(
    screenWidthDp: Int,
    playerCount: Int,
    isLandscape: Boolean,
): Boolean = usesMatchSummarySideBySidePlayerGrid(
    LayoutContext(
        smallestWidthDp = screenWidthDp,
        widthDp = screenWidthDp,
        heightDp = if (isLandscape) 400 else 900,
        fontScale = 1f,
    ),
    playerCount,
)

fun scoringPadWidth(isLandscape: Boolean, screenWidthDp: Int): Dp =
    scoringPadWidth(
        LayoutContext(
            smallestWidthDp = screenWidthDp,
            widthDp = screenWidthDp,
            heightDp = if (isLandscape) 400 else 900,
            fontScale = 1f,
        ),
    )
