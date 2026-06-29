package com.jacobrozell.dartbuddy.domain.surface

import com.jacobrozell.dartbuddy.BuildConfig
import com.jacobrozell.dartbuddy.domain.model.MatchType

/**
 * Controls which product areas are reachable in this build.
 *
 * **Debug:** defaults to the full catalog (training bots, export, etc.).
 * **Release:** defaults to lean 1.0 (X01 + Cricket, custom bots on, training/export off).
 * Launch intent extras override either default.
 */
object ProductSurface {
    data class Configuration(
        val showsModesTab: Boolean = false,
        val showsPartyModes: Boolean = false,
        val showsCoopModes: Boolean = false,
        val showsTrainingBots: Boolean = false,
        val showsCustomBots: Boolean = true,
        val showsPlayerExport: Boolean = false,
        val showsAccessibilityMarketing: Boolean = true,
        val bundledLocaleCodes: List<String> = listOf("en"),
    )

    const val FULL_SURFACE_LAUNCH_ARGUMENT = "enable_full_product_surface"
    const val LEAN_SURFACE_LAUNCH_ARGUMENT = "enable_lean_product_surface"

    val lean1_0 = Configuration()

    val full = Configuration(
        showsModesTab = true,
        showsPartyModes = true,
        showsCoopModes = true,
        showsTrainingBots = true,
        showsCustomBots = true,
        showsPlayerExport = true,
        showsAccessibilityMarketing = true,
        bundledLocaleCodes = listOf("en", "de", "es", "nl", "fr", "zh-Hans", "it"),
    )

    private var launchArguments: List<String> = emptyList()

    fun configure(launchArguments: List<String>) {
        this.launchArguments = launchArguments
    }

    fun configureFromIntentExtras(hasLean: Boolean, hasFull: Boolean) {
        launchArguments = buildList {
            if (hasLean) add(LEAN_SURFACE_LAUNCH_ARGUMENT)
            if (hasFull) add(FULL_SURFACE_LAUNCH_ARGUMENT)
        }
    }

    val isFullProductSurfaceEnabled: Boolean
        get() = isFullProductSurfaceEnabled(launchArguments)

    fun isFullProductSurfaceEnabled(arguments: List<String>): Boolean {
        if (LEAN_SURFACE_LAUNCH_ARGUMENT in arguments) return false
        if (FULL_SURFACE_LAUNCH_ARGUMENT in arguments) return true
        return BuildConfig.DEBUG
    }

    val active: Configuration
        get() = configurationFor(launchArguments)

    fun configurationFor(arguments: List<String>): Configuration =
        if (isFullProductSurfaceEnabled(arguments)) full else lean1_0

    val showsModesTab: Boolean get() = active.showsModesTab
    val showsPartyModes: Boolean get() = active.showsPartyModes
    val showsCoopModes: Boolean get() = active.showsCoopModes
    val showsTrainingBots: Boolean get() = active.showsTrainingBots
    val showsCustomBots: Boolean get() = active.showsCustomBots
    val showsPlayerExport: Boolean get() = active.showsPlayerExport
    val showsAccessibilityMarketing: Boolean get() = active.showsAccessibilityMarketing
    val bundledLocaleCodes: List<String> get() = active.bundledLocaleCodes

    /** Coarse build slice label for analytics (`full` vs `lean`). */
    val analyticsLabel: String
        get() = if (isFullProductSurfaceEnabled) "full" else "lean"

    fun isMatchTypeReachable(matchType: MatchType): Boolean =
        isMatchTypeReachable(matchType, launchArguments)

    fun isMatchTypeReachable(matchType: MatchType, arguments: List<String>): Boolean {
        if (isFullProductSurfaceEnabled(arguments)) return true
        return when (matchType) {
            MatchType.X01, MatchType.Cricket -> true
            else -> false
        }
    }
}
