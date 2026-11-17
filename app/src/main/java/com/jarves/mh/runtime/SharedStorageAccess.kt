package com.jarves.mh.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import com.jarves.mh.BuildConfig

/**
 * Decide se a sessão pode montar o armazenamento compartilhado do aparelho.
 *
 * Build sideload: `WRITE_EXTERNAL_STORAGE` e `READ_EXTERNAL_STORAGE` concedidas em runtime.
 * Build Play: `MANAGE_EXTERNAL_STORAGE` (acesso a todos os arquivos), exigida em API 30+.
 * Ver a Spec da issue #4 e a ADR de acesso ao armazenamento.
 */
internal object SharedStorageAccess {
    fun isGranted(
        sdkInt: Int,
        isPlayBuild: Boolean,
        isManager: Boolean,
        writeGranted: Boolean,
        readGranted: Boolean,
    ): Boolean {
        if (isPlayBuild) return sdkInt >= 30 && isManager
        return writeGranted && readGranted
    }
}

/** Estado atual da permissão, lido do sistema e de BuildConfig. */
internal fun currentSharedStorageAccess(context: Context): Boolean = SharedStorageAccess.isGranted(
    sdkInt = Build.VERSION.SDK_INT,
    isPlayBuild = BuildConfig.IS_PLAY_BUILD,
    isManager = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager(),
    writeGranted = context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED,
    readGranted = context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED,
)

/** Ação extra quando o pedido de permissão não resolve, por negação permanente. */
internal enum class StorageRequestFallback { NONE, OPEN_APP_SETTINGS }

/**
 * Sideload: sem a permissão e sem poder pedir de novo (negação permanente), o diálogo
 * não aparece mais; a única saída é a tela de configurações do app.
 * Play: o pedido é feito na tela de acesso a todos os arquivos, então não há fallback aqui.
 */
internal fun storageRequestFallback(isPlayBuild: Boolean, granted: Boolean, canAskAgain: Boolean): StorageRequestFallback =
    if (isPlayBuild || granted || canAskAgain) StorageRequestFallback.NONE else StorageRequestFallback.OPEN_APP_SETTINGS
