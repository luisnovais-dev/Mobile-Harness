package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

class NewProjectFolderTest {
    private val root = "/storage/emulated/0/MeuProjeto"

    @Test
    fun blankChoiceMeansPrivateAreaWithoutPermissionCheck() {
        assertEquals("", validatedNewProjectFolder("", storageAccess = false))
        assertEquals("", validatedNewProjectFolder("", storageAccess = true))
    }

    @Test
    fun chosenFolderIsAcceptedOnlyWithPermission() {
        assertEquals(root, validatedNewProjectFolder(root, storageAccess = true))
        assertNull(validatedNewProjectFolder(root, storageAccess = false))
    }

    @Test
    fun invalidFolderIsRefusedEvenWithPermission() {
        assertNull(validatedNewProjectFolder("/data/user/0/com.jarves.mh/files", storageAccess = true))
        assertNull(validatedNewProjectFolder("/storage/emulated/0/../../data", storageAccess = true))
    }
}

class ExternalFolderDeletionTest {
    private val root = "/storage/emulated/0/MeuProjeto"

    @Test
    fun deletesOnlyWhenConfirmedAndRootIsValid() {
        assertTrue(shouldDeleteExternalFolder(root, confirmed = true))
        assertFalse(shouldDeleteExternalFolder(root, confirmed = false))
        assertFalse(shouldDeleteExternalFolder("", confirmed = true))
        assertFalse(shouldDeleteExternalFolder("/storage/emulated/0", confirmed = true))
        assertFalse(shouldDeleteExternalFolder("/data/user/0/com.jarves.mh/files", confirmed = true))
    }
}
