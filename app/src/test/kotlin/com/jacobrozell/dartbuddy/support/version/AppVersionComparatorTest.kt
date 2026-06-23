package com.jacobrozell.dartbuddy.support.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionComparatorTest {
    @Test
    fun patchVersionIsNewer() {
        assertTrue(AppVersionComparator.isStoreVersionNewer("1.0.10", "1.0.9"))
        assertFalse(AppVersionComparator.isStoreVersionNewer("1.0.9", "1.0.10"))
    }

    @Test
    fun minorVersionIsNewer() {
        assertTrue(AppVersionComparator.isStoreVersionNewer("1.2.0", "1.1.9"))
    }

    @Test
    fun equalVersionsAreNotNewer() {
        assertEquals(0, AppVersionComparator.compare("1.0.0", "1.0.0"))
        assertFalse(AppVersionComparator.isStoreVersionNewer("1.0.0", "1.0.0"))
    }

    @Test
    fun ignoresBuildMetadataSuffix() {
        assertEquals(AppVersionComparator.components("1.2.3-beta"), listOf(1, 2, 3))
        assertEquals(0, AppVersionComparator.compare("1.2.3", "1.2.3-rc1"))
    }

    @Test
    fun padsMissingSegmentsWithZero() {
        assertEquals(0, AppVersionComparator.compare("1.2", "1.2.0"))
        assertTrue(AppVersionComparator.isStoreVersionNewer("2.0", "1.9.9"))
    }
}
