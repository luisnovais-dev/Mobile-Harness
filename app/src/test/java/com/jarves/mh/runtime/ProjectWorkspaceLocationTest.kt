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

    /** F2: a pasta que outro projeto também usa não é apagada; os arquivos dele ficariam perdidos. */
    @Test
    fun folderSharedWithAnotherProjectIsNotDeleted() {
        val root = "/storage/emulated/0/MeuProjeto"
        assertFalse(shouldDeleteExternalFolder(root, confirmed = true, sharedWithAnotherProject = true))
        assertTrue(shouldDeleteExternalFolder(root, confirmed = true, sharedWithAnotherProject = false))
    }
}

class NewProjectFolderRefusalTest {
    private val root = "/storage/emulated/0/MeuProjeto"

    /** N2: uma raiz inválida é recusada como pasta inválida, mesmo com acesso ao armazenamento. */
    @Test
    fun invalidFolderIsReportedAsInvalidEvenWithAccess() {
        assertEquals(FolderRefusal.INVALID_FOLDER, newFolderRefusal("/storage/emulated/0", storageAccess = true))
        assertEquals(FolderRefusal.INVALID_FOLDER, newFolderRefusal("/data/user/0/com.jarves.mh/files", storageAccess = true))
        assertEquals(FolderRefusal.INVALID_FOLDER, newFolderRefusal("/storage/emulated/0/../../data", storageAccess = true))
    }

    @Test
    fun validFolderWithoutAccessIsReportedAsNoAccess() {
        assertEquals(FolderRefusal.NO_ACCESS, newFolderRefusal(root, storageAccess = false))
    }

    @Test
    fun validFolderWithAccessAndPrivateAreaAreAccepted() {
        assertNull(newFolderRefusal(root, storageAccess = true))
        assertNull(newFolderRefusal("", storageAccess = false))
    }

    /** F2: dois projetos não podem usar a mesma pasta do aparelho. */
    @Test
    fun folderAlreadyUsedByAnotherProjectIsDetected() {
        assertTrue(folderUsedByAnotherProject(root, listOf("/storage/emulated/0/Outra", root)))
        assertFalse(folderUsedByAnotherProject(root, listOf("/storage/emulated/0/Outra")))
        assertFalse(folderUsedByAnotherProject(root, emptyList()))
        assertFalse(folderUsedByAnotherProject("", listOf("")))
    }

    /** N6: uma pasta dentro da de outro projeto, ou que a contém, é a mesma área de trabalho. */
    @Test
    fun folderInsideOrAroundAnotherProjectFolderIsDetected() {
        val outer = "/storage/emulated/0/Foo"
        assertTrue(folderUsedByAnotherProject("$outer/Bar", listOf(outer)))
        assertTrue(folderUsedByAnotherProject(outer, listOf("$outer/Bar")))
        assertFalse(folderUsedByAnotherProject("/storage/emulated/0/Foobar", listOf(outer)))
    }
}
