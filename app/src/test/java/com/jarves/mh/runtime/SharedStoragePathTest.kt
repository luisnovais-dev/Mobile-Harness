package com.jarves.mh.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedStoragePathTest {
    @Test
    fun primaryDocumentIdBecomesPrimaryStoragePath() {
        assertEquals("/storage/emulated/0/MeuProjeto", SharedStoragePath.fromDocumentId("primary:MeuProjeto"))
        assertEquals("/storage/emulated/0/A/B", SharedStoragePath.fromDocumentId("primary:A/B"))
    }

    @Test
    fun rejectsRootOfVolumeAndOtherVolumes() {
        assertNull(SharedStoragePath.fromDocumentId("primary:"))
        assertNull(SharedStoragePath.fromDocumentId("1234-ABCD:Projeto"))
        assertNull(SharedStoragePath.fromDocumentId("home:Projeto"))
    }

    @Test
    fun rejectsTraversalAbsoluteAndNullBytes() {
        assertNull(SharedStoragePath.fromDocumentId("primary:../x"))
        assertNull(SharedStoragePath.fromDocumentId("primary:a/../../b"))
        assertNull(SharedStoragePath.fromDocumentId("primary:/abs"))
        assertNull(SharedStoragePath.fromDocumentId("primary:a\u0000b"))
        assertNull(SharedStoragePath.fromDocumentId("primary:a//b"))
    }

    @Test
    fun validExternalRootMustStayUnderPrimaryStorage() {
        assertTrue(SharedStoragePath.isValidExternalRoot("/storage/emulated/0/MeuProjeto"))
        assertFalse(SharedStoragePath.isValidExternalRoot("/storage/emulated/0"))
        assertFalse(SharedStoragePath.isValidExternalRoot("/storage/emulated/0/../../data"))
        assertFalse(SharedStoragePath.isValidExternalRoot("/data/user/0/com.jarves.mh/files/workspaces/x"))
        assertFalse(SharedStoragePath.isValidExternalRoot(""))
    }
}

class StoredExternalRootTest {
    @Test
    fun validStoredRootIsKeptAndInvalidOneIsDropped() {
        assertEquals("/storage/emulated/0/MeuProjeto", storedExternalRoot("/storage/emulated/0/MeuProjeto"))
        assertEquals("", storedExternalRoot("/storage/emulated/0/../../data"))
        assertEquals("", storedExternalRoot("/data/user/0/x"))
        assertEquals("", storedExternalRoot(""))
    }
}
