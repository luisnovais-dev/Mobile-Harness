package com.jarves.mh.runtime

/**
 * Converte e valida caminhos do armazenamento principal do aparelho (ADR 0002, Spec #5).
 * Só o volume primário (`primary:`) é aceito. A raiz do volume é recusada.
 */
internal object SharedStoragePath {
    const val PRIMARY_ROOT = "/storage/emulated/0"
    private const val PRIMARY_PREFIX = "primary:"

    /** Converte um `documentId` do seletor do sistema em caminho absoluto, ou devolve null se inválido. */
    fun fromDocumentId(documentId: String): String? {
        if (!documentId.startsWith(PRIMARY_PREFIX)) return null
        val relative = documentId.removePrefix(PRIMARY_PREFIX)
        if (relative.isBlank() || relative.startsWith("/") || relative.contains('\u0000')) return null
        val segments = relative.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) return null
        return "$PRIMARY_ROOT/${segments.joinToString("/")}"
    }

    /** Verdadeiro quando o caminho salvo é uma pasta dentro do volume primário, abaixo da raiz. */
    fun isValidExternalRoot(path: String): Boolean {
        if (!path.startsWith("$PRIMARY_ROOT/")) return false
        if (path.contains('\u0000')) return false
        val relative = path.removePrefix("$PRIMARY_ROOT/")
        if (relative.isBlank()) return false
        return relative.split('/').none { it.isBlank() || it == "." || it == ".." }
    }
}

/** Caminho externo guardado no projeto, ou vazio quando inválido (dados de preferências não são confiáveis). */
internal fun storedExternalRoot(raw: String): String =
    raw.takeIf { SharedStoragePath.isValidExternalRoot(it) } ?: ""
