package com.noxis.os.system

import android.content.Context

/**
 * OTA менеджер — поки stub, архітектура готова.
 *
 * Майбутній flow:
 * 1. checkForUpdates() → сервер повертає UpdateInfo
 * 2. Якщо є оновлення → показати діалог користувачу
 * 3. downloadFirmware() → зберегти в firmwareDir
 * 4. FirmwareManager.install() + activate()
 * 5. restartNoxis() → перезапустити DesktopActivity
 *
 * Для прошивок що потребують bootloader unlock:
 * FirmwareManager.install() поверне BootloaderLocked →
 * показати екран розблокування bootloader
 */
object UpdateManager {

    data class UpdateInfo(
        val firmwareId: String,
        val version: String,
        val downloadUrl: String,
        val changelog: String,
        val requiresUnlock: Boolean,
        val channel: String  // stable / beta
    )

    // TODO: реалізувати після визначення серверного API
    fun checkForUpdates(context: Context, onResult: (UpdateInfo?) -> Unit) {
        val settings = SettingsManager.get(context)
        if (!settings.serverEnabled || settings.serverUrl.isBlank()) {
            onResult(null)
            return
        }
        // Placeholder — буде HTTP запит до settings.serverUrl
        onResult(null)
    }

    fun downloadFirmware(
        context: Context,
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        // TODO: HTTP download → SystemPaths.firmwareDir(context)
        onComplete(false)
    }

    /**
     * Перезапустити Noxis (не телефон)
     */
    fun restartNoxis(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        context.startActivity(intent)
    }
}
