package com.noxis.os.system.lki

import android.graphics.Bitmap
import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.noxis.os.util.SystemPaths
import java.io.File

data class AppInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val icon: Bitmap?,
    val installDir: String,
    val mainScript: String,
    val isSystem: Boolean = false,
    var gridCol: Int = 0,
    var gridRow: Int = 0
)

data class LkiManifest(
    @SerializedName("id")          val id: String = "",
    @SerializedName("name")        val name: String = "",
    @SerializedName("version")     val version: String = "1.0.0",
    @SerializedName("author")      val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("main")        val main: String = "main.lua",
    @SerializedName("icon")        val icon: String = "icon.png",
    @SerializedName("permissions") val permissions: List<String> = emptyList()
) {
    fun isValid() = id.isNotBlank() && name.isNotBlank()
}

/**
 * Реєстр усіх застосунків (системних + встановлених)
 */
object AppRegistry {

    private val gson = Gson()

    fun getAll(context: Context): List<AppInfo> {
        val list = mutableListOf<AppInfo>()
        list.addAll(getSystemApps())
        list.addAll(getInstalledApps(context))
        return list
    }

    /**
     * Вбудовані системні застосунки
     * installDir = "" — їх Activity відкривається напряму
     */
    private fun getSystemApps(): List<AppInfo> = listOf(
        AppInfo(
            id = "com.noxis.files",
            name = "Файли",
            version = "1.0",
            author = "Noxis",
            description = "Файловий менеджер",
            icon = null,
            installDir = "",
            mainScript = "",
            isSystem = true,
            gridCol = 0, gridRow = 0
        ),
        AppInfo(
            id = "com.noxis.notes",
            name = "Нотатки",
            version = "1.0",
            author = "Noxis",
            description = "Текстовий редактор",
            icon = null,
            installDir = "",
            mainScript = "",
            isSystem = true,
            gridCol = 1, gridRow = 0
        ),
        AppInfo(
            id = "com.noxis.browser",
            name = "Браузер",
            version = "1.0",
            author = "Noxis",
            description = "Веб-браузер",
            icon = null,
            installDir = "",
            mainScript = "",
            isSystem = true,
            gridCol = 2, gridRow = 0
        ),
        AppInfo(
            id = "com.noxis.terminal",
            name = "Термінал",
            version = "1.0",
            author = "Noxis",
            description = "Lua термінал",
            icon = null,
            installDir = "",
            mainScript = "",
            isSystem = true,
            gridCol = 3, gridRow = 0
        ),
        AppInfo(
            id = "com.noxis.settings",
            name = "Налаштування",
            version = "1.0",
            author = "Noxis",
            description = "Системні налаштування",
            icon = null,
            installDir = "",
            mainScript = "",
            isSystem = true,
            gridCol = 0, gridRow = 1
        )
    )

    fun getInstalledApps(context: Context): List<AppInfo> {
        val appsDir = SystemPaths.appsDir
        if (!appsDir.exists()) return emptyList()

        return appsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                try {
                    val meta = gson.fromJson(
                        File(dir, "meta.json").readText(), AppMeta::class.java
                    )
                    val icon = File(dir, "icon.png").let {
                        if (it.exists()) android.graphics.BitmapFactory.decodeFile(it.absolutePath)
                        else null
                    }
                    AppInfo(
                        id = meta.id, name = meta.name,
                        version = meta.version, author = meta.author,
                        description = meta.description, icon = icon,
                        installDir = dir.absolutePath, mainScript = meta.mainScript,
                        gridCol = meta.gridCol, gridRow = meta.gridRow
                    )
                } catch (e: Exception) { null }
            } ?: emptyList()
    }

    data class AppMeta(
        val id: String = "", val name: String = "",
        val version: String = "", val author: String = "",
        val description: String = "", val mainScript: String = "",
        val gridCol: Int = 0, val gridRow: Int = 0
    )
}
