package com.jarves.mh.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedStorageAccessTest {
    @Test
    fun sideloadNeedsWriteAndReadGranted() {
        assertTrue(SharedStorageAccess.isGranted(sdkInt = 30, isPlayBuild = false, isManager = false, writeGranted = true, readGranted = true))
        assertFalse(SharedStorageAccess.isGranted(sdkInt = 30, isPlayBuild = false, isManager = false, writeGranted = true, readGranted = false))
        assertFalse(SharedStorageAccess.isGranted(sdkInt = 30, isPlayBuild = false, isManager = false, writeGranted = false, readGranted = true))
    }

    @Test
    fun sideloadIgnoresAllFilesAccess() {
        assertFalse(SharedStorageAccess.isGranted(sdkInt = 30, isPlayBuild = false, isManager = true, writeGranted = false, readGranted = false))
    }

    @Test
    fun playBuildNeedsAllFilesAccessOnApi30() {
        assertTrue(SharedStorageAccess.isGranted(sdkInt = 36, isPlayBuild = true, isManager = true, writeGranted = false, readGranted = false))
        assertFalse(SharedStorageAccess.isGranted(sdkInt = 36, isPlayBuild = true, isManager = false, writeGranted = true, readGranted = true))
    }

    @Test
    fun playBuildBelowApi30NeverGrants() {
        assertFalse(SharedStorageAccess.isGranted(sdkInt = 29, isPlayBuild = true, isManager = true, writeGranted = true, readGranted = true))
    }

    @Test
    fun sideloadAfterPermanentDenialOpensAppSettings() {
        assertEquals(
            StorageRequestFallback.OPEN_APP_SETTINGS,
            storageRequestFallback(isPlayBuild = false, granted = false, canAskAgain = false),
        )
    }

    @Test
    fun sideloadAfterDenialThatCanBeAskedAgainShowsNoFallback() {
        assertEquals(
            StorageRequestFallback.NONE,
            storageRequestFallback(isPlayBuild = false, granted = false, canAskAgain = true),
        )
    }

    @Test
    fun grantedOrPlayBuildShowsNoFallback() {
        assertEquals(StorageRequestFallback.NONE, storageRequestFallback(isPlayBuild = false, granted = true, canAskAgain = false))
        assertEquals(StorageRequestFallback.NONE, storageRequestFallback(isPlayBuild = true, granted = false, canAskAgain = false))
    }
}

