package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContextFolderTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun directChildFileIsAccepted() {
        val folder = tmp.newFolder("pasta")
        val file = File(folder, "notas.txt").apply { writeText("x") }
        assertTrue(contextFileWithinFolder(folder, file))
    }

    @Test
    fun filesOutsideTheFolderAreRefused() {
        val folder = tmp.newFolder("pasta")
        val outside = File(tmp.root, "fora.txt").apply { writeText("x") }
        assertFalse(contextFileWithinFolder(folder, outside))
        assertFalse(contextFileWithinFolder(folder, File(folder, "../fora.txt")))
    }

    @Test
    fun directoriesAndNestedFilesAreRefused() {
        val folder = tmp.newFolder("pasta")
        val sub = File(folder, "sub").apply { mkdirs() }
        val nested = File(sub, "a.txt").apply { writeText("x") }
        assertFalse(contextFileWithinFolder(folder, sub))
        assertFalse(contextFileWithinFolder(folder, nested))
    }
}
