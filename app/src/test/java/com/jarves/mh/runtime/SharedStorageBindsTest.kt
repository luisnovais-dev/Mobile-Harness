package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SharedStorageBindsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun withoutAccessAddsNoBindAndCreatesNoMountPoint() {
        val rootfs = tmp.newFolder("ubuntu")
        assertEquals(emptyList<String>(), sharedStorageBinds(access = false))
        assertFalse(File(rootfs, "sdcard").exists())
    }

    @Test
    fun withAccessBindsPrimaryStorageToSdcard() {
        assertEquals(listOf("-b", "/storage/emulated/0:/sdcard"), sharedStorageBinds(access = true))
    }

    @Test
    fun bindsAreComputedWithoutSideEffects() {
        val rootfs = tmp.newFolder("ubuntu")
        sharedStorageBinds(access = true)
        assertFalse(File(rootfs, "sdcard").exists())
    }

    @Test
    fun mountPointIsCreatedOnlyWithAccess() {
        val rootfs = tmp.newFolder("ubuntu")
        ensureSharedStorageMountPoint(access = false, rootfs = rootfs)
        assertFalse(File(rootfs, "sdcard").exists())
        ensureSharedStorageMountPoint(access = true, rootfs = rootfs)
        assertTrue(File(rootfs, "sdcard").isDirectory)
    }
}
