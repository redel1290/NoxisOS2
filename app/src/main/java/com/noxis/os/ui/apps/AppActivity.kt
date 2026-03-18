package com.noxis.os.ui.apps

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.noxis.os.system.SettingsManager
import com.noxis.os.system.lki.AppRegistry
import com.noxis.os.system.lua.LuaRuntime
import com.noxis.os.system.lua.LuaWindowApi
import com.noxis.os.ui.desktop.NavbarView
import com.noxis.os.ui.desktop.StatusBarView
import com.noxis.os.util.dpToPx

class AppActivity : AppCompatActivity() {

    private lateinit var contentFrame: FrameLayout
    private var luaRuntime: LuaRuntime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appId = intent.getStringExtra("app_id") ?: run { finish(); return }
        val appName = intent.getStringExtra("app_name") ?: appId
        val settings = SettingsManager.get(this)

        val app = AppRegistry.getInstalledApps(this).find { it.id == appId }
            ?: run { finish(); return }

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0D0D0F"))

        val statusBar = StatusBarView(this)
        val STATUSBAR_H = dpToPx(24)
        val NAVBAR_H = dpToPx(52)

        contentFrame = FrameLayout(this)

        val navbar = NavbarView(this).apply {
            onBack = { finish() }
            onHome = {
                finish()
                // Повернутись на робочий стіл
            }
        }

        root.addView(contentFrame, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).also {
            it.topMargin = if (settings.statusbarVisible) STATUSBAR_H else 0
            it.bottomMargin = if (settings.navbarVisible) NAVBAR_H else 0
        })

        if (settings.statusbarVisible) {
            root.addView(statusBar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, STATUSBAR_H,
                android.view.Gravity.TOP
            ))
        }
        if (settings.navbarVisible) {
            root.addView(navbar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, NAVBAR_H,
                android.view.Gravity.BOTTOM
            ))
        }

        setContentView(root)

        // Запускаємо Lua
        luaRuntime = LuaRuntime(this)
        luaRuntime?.launch(app, object : LuaWindowApi {
            override fun onSetTitle(title: String) {
                runOnUiThread { supportActionBar?.title = title }
            }
            override fun onSetContent(content: String) {
                runOnUiThread {
                    contentFrame.removeAllViews()
                    contentFrame.addView(TextView(this@AppActivity).apply {
                        text = content
                        setTextColor(Color.parseColor("#F0F0F5"))
                        setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                    })
                }
            }
            override fun onClose() = finish()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        luaRuntime = null
    }

    override fun finish() {
        super.finish()
        if (SettingsManager.get(this).animations) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
