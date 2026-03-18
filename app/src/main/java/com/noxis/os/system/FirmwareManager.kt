package com.noxis.os.system

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.noxis.os.util.SystemPaths
import java.io.File
import java.util.zip.ZipInputStream

data class FirmwareManifest(
    @SerializedName("id")          val id: String = "",
    @SerializedName("name")        val name: String = "",
    @SerializedName("version")     val version: String = "1.0.0",
    @SerializedName("author")      val author: String = "",
    @SerializedName("description") val description: String = "",
    // Чи потрібен розблокований bootloader для цієї прошивки
    @SerializedName("requires_unlock") val requiresUnlock: Boolean = false,
    // Чи дає розширені права застосункам
    @SerializedName("root_access") val rootAccess: Boolean = false
)

/**
 * Прошивки зберігаються ТІЛЬКИ в /data/data/com.noxis.os/files/system/firmware/
 * Недоступні через /0/NoxisOS/ — захист від ручної заміни
 */
object FirmwareManager {

    private val gson = Gson()

    fun getActive(context: Context): FirmwareManifest {
        val settings = SettingsManager.get(context)
        val firmwareDir = File(SystemPaths.firmwareDir(context), settings.activeFirmware)
        val manifestFile = File(firmwareDir, "manifest.json")
        return if (manifestFile.exists()) {
            try { gson.fromJson(manifestFile.readText(), FirmwareManifest::class.java) }
            catch (e: Exception) { stockFirmware() }
        } else {
            stockFirmware()
        }
    }

    /**
     * Встановити прошивку з .nxfw файлу
     * Потребує розблокованого bootloader якщо прошивка це вимагає
     */
    fun install(context: Context, nxfwFile: File): FirmwareInstallResult {
        val kernel = KernelManager.get(context)

        // Читаємо маніфест
        val manifest = readManifest(nxfwFile)
            ?: return FirmwareInstallResult.Error("Невалідний файл прошивки")

        // Перевірка bootloader
        if (manifest.requiresUnlock && !kernel.bootloaderUnlocked) {
            return FirmwareInstallResult.BootloaderLocked
        }

        val targetDir = File(SystemPaths.firmwareDir(context), manifest.id)
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        return try {
            ZipInputStream(nxfwFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val out = File(targetDir, entry.name)
                    if (entry.isDirectory) out.mkdirs()
                    else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            FirmwareInstallResult.Success(manifest)
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            FirmwareInstallResult.Error("Помилка: ${e.message}")
        }
    }

    /**
     * Активувати встановлену прошивку (потребує перезапуску Noxis)
     */
    fun activate(context: Context, firmwareId: String): Boolean {
        val firmwareDir = File(SystemPaths.firmwareDir(context), firmwareId)
        if (!firmwareDir.exists()) return false
        val manifest = try {
            gson.fromJson(File(firmwareDir, "manifest.json").readText(), FirmwareManifest::class.java)
        } catch (e: Exception) { return false }

        SettingsManager.update(context) {
            copy(activeFirmware = firmwareId, firmwareVersion = manifest.version)
        }
        return true
    }

    fun getInstalled(context: Context): List<FirmwareManifest> {
        return SystemPaths.firmwareDir(context).listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull {
                try { gson.fromJson(File(it, "manifest.json").readText(), FirmwareManifest::class.java) }
                catch (e: Exception) { null }
            } ?: emptyList()
    }

    private fun readManifest(file: File): FirmwareManifest? {
        return try {
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.json")
                        return gson.fromJson(zip.bufferedReader().readText(), FirmwareManifest::class.java)
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        } catch (e: Exception) { null }
    }

    private fun stockFirmware() = FirmwareManifest(
        id = "com.noxis.stock",
        name = "Noxis Stock",
        version = "1.0.0",
        author = "Noxis",
        requiresUnlock = false,
        rootAccess = false
    )
}

sealed class FirmwareInstallResult {
    data class Success(val firmware: FirmwareManifest) : FirmwareInstallResult()
    data class Error(val message: String) : FirmwareInstallResult()
    object BootloaderLocked : FirmwareInstallResult()
}
