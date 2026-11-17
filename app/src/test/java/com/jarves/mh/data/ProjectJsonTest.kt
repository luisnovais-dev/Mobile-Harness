package com.jarves.mh.data

import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectKind
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** F6, Spec #5 Task 1 e Seg. 24: persistência de `externalRoot`, com JSON antigo sem o campo. */
class ProjectJsonTest {
    private fun project(externalRoot: String = "") = Project(
        id = "p1",
        name = "Shop",
        description = "",
        language = "Kotlin",
        externalRoot = externalRoot,
        kind = ProjectKind.PROJECT,
    )

    @Test
    fun externalRootSurvivesSaveAndLoad() {
        val root = "/storage/emulated/0/MeuProjeto"
        assertEquals(root, projectExternalRootFromJson(projectToJson(project(root))))
    }

    @Test
    fun projectWithoutExternalRootWritesNoField() {
        assertFalse(projectToJson(project()).has("externalRoot"))
    }

    @Test
    fun olderJsonWithoutFieldLoadsAsPrivateProject() {
        assertEquals("", projectExternalRootFromJson(JSONObject().put("id", "p1").put("name", "Shop")))
    }

    @Test
    fun storedRootOutsidePrimaryVolumeIsDropped() {
        assertEquals("", projectExternalRootFromJson(JSONObject().put("externalRoot", "/data/user/0/com.jarves.mh/files")))
    }
}
