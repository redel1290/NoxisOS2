package com.noxis.os.system

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.noxis.os.util.SystemPaths

data class NoxisSettings(
    // --- Вигляд ---
    @SerializedName("accent_color")       val accentColor: String = "#7B5EA7",
    @SerializedName("wallpaper")          val wallpaper: String = "default",
    @SerializedName("dark_mode")          val darkMode: Boolean = true,
    @SerializedName("icon_size")          val iconSize: Int = 60,          // dp
    @SerializedName("icon_label_visible") val iconLabelVisible: Boolean = true,
    @SerializedName("grid_columns")       val gridColumns: Int = 4,

    // --- Навбар ---
    @SerializedName("navbar_visible")     val navbarVisible: Boolean = true,
    @SerializedName("navbar_haptic")      val navbarHaptic: Boolean = true,

    // --- Статус бар ---
    @SerializedName("statusbar_visible")  val statusbarVisible: Boolean = true,
    @SerializedName("statusbar_clock")    val statusbarClock: Boolean = true,
    @SerializedName("statusbar_battery")  val statusbarBattery: Boolean = true,

    // --- Анімації ---
    @SerializedName("animations")         val animations: Boolean = true,
    @SerializedName("transition_speed")   val transitionSpeed: Float = 1.0f,  // 0.5 = швидко, 2.0 = повільно

    // --- Застосунки ---
    @SerializedName("recent_apps_count")  val recentAppsCount: Int = 10,

    // --- Мова ---
    @SerializedName("language")           val language: String = "uk",

    // --- Мережа / OTA ---
    @SerializedName("update_channel")     val updateChannel: String = "stable",  // stable / beta
    @SerializedName("server_url")         val serverUrl: String = "",
    @SerializedName("server_enabled")     val serverEnabled: Boolean = false,

    // --- Активна прошивка ---
    @SerializedName("active_firmware")    val activeFirmware: String = "com.noxis.stock",
    @SerializedName("firmware_version")   val firmwareVersion: String = "1.0.0"
)

object SettingsManager {

    private val gson = Gson()
    private var cached: NoxisSettings? = null

    fun get(context: Context): NoxisSettings {
        if (cached != null) return cached!!
        val file = SystemPaths.settingsFile(context)
        cached = if (file.exists()) {
            try { gson.fromJson(file.readText(), NoxisSettings::class.java) }
            catch (e: Exception) { NoxisSettings() }
        } else {
            NoxisSettings()
        }
        return cached!!
    }

    fun save(context: Context, settings: NoxisSettings) {
        cached = settings
        SystemPaths.settingsFile(context).writeText(gson.toJson(settings))
    }

    fun update(context: Context, block: NoxisSettings.() -> NoxisSettings) {
        save(context, get(context).block())
    }

    fun invalidate() { cached = null }
}
