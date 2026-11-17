package com.jarves.mh.runtime

import java.io.File
import org.junit.Assert.assertEquals
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
}
