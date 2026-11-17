package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ShareCopyTest {
    private val cache = File("/data/user/0/com.jarves.mh/cache")

    @get:Rule val temp = TemporaryFolder()

    @Test
    fun clearRemovesShareCopiesButKeepsCameraFolder() {
        val cacheDir = temp.newFolder("cache")
        File(cacheDir, "camera/shared/abc").mkdirs()
        File(cacheDir, "camera/shared/abc/relatorio.pdf").writeText("x")
        File(cacheDir, "camera/foto.jpg").writeText("p")
        clearShareCopies(cacheDir)
        assertFalse(File(cacheDir, "camera/shared").exists())
        assertTrue(File(cacheDir, "camera/foto.jpg").exists())
    }

    @Test
    fun copyGoesUnderCameraFolderExposedByFileProvider() {
        val dest = shareCopyDestination(cache, "relatorio.pdf", "abc")
        assertEquals(File(cache, "camera/shared/abc/relatorio.pdf"), dest)
        assertTrue(dest.path.startsWith(File(cache, "camera").path + File.separator))
    }

    @Test
    fun fileNameCannotEscapeTheShareFolder() {
        assertEquals("evil.sh", shareCopyDestination(cache, "../../evil.sh", "abc").name)
        assertEquals("arquivo", shareCopyDestination(cache, "", "abc").name)
    }
}
