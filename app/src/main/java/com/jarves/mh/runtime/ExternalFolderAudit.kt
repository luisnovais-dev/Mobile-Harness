package com.jarves.mh.runtime

import java.io.File
import java.time.Instant

/** Ações sobre pastas do aparelho que a Spec #5 exige registrar (Seg. 21). */
enum class ExternalFolderAction { READ, DELETE, RESTORE }

/**
 * Uma linha do registro: instante, ação, resultado e caminho. Nunca traz conteúdo de arquivo.
 * Tabulação e quebra de linha no caminho são trocadas por espaço, para que a linha não se divida.
 */
internal fun auditLine(timestampMs: Long, action: ExternalFolderAction, result: String, folderPath: String): String {
    val safePath = folderPath.replace(Regex("[\\t\\r\\n]"), " ")
    return listOf(Instant.ofEpochMilli(timestampMs).toString(), action.name.lowercase(), result, safePath)
        .joinToString("\t")
}

/** Registro local e append-only das ações sobre pastas externas. Falha de gravação não interrompe a ação. */
class ExternalFolderAuditLog(
    private val file: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun record(action: ExternalFolderAction, result: String, folderPath: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText(auditLine(now(), action, result, folderPath) + "\n")
        }
    }
}
