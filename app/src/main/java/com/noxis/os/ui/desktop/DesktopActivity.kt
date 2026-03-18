package com.noxis.os.ui.desktop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.noxis.os.system.KernelManager
import com.noxis.os.system.SettingsManager
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.system.lki.AppRegistry
import com.noxis.os.ui.apps.*
import com.noxis.os.ui.settings.SettingsActivity
import com.noxis.os.util.SystemPaths
import com.noxis.os.util.dpToPx

class DesktopActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var desktopView: DesktopView
    private lateinit var statusBar: StatusBarView
    private lateinit var navbar: NavbarView

    private val STATUSBAR_H get() = dpToPx(28)
    private val NAVBAR_H get() = dpToPx(56)
    private val REQ_PERM = 100
    private val REQ_STORAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KernelManager.get(this)
        setupEdgeToEdge()
        buildUI()
        requestPermissions()
    }

    private fun setupEdgeToEdge() {
        // Ховаємо системний навбар і статус бар Android повністю
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun buildUI() {
        val settings = SettingsManager.get(this)

        root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0D0D0F"))

        desktopView = DesktopView(this)
        desktopView.onAppClick = { app -> openApp(app) }

        statusBar = StatusBarView(this)
        navbar = NavbarView(this).apply {
            onBack = { /* на десктопі нічого */ }
            onHome = { /* вже тут */ }
            onRecent = { }
        }

        // Десктоп займає весь екран
        root.addView(desktopView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).also {
            it.topMargin = STATUSBAR_H
            it.bottomMargin = NAVBAR_H
        })

        // Статус бар зверху
        root.addView(statusBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, STATUSBAR_H,
            android.view.Gravity.TOP
        ))

        // Навбар знизу
        root.addView(navbar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, NAVBAR_H,
            android.view.Gravity.BOTTOM
        ))

        setContentView(root)
    }

    private fun rebuildUI() {
        root.removeAllViews()
        buildUI()
        loadApps()
    }

    private fun openApp(app: AppInfo) {
        val intent = when (app.id) {
            "com.noxis.settings" -> Intent(this, SettingsActivity::class.java)
            "com.noxis.files"    -> Intent(this, FileManagerActivity::class.java)
            "com.noxis.notes"    -> Intent(this, NotesActivity::class.java)
            "com.noxis.browser"  -> Intent(this, BrowserActivity::class.java)
            "com.noxis.terminal" -> Intent(this, TerminalActivity::class.java)
            else -> Intent(this, AppActivity::class.java).apply {
                putExtra("app_id", app.id)
                putExtra("app_name", app.name)
            }
        }
        startActivity(intent)
        if (SettingsManager.get(this).animations) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loadApps() {
        SystemPaths.initExternalDirs()
        desktopView.reload(AppRegistry.getAll(this))
    }

    override fun onResume() {
        super.onResume()
        // Повторно ховаємо системний UI (може відновитися після свайпу)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        // Повне перебудування щоб налаштування вигляду застосувались
        rebuildUI()
    }

    // --- Дозволи ---
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")), REQ_STORAGE
                )
            } else onPermissionsGranted()
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val denied = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (denied.isNotEmpty()) ActivityCompat.requestPermissions(this, denied.toTypedArray(), REQ_PERM)
            else onPermissionsGranted()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQ_PERM) onPermissionsGranted()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_STORAGE) onPermissionsGranted()
    }

    private fun onPermissionsGranted() {
        SystemPaths.initExternalDirs()
        loadApps()
    }

    override fun onBackPressed() {
        if (::desktopView.isInitialized && desktopView.isDrawerOpen()) {
            desktopView.closeDrawer()
        }
    }
}
