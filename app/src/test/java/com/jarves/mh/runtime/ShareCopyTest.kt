package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCopyTest {
    private val cache = File("/data/user/0/com.jarves.mh/cache")

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
