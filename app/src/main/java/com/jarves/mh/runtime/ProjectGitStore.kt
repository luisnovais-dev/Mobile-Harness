package com.jarves.mh.runtime

import java.io.File
import java.net.InetAddress

/**
 * Histórico git de projetos em pasta externa (Spec #6, opção 1).
 * O histórico (`gitdir`) fica no rootfs privado; a pasta externa guarda os arquivos e a árvore de trabalho.
 * Git sobre o armazenamento compartilhado não grava objetos nem índice (ver ADR 0002), por isso o histórico não vai para lá.
 */
internal object ProjectGitStore {
    const val GUEST_GIT_ROOT = "/root/gd"
    const val WORKTREE_DIR = ".worktrees/"

    private val SLUG = Regex("^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$")
    private val BRANCH_CHARS = Regex("^[A-Za-z0-9._/-]+$")
    private val GIT_WORD = Regex("(^|[^A-Za-z0-9_])git([^A-Za-z0-9_]|$)")

    /** Caminho do `gitdir` dentro da sessão. Recusa slug fora do formato do projeto. */
    fun guestGitDir(slug: String): String {
        require(SLUG.matches(slug)) { "Invalid project slug" }
        return "$GUEST_GIT_ROOT/$slug"
    }

    /** RF-4: nome de branch sem `..`, `//`, `-` inicial, `/` nas pontas, `.lock` final ou caracteres de controle. */
    fun isValidBranch(name: String): Boolean {
        if (name.isEmpty() || !BRANCH_CHARS.matches(name)) return false
        if (name.startsWith("-") || name.startsWith("/") || name.endsWith("/")) return false
        if (name.endsWith(".") || name.endsWith(".lock") || name.contains("..") || name.contains("//")) return false
        return true
    }

    /** RF-5 e CA-8: só `https` e `ssh`, sem senha na URL. Usuário de ssh (`git@`) é permitido. */
    fun isAllowedCloneUrl(url: String): Boolean {
        val lower = url.lowercase()
        val scheme = when {
            lower.startsWith("https://") -> "https"
            lower.startsWith("ssh://") -> "ssh"
            else -> return false
        }
        val authority = url.substringAfter("://").substringBefore("/")
        if (!authority.contains("@")) return true
        if (scheme == "https") return false
        return !authority.substringBefore("@").contains(":")
    }

    /** Argumentos do clone: histórico no `gitdir` privado; a árvore de trabalho é o diretório atual (pasta externa). */
    fun cloneArgs(url: String, branch: String?, guestGitDir: String): List<String> = buildList {
        addAll(listOf("git", "clone", "--progress", "--single-branch"))
        branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
        addAll(listOf("--separate-git-dir", guestGitDir))
        add(url)
        add(".")
    }

    /**
     * Pasta externa não guarda bit de execução. O `git init` com `--separate-git-dir` testa o modo no rootfs
     * e grava `core.filemode=true`, o que faria cada arquivo com `+x` aparecer como modificado.
     */
    fun filemodeConfigArgs(): List<String> = listOf("git", "config", "core.filemode", "false")

    /** RS-17 e decisão 3: endereço interno, de loopback, link-local (inclui metadados de nuvem) ou CGNAT. */
    fun isInternalAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isSiteLocalAddress) return true
        if (address.isLinkLocalAddress || address.isMulticastAddress) return true
        val bytes = address.address
        if (bytes.size == 4) {
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            // 0.0.0.0/8 e 100.64.0.0/10 (CGNAT)
            if (first == 0 || (first == 100 && second in 64..127)) return true
        }
        // fc00::/7 (unique local)
        if (bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC) return true
        return false
    }

    /** RS-17: resolve o host antes do clone. Falha de resolução conta como bloqueio. */
    fun resolvesToInternal(host: String): Boolean =
        runCatching { InetAddress.getAllByName(host).any(::isInternalAddress) }.getOrDefault(true)

    /** E3: comando que chama `git` como palavra (não `gitignore`, `mygit`). */
    fun commandUsesGit(command: String): Boolean = GIT_WORD.containsMatchIn(command)

    /** `info/exclude` do `gitdir` privado, no rootfs do aparelho. */
    fun excludeFile(rootfs: File, slug: String): File =
        File(rootfs, "${guestGitDir(slug).removePrefix("/")}/info/exclude")

    /** Decisão 2: `.worktrees/` fica fora do `git status`, sem alterar arquivos versionados. Idempotente. */
    fun addWorktreeExclude(exclude: File) {
        exclude.parentFile?.mkdirs()
        val current = if (exclude.exists()) exclude.readLines() else emptyList()
        if (current.contains(WORKTREE_DIR)) return
        exclude.appendText(WORKTREE_DIR + "\n")
    }

    /**
     * E3: a pasta externa tem `.git` apontando para `/root/gd/...`, e esse gitdir não existe mais.
     * Projeto sem `.git` ou com `.git` de outro lugar não conta: não foi clonado com o histórico privado.
     */
    fun isGitDirMissingFor(rootfs: File, folder: File): Boolean {
        val gitFile = File(folder, ".git")
        if (!gitFile.isFile) return false
        val target = gitFile.readText().trim().removePrefix("gitdir:").trim()
        if (!target.startsWith("$GUEST_GIT_ROOT/")) return false
        return !File(rootfs, target.removePrefix("/")).isDirectory
    }

    /** E3: o `gitdir` some quando os dados do app são limpos ou o runtime é reinstalado. */
    fun isGitDirMissing(rootfs: File, slug: String): Boolean =
        !File(rootfs, guestGitDir(slug).removePrefix("/")).isDirectory
}
