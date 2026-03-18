package com.noxis.os.system

import android.content.Context
import com.noxis.os.util.SystemPaths
import java.io.File

/**
 * Єдина точка перевірки прав доступу для Lua застосунків.
 * Права визначаються активною прошивкою.
 */
object PermissionGate {

    enum class Permission {
        READ_OWN,           // читати власну папку (є завжди)
        WRITE_OWN,          // писати у власну папку (є завжди)
        READ_HOME,          // читати /NoxisOS/home/
        WRITE_HOME,         // писати у /NoxisOS/home/
        READ_OTHER_APP,     // читати дані інших застосунків (тільки root прошивка)
        WRITE_OTHER_APP,    // писати у дані інших застосунків (тільки root прошивка)
        SYSTEM_SETTINGS,    // змінювати системні налаштування (тільки root прошивка)
        READ_SECURE,        // доступ до захищених даних (тільки root прошивка)
        INSTALL_FIRMWARE,   // встановлювати прошивки (тільки root + unlock)
        NETWORK             // мережевий доступ (залежить від налаштувань)
    }

    fun check(context: Context, appId: String, permission: Permission): Boolean {
        val firmware = FirmwareManager.getActive(context)
        val kernel = KernelManager.get(context)

        return when (permission) {
            Permission.READ_OWN,
            Permission.WRITE_OWN -> true  // завжди дозволено

            Permission.READ_HOME,
            Permission.WRITE_HOME -> true  // дозволено всім

            Permission.READ_OTHER_APP,
            Permission.WRITE_OTHER_APP,
            Permission.SYSTEM_SETTINGS,
            Permission.READ_SECURE -> firmware.rootAccess && kernel.bootloaderUnlocked

            Permission.INSTALL_FIRMWARE -> firmware.rootAccess && kernel.bootloaderUnlocked

            Permission.NETWORK -> SettingsManager.get(context).serverEnabled
        }
    }

    /**
     * Резолв шляху для Lua — повертає реальний шлях або null якщо доступ заборонено
     */
    fun resolvePath(context: Context, appId: String, requestedPath: String): File? {
        return when {
            // Власна папка — завжди дозволено
            requestedPath.startsWith("app://") -> {
                File(SystemPaths.appDir(appId), requestedPath.removePrefix("app://"))
            }
            // Власні дані — завжди дозволено
            requestedPath.startsWith("data://") -> {
                File(SystemPaths.appDataDir(context, appId), requestedPath.removePrefix("data://"))
            }
            // Домашня папка користувача
            requestedPath.startsWith("home://") -> {
                File(SystemPaths.homeDir, requestedPath.removePrefix("home://"))
            }
            // Медіа
            requestedPath.startsWith("media://") -> {
                File(SystemPaths.externalRoot, "media/${requestedPath.removePrefix("media://")}")
            }
            // Системні шляхи — тільки root
            requestedPath.startsWith("system://") -> {
                if (check(context, appId, Permission.SYSTEM_SETTINGS))
                    File(SystemPaths.systemDir(context), requestedPath.removePrefix("system://"))
                else null
            }
            else -> null
        }
    }
}
