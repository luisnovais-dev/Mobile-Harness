package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Spec #5, RF-5 e Seg. 20: a sessão nunca recebe bind do volume compartilhado. */
class ProotBindsTest {
    @get:Rule val temp = TemporaryFolder()

    private fun binds(hostExists: (String) -> Boolean = { true }): List<String> {
        val rootfs = temp.newFolder("rootfs")
        val workspace = temp.newFolder("workspace")
        val bridge = temp.newFolder("bridge")
        return prootWorkspaceBinds(rootfs, workspace, bridge, "/workspace", hostExists)
    }

    @Test
    fun neverBindsSharedStorageVolume() {
        val args = binds()
        assertTrue(args.none { it.contains("/storage/emulated/0") || it.contains("/sdcard") })
    }

    @Test
    fun projectWorkspaceIsTheOnlyProjectBind() {
        val rootfs = temp.newFolder("rootfs2")
        val workspace = temp.newFolder("workspace2")
        val bridge = temp.newFolder("bridge2")
        val args = prootWorkspaceBinds(rootfs, workspace, bridge, "/workspace") { true }
        assertTrue(args.contains("${workspace.absolutePath}:/workspace"))
        assertTrue(args.contains("${bridge.absolutePath}:/pocket-bridge"))
    }

    @Test
    fun hostSystemPathsAreBoundOnlyWhenTheyExist() {
        val args = binds { it == "/system" }
        assertTrue(args.contains("/system"))
        assertFalse(args.contains("/vendor"))
    }
}
