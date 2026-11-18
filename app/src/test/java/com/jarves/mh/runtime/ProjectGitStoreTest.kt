package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
