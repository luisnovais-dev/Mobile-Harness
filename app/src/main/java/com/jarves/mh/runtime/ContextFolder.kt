package com.jarves.mh.runtime

import java.io.File

/**
 * Verdadeiro só para um arquivo que é filho direto da pasta autorizada (Spec #5, RF-9).
 * Usa caminhos canônicos, então links simbólicos e `..` não escapam da pasta.
 */
internal fun contextFileWithinFolder(folder: File, candidate: File): Boolean {
    val folderPath = folder.canonicalFile
    val target = candidate.canonicalFile
    return target.isFile && target.parentFile == folderPath
}

/** Tipo do anexo: o do provedor quando existe; senão, pela extensão (F5). Arquivo de pasta não tem tipo do provedor. */
internal fun attachmentMimeType(resolved: String?, extension: String): String =
    resolved?.takeIf { it.isNotBlank() }
        ?: attachmentImageMimeTypes[extension.lowercase()]
        ?: "application/octet-stream"

private val attachmentImageMimeTypes = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "bmp" to "image/bmp",
    "heic" to "image/heic",
    "heif" to "image/heif",
)
