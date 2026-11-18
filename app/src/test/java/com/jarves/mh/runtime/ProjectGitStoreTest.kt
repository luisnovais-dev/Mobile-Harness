package com.jarves.mh.runtime

import java.io.File
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Spec #6 (opção 1): histórico git no rootfs privado, árvore de trabalho na pasta externa. RF-4, RF-5, CA-6..CA-8, CA-10, E3. */
class ProjectGitStoreTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun gitDirLivesInPrivateRootfsPerSlug() {
        assertEquals("/root/gd/q1probe", ProjectGitStore.guestGitDir("q1probe"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun gitDirRejectsSlugWithPathTraversal() {
        ProjectGitStore.guestGitDir("../etc")
    }

    @Test
    fun acceptsOrdinaryBranchNames() {
        assertTrue(ProjectGitStore.isValidBranch("main"))
        assertTrue(ProjectGitStore.isValidBranch("feat/6-fluxo-git"))
    }

    @Test
    fun rejectsUnsafeBranchNames() {
        listOf("", "..", "-x", "a/../b", "a b", "x.lock", "/abs", "trail/", "a//b", "ctrl\u0007").forEach { name ->
            assertFalse("should reject '$name'", ProjectGitStore.isValidBranch(name))
        }
    }

    @Test
    fun allowsHttpsAndSshWithoutSecrets() {
        assertTrue(ProjectGitStore.isAllowedCloneUrl("https://github.com/owner/repo.git"))
        assertTrue(ProjectGitStore.isAllowedCloneUrl("ssh://git@github.com/owner/repo.git"))
    }

    @Test
    fun rejectsOtherSchemesAndEmbeddedCredentials() {
        listOf(
            "http://example.com/repo.git",
            "file:///sdcard/repo",
            "git@github.com:owner/repo.git",
            "https://user:token@github.com/owner/repo.git",
            "https://token@github.com/owner/repo.git",
            "ssh://user:pass@host/repo.git",
        ).forEach { url ->
            assertFalse("should reject '$url'", ProjectGitStore.isAllowedCloneUrl(url))
        }
    }

    @Test
    fun cloneArgsUseSeparateGitDirAndCurrentDirectory() {
        val args = ProjectGitStore.cloneArgs("https://github.com/owner/repo.git", "main", "/root/gd/q1probe")
        assertEquals(listOf("git", "clone", "--progress", "--single-branch", "--branch", "main"), args.take(6))
        val sep = args.indexOf("--separate-git-dir")
        assertTrue(sep > 0)
        assertEquals("/root/gd/q1probe", args[sep + 1])
        assertEquals("https://github.com/owner/repo.git", args[args.size - 2])
        assertEquals(".", args.last())
    }

    @Test
    fun cloneArgsOmitBranchWhenAbsent() {
        val args = ProjectGitStore.cloneArgs("https://github.com/owner/repo.git", null, "/root/gd/q1probe")
        assertFalse(args.contains("--branch"))
    }

    @Test
    fun internalAndMetadataAddressesAreBlocked() {
        listOf("127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.0.1", "169.254.169.254", "0.0.0.0", "100.64.0.1", "::1", "fd00::1", "fe80::1")
            .forEach { ip -> assertTrue("should block $ip", ProjectGitStore.isInternalAddress(InetAddress.getByName(ip))) }
    }

    @Test
    fun publicAddressesAreAllowed() {
        listOf("8.8.8.8", "140.82.112.3", "2606:4700:4700::1111")
            .forEach { ip -> assertFalse("should allow $ip", ProjectGitStore.isInternalAddress(InetAddress.getByName(ip))) }
    }

    @Test
    fun localhostResolvesToInternalAddress() {
        assertTrue(ProjectGitStore.resolveHost("localhost")!!.any(ProjectGitStore::isInternalAddress))
    }

    @Test
    fun unresolvableHostIsNotMistakenForInternal() {
        // .invalid nunca resolve (RFC 2606): null, para a mensagem de host não resolvido.
        assertNull(ProjectGitStore.resolveHost("nome-que-nao-existe.invalid"))
    }

    @Test
    fun gitFileMissingTargetIsReported() {
        val rootfs = temp.newFolder("rootfs2")
        val folder = temp.newFolder("folder")
        File(folder, ".git").writeText("gitdir: /root/gd/sumiu\n")
        assertTrue(ProjectGitStore.isGitDirMissingFor(rootfs, folder))
        File(rootfs, "root/gd/sumiu").mkdirs()
        assertFalse(ProjectGitStore.isGitDirMissingFor(rootfs, folder))
    }

    @Test
    fun workspaceSubfolderUsesNearestAncestorGitFile() {
        // rootPath: o workspace é subpasta; o .git fica na raiz da pasta externa (como o git faz).
        val rootfs = temp.newFolder("rootfs4")
        val externa = temp.newFolder("externa")
        File(externa, ".git").writeText("gitdir: /root/gd/ausente\n")
        val sub = File(externa, "app").apply { mkdirs() }
        assertTrue(ProjectGitStore.isGitDirMissingForWorkspace(rootfs, sub))
        File(rootfs, "root/gd/ausente").mkdirs()
        assertFalse(ProjectGitStore.isGitDirMissingForWorkspace(rootfs, sub))
    }

    @Test
    fun folderWithoutGitFileOrForeignGitFileIsNotReported() {
        val rootfs = temp.newFolder("rootfs3")
        val semGit = temp.newFolder("sem-git")
        assertFalse(ProjectGitStore.isGitDirMissingFor(rootfs, semGit))
        val outro = temp.newFolder("outro")
        File(outro, ".git").writeText("gitdir: /data/outro/.git\n")
        assertFalse(ProjectGitStore.isGitDirMissingFor(rootfs, outro))
    }

    @Test
    fun filemodeIsDisabledForExternalFolder() {
        // Pasta externa não guarda bit de execução: sem isso, cada arquivo com +x aparece como modificado.
        assertEquals(listOf("git", "config", "core.filemode", "false"), ProjectGitStore.filemodeConfigArgs())
    }

    @Test
    fun worktreeExcludeIsWrittenOnceInPrivateGitDir() {
        val rootfs = temp.newFolder("rootfs")
        val exclude = ProjectGitStore.excludeFile(rootfs, "q1probe")
        ProjectGitStore.addWorktreeExclude(exclude)
        ProjectGitStore.addWorktreeExclude(exclude)
        val lines = exclude.readLines().filter { it == ".worktrees/" }
        assertEquals(1, lines.size)
    }

    @Test
    fun gitDirMissingWhenRootfsHasNoDirectory() {
        val rootfs = temp.newFolder("rootfs")
        assertTrue(ProjectGitStore.isGitDirMissing(rootfs, "q1probe"))
        File(rootfs, "root/gd/q1probe").mkdirs()
        assertFalse(ProjectGitStore.isGitDirMissing(rootfs, "q1probe"))
    }
}
