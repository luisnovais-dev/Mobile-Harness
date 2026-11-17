package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Spec #5, Seg. 21: ações sobre pastas externas registram quem, quando, o quê e o resultado, sem conteúdo. */
class ExternalFolderAuditTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun lineHasTimeActionResultAndPathOnly() {
        val line = auditLine(1000L, ExternalFolderAction.DELETE, "ok", "/storage/emulated/0/MeuProjeto")
        assertEquals("1970-01-01T00:00:01Z\tdelete\tok\t/storage/emulated/0/MeuProjeto", line)
    }

    @Test
    fun tabsAndNewlinesInPathCannotForgeAnotherLine() {
        val line = auditLine(0L, ExternalFolderAction.READ, "ok", "/storage/a\tb\nc")
        assertEquals(1, line.lines().size)
    }

    @Test
    fun recordAppendsOneLinePerEvent() {
        val file = temp.newFile("external-folders.log")
        val log = ExternalFolderAuditLog(file) { 2000L }
        log.record(ExternalFolderAction.READ, "ok", "/storage/emulated/0/MeuProjeto")
        log.record(ExternalFolderAction.RESTORE, "refused", "/storage/emulated/0/MeuProjeto")
        assertEquals(2, file.readLines().size)
    }

    @Test
    fun firstRecordCreatesTheAuditFolder() {
        val file = File(temp.root, "audit/external-folders.log")
        ExternalFolderAuditLog(file) { 0L }.record(ExternalFolderAction.READ, "ok", "/storage/emulated/0/MeuProjeto")
        assertEquals(1, file.readLines().size)
    }

    @Test
    fun failureToWriteDoesNotBreakTheAction() {
        val blocker = temp.newFile("blocker")
        val log = ExternalFolderAuditLog(File(blocker, "audit.log")) { 0L }
        log.record(ExternalFolderAction.READ, "ok", "/storage/emulated/0/MeuProjeto")
    }
}
