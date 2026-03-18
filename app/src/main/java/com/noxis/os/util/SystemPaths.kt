package com.noxis.os.util

import android.content.Context
import android.os.Environment
import java.io.File

object SystemPaths {

    // ================================================
    // ВНУТРІШНІ (захищені, недоступні користувачу)
    // /data/data/com.noxis.os/files/
    // ================================================

    fun internalRoot(context: Context) =
        context.filesDir

    // Системні дані ОС
    fun systemDir(context: Context) =
        File(internalRoot(context), "system").also { it.mkdirs() }

    // Прошивки (тільки тут, не в /0/NoxisOS/)
    fun firmwareDir(context: Context) =
        File(systemDir(context), "firmware").also { it.mkdirs() }

    // Ядро ОС — bootloader стан, версія, дата встановлення
    // НЕЗМІННИЙ прошивками
    fun kernelFile(context: Context) =
        File(systemDir(context), "kernel.json")

    // Активні налаштування системи
    fun settingsFile(context: Context) =
        File(systemDir(context), "settings.json")

    // Позиції іконок на робочому столі
    fun desktopFile(context: Context) =
        File(systemDir(context), "desktop.json")

    // Захищені дані (паролі, токени) — AES-256
    fun secureDir(context: Context) =
        File(internalRoot(context), "secure").also { it.mkdirs() }

    fun secureFile(context: Context) =
        File(secureDir(context), "keystore.json")

    // ================================================
    // ЗОВНІШНІ (доступні користувачу)
    // /storage/emulated/0/NoxisOS/
    // ================================================

    val externalRoot: File
        get() = File(Environment.getExternalStorageDirectory(), "NoxisOS")

    // Встановлені застосунки
    val appsDir: File
        get() = File(externalRoot, "apps")

    // Файли користувача (аналог /home)
    val homeDir: File
        get() = File(externalRoot, "home")

    // Тимчасові файли
    val tmpDir: File
        get() = File(externalRoot, "tmp")

    // Медіа
    val imagesDir: File
        get() = File(externalRoot, "media/images")

    val downloadsDir: File
        get() = File(externalRoot, "media/downloads")

    val documentsDir: File
        get() = File(externalRoot, "media/documents")

    // Папка конкретного застосунку
    fun appDir(appId: String): File =
        File(appsDir, appId)

    // Дані застосунку (пісочниця)
    fun appDataDir(context: Context, appId: String): File =
        File(File(internalRoot(context), "appdata"), appId).also { it.mkdirs() }

    fun initExternalDirs() {
        listOf(appsDir, homeDir, tmpDir, imagesDir, downloadsDir, documentsDir)
            .forEach { it.mkdirs() }
    }
}
