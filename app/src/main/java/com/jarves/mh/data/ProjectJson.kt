package com.jarves.mh.data

import com.jarves.mh.model.Project
import com.jarves.mh.runtime.storedExternalRoot
import org.json.JSONObject

/** Forma salva de um projeto em `projects_json` (Spec #5, RF-3). `externalRoot` só é gravado quando existe. */
internal fun projectToJson(project: Project): JSONObject = JSONObject().apply {
    put("id", project.id)
    put("name", project.name)
    put("description", project.description)
    put("language", project.language)
    put("slug", project.slug)
    put("rootPath", project.rootPath)
    if (project.externalRoot.isNotBlank()) put("externalRoot", project.externalRoot)
    put("updatedAtMillis", project.updatedAtMillis)
    put("kind", project.kind.name)
    put("type", project.type.name)
    project.androidTemplate?.let { put("androidTemplate", it.name) }
}

/** Pasta externa lida do JSON salvo. Ausente ou inválida vira "" (área privada), como em JSON antigo. */
internal fun projectExternalRootFromJson(obj: JSONObject): String = storedExternalRoot(obj.optString("externalRoot", ""))
