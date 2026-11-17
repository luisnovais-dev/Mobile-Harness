package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Spec #5, RF-7: o store de checkpoints usa a base escolhida pelo ViewModel, a mesma dos bridges. */
class WorkspaceCheckpointsBaseTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun projectWithoutConfiguredBaseUsesPrivateWorkspace() {
        val files = temp.newFolder("files")
        val checkpoints = WorkspaceCheckpoints(files)
        assertEquals(File(files, "workspaces/p1").canonicalFile, checkpoints.ensureWorkspace("p1"))
    }

    @Test
    fun configuredExternalBaseIsUsedForProjectWorkspace() {
        val checkpoints = WorkspaceCheckpoints(temp.newFolder("files"))
        val external = temp.newFolder("MeuProjeto")
        checkpoints.configureProjectBase("p1", external)
        assertEquals(external.canonicalFile, checkpoints.ensureWorkspace("p1"))
    }

    @Test
    fun rootPathStaysInsideExternalBase() {
        val checkpoints = WorkspaceCheckpoints(temp.newFolder("files"))
        val external = temp.newFolder("MeuProjeto")
        checkpoints.configureProjectBase("p1", external)
        checkpoints.configureProjectRoot("p1", "app")
        assertEquals(File(external, "app").canonicalFile, checkpoints.ensureWorkspace("p1"))
    }

    /** F3: a pasta externa some ou perde acesso depois de abrir o projeto. Não é recriada. */
    @Test
    fun missingExternalBaseIsNotCreated() {
        val checkpoints = WorkspaceCheckpoints(temp.newFolder("files"))
        val missing = File(temp.root, "device/MeuProjeto-gone")
        checkpoints.configureProjectBase("p1", missing)
        try {
            checkpoints.ensureWorkspace("p1")
            fail("a missing external folder must not be created")
        } catch (expected: IllegalStateException) {
            assertFalse(missing.exists())
        }
    }

    /** F3 (RF-14): a base é resolvida a cada uso; sem permissão, a sessão volta para a área privada. */
    @Test
    fun baseIsResolvedAgainOnEachUse() {
        val files = temp.newFolder("files")
        val checkpoints = WorkspaceCheckpoints(files)
        val external = temp.newFolder("MeuProjeto7")
        var granted = true
        checkpoints.configureProjectBase("p1") { if (granted) external else File(files, "workspaces/p1") }
        assertEquals(external.canonicalFile, checkpoints.ensureWorkspace("p1"))
        granted = false
        assertEquals(File(files, "workspaces/p1").canonicalFile, checkpoints.ensureWorkspace("p1"))
    }
}
