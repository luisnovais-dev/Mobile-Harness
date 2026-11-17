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

/**
 * Apagar a pasta do aparelho exige confirmação explícita, uma raiz válida e que nenhum outro projeto
 * use a mesma pasta (Spec #5, RF-11; F2).
 */
internal fun shouldDeleteExternalFolder(
    externalRoot: String,
    confirmed: Boolean,
    sharedWithAnotherProject: Boolean = false,
): Boolean = confirmed && !sharedWithAnotherProject && storedExternalRoot(externalRoot).isNotBlank()

/** Motivo pelo qual a pasta escolhida na criação não pode ser usada (N2). */
internal enum class FolderRefusal { NO_ACCESS, INVALID_FOLDER }

/** Pasta pedida na criação: o motivo de recusa, ou null quando pode ser usada. A pasta inválida vem antes da falta de acesso (N2). */
internal fun newFolderRefusal(requested: String, storageAccess: Boolean): FolderRefusal? = when {
    requested.isBlank() -> null
    storedExternalRoot(requested).isBlank() -> FolderRefusal.INVALID_FOLDER
    !storageAccess -> FolderRefusal.NO_ACCESS
    else -> null
}

/**
 * Verdadeiro quando as duas pastas são a mesma ou uma está dentro da outra (N6).
 * Assim, excluir ou restaurar uma nunca mexe na área de outro projeto.
 */
internal fun foldersOverlap(first: String, second: String): Boolean =
    first == second || first.startsWith("$second/") || second.startsWith("$first/")

/** Verdadeiro quando a pasta pedida se sobrepõe à de outro projeto; dois projetos não compartilham área (F2, N6). */
internal fun folderUsedByAnotherProject(requested: String, existingRoots: Collection<String>): Boolean =
    requested.isNotBlank() && existingRoots.any { it.isNotBlank() && foldersOverlap(requested, it) }
