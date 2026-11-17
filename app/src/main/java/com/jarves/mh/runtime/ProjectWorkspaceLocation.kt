package com.jarves.mh.runtime

import java.io.File

/**
 * Escolhe a pasta de trabalho do projeto (Spec #5, RF-4 e RF-14).
 * Usa a pasta externa só com permissão e com a pasta existente; caso contrário, a área privada.
 */
internal fun resolveProjectWorkspace(privateDir: File, externalRoot: String, storageAccess: Boolean, externalExists: Boolean): File =
    if (externalRoot.isNotBlank() && storageAccess && externalExists) File(externalRoot) else privateDir

/** Verdadeiro quando o projeto tem pasta externa, mas ela não pode ser usada agora (aviso ao usuário). */
internal fun isExternalWorkspaceUnavailable(externalRoot: String, storageAccess: Boolean, externalExists: Boolean): Boolean =
    externalRoot.isNotBlank() && !(storageAccess && externalExists)

/**
 * Pasta escolhida na criação: "" para área privada, o caminho validado quando há permissão,
 * ou null quando não pode ser usada (sem permissão ou caminho inválido).
 */
internal fun validatedNewProjectFolder(requested: String, storageAccess: Boolean): String? {
    if (requested.isBlank()) return ""
    val safe = storedExternalRoot(requested).takeIf { it.isNotBlank() } ?: return null
    return safe.takeIf { storageAccess }
}

/** Apagar a pasta do aparelho exige confirmação explícita e uma raiz válida (Spec #5, RF-11). */
internal fun shouldDeleteExternalFolder(externalRoot: String, confirmed: Boolean): Boolean =
    confirmed && storedExternalRoot(externalRoot).isNotBlank()
