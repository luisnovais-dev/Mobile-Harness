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
