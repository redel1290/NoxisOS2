package com.noxis.os.system

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.noxis.os.util.SystemPaths

/**
 * Стан ядра Noxis OS.
 * Записується один раз при першому запуску.
 * ЖОДНА прошивка не може змінити ці значення —
 * перевірка вшита в код, не в дані.
 */
data class KernelInfo(
    @SerializedName("install_date")   val installDate: Long = System.currentTimeMillis(),
    @SerializedName("noxis_version")  val noxisVersion: String = BuildConfig.NOXIS_VERSION,
    @SerializedName("bootloader_unlocked") val bootloaderUnlocked: Boolean = false,
    // Якщо хоч раз був розблокований — назавжди неофіційний
    // Прошивка НЕ може змінити це поле
    @SerializedName("ever_unlocked")  val everUnlocked: Boolean = false,
    @SerializedName("unlock_date")    val unlockDate: Long? = null
) {
    val officialStatus: OfficialStatus
        get() = if (everUnlocked) OfficialStatus.UNOFFICIAL else OfficialStatus.OFFICIAL

    enum class OfficialStatus { OFFICIAL, UNOFFICIAL }
}

object BuildConfig {
    const val NOXIS_VERSION = "2.0.0"
    const val NOXIS_VERSION_CODE = 2
}

/**
 * Менеджер ядра — єдине місце де можна читати/писати KernelInfo.
 * Прошивки отримують тільки read-only копію.
 */
object KernelManager {

    private val gson = Gson()
    private var cached: KernelInfo? = null

    fun get(context: Context): KernelInfo {
        if (cached != null) return cached!!
        val file = SystemPaths.kernelFile(context)
        cached = if (file.exists()) {
            try {
                gson.fromJson(file.readText(), KernelInfo::class.java)
            } catch (e: Exception) {
                createNew(context)
            }
        } else {
            createNew(context)
        }
        return cached!!
    }

    /**
     * Розблокувати bootloader — викликається тільки після
     * підтвердження з сервера + підтвердження користувача
     */
    fun unlock(context: Context) {
        val current = get(context)
        // everUnlocked = true назавжди, незважаючи на прошивку
        val updated = current.copy(
            bootloaderUnlocked = true,
            everUnlocked = true,
            unlockDate = System.currentTimeMillis()
        )
        save(context, updated)
    }

    /**
     * Заблокувати bootloader (але everUnlocked залишається true)
     */
    fun lock(context: Context) {
        val current = get(context)
        val updated = current.copy(bootloaderUnlocked = false)
        save(context, updated)
    }

    /**
     * Оновити версію після OTA — єдине що може змінити прошивка
     */
    fun updateVersion(context: Context, newVersion: String) {
        val current = get(context)
        // Прошивка може міняти ТІЛЬКИ версію, не статус bootloader
        val updated = current.copy(noxisVersion = newVersion)
        save(context, updated)
    }

    private fun createNew(context: Context): KernelInfo {
        val info = KernelInfo()
        save(context, info)
        return info
    }

    private fun save(context: Context, info: KernelInfo) {
        cached = info
        SystemPaths.kernelFile(context).writeText(gson.toJson(info))
    }
}
