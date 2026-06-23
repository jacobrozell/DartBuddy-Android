package com.jacobrozell.dartbuddy.support.state

object AppAppearancePolicy {
    /** Settings uses brand scoreboard chrome unless the user explicitly chose Light. */
    fun settingsUsesBrandPalette(appearanceModeRaw: String): Boolean =
        appearanceModeRaw != "light"
}
