package com.jarves.mh.runtime

import java.io.File

/**
 * Destino da cópia usada para compartilhar um arquivo do projeto (Spec #5, RF-10, D3).
 * Fica em cache/camera/shared/<id>/, a única raiz de cache exposta pelo file_paths.xml.
 */
internal fun shareCopyDestination(cacheDir: File, fileName: String, shareId: String): File {
    val safeName = File(fileName).name.replace(Regex("[^A-Za-z0-9._ -]"), "_").ifBlank { "arquivo" }
    return File(cacheDir, "camera/shared/$shareId/$safeName")
}

/**
 * Apaga as cópias de compartilhamento deixadas por envios anteriores (RF-10: "na próxima abertura").
 * Só remove `camera/shared/`; outros arquivos de `camera/`, como fotos, ficam.
 */
internal fun clearShareCopies(cacheDir: File) {
    File(cacheDir, "camera/shared").deleteRecursively()
}
