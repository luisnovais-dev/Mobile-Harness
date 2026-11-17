package com.jarves.mh.runtime

import java.io.File

/**
 * Binds `-b` da sessão PRoot, na mesma ordem de sempre (Spec #5, RF-5, ADR 0002).
 * A sessão recebe o sistema do aparelho e só a pasta do projeto como área de trabalho.
 * Nunca recebe o volume compartilhado (`/storage/emulated/0`, `/sdcard`).
 */
internal fun prootWorkspaceBinds(
    rootfs: File,
    workspace: File,
    bridge: File,
    guestWorkspacePath: String,
    hostPathExists: (String) -> Boolean = { File(it).exists() },
): List<String> = buildList {
    add("-b")
    add("/dev")
    add("-b")
    add("/proc")
    add("-b")
    add("/sys")
    // ARM64 Android build tools (notably aapt2) use Bionic's
    // /system/bin/linker64 and, on newer releases, APEX libraries.
    listOf("/system", "/apex", "/vendor", "/product").forEach { hostPath ->
        if (hostPathExists(hostPath)) {
            File(rootfs, hostPath.removePrefix("/")).mkdirs()
            add("-b")
            add(hostPath)
        }
    }
    add("-b")
    add("${workspace.absolutePath}:$guestWorkspacePath")
    add("-b")
    add("${bridge.absolutePath}:/pocket-bridge")
}
