package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectWorkspaceLocationTest {
    private val privateDir = File("/data/user/0/com.jarves.mh/files/workspaces/p1")
    private val external = "/storage/emulated/0/MeuProjeto"

    @Test
    fun noExternalRootUsesPrivateWorkspace() {
        assertEquals(privateDir, resolveProjectWorkspace(privateDir, "", storageAccess = true, externalExists = true))
    }

    @Test
    fun externalRootWithAccessAndExistingFolderIsUsed() {
        assertEquals(File(external), resolveProjectWorkspace(privateDir, external, storageAccess = true, externalExists = true))
    }

    @Test
    fun withoutAccessFallsBackToPrivateWorkspace() {
        assertEquals(privateDir, resolveProjectWorkspace(privateDir, external, storageAccess = false, externalExists = true))
    }

    @Test
    fun missingExternalFolderFallsBackToPrivateWorkspace() {
        assertEquals(privateDir, resolveProjectWorkspace(privateDir, external, storageAccess = true, externalExists = false))
    }

    @Test
    fun fallbackIsReportedSoTheUserSeesAWarning() {
        assertTrue(isExternalWorkspaceUnavailable(external, storageAccess = false, externalExists = true))
        assertTrue(isExternalWorkspaceUnavailable(external, storageAccess = true, externalExists = false))
        assertFalse(isExternalWorkspaceUnavailable("", storageAccess = false, externalExists = false))
        assertFalse(isExternalWorkspaceUnavailable(external, storageAccess = true, externalExists = true))
    }
}
