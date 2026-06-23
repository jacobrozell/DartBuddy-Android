package com.jacobrozell.dartbuddy.support.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAppearancePolicyTest {
    @Test
    fun settingsUsesBrandPaletteUnlessLight() {
        assertTrue(AppAppearancePolicy.settingsUsesBrandPalette("system"))
        assertTrue(AppAppearancePolicy.settingsUsesBrandPalette("dark"))
        assertFalse(AppAppearancePolicy.settingsUsesBrandPalette("light"))
    }
}
