package com.noxis.os.ui.apps

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.noxis.os.system.SettingsManager
import com.noxis.os.ui.desktop.NavbarView
import com.noxis.os.ui.desktop.StatusBarView
import com.noxis.os.util.dpToPx

/**
 * Базова активність для всіх вбудованих застосунків.
 * Автоматично додає статус бар і навбар.
 * Підкласи розміщують свій UI в contentRoot.
 */
abstract class BaseAppActivity : AppCompatActivity() {

    protected lateinit var contentRoot: FrameLayout
    private lateinit var titleView: TextView

    protected abstract val appTitle: String
    protected open val showTitle: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsManager.get(this)
        val STATUSBAR_H = dpToPx(24)
        val NAVBAR_H = dpToPx(52)
        val TITLEBAR_H = if (showTitle) dpToPx(48) else 0

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0D0D0F"))

        // Тайтлбар застосунку
        val titleBar = if (showTitle) {
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#141417"))
                setPadding(dpToPx(16), 0, dpToPx(16), 0)

                titleView = TextView(this@BaseAppActivity).apply {
                    text = appTitle
                    textSize = 16f
                    setTextColor(Color.parseColor("#F0F0F5"))
                }
                addView(titleView)
            }
        } else null

        // Контент
        contentRoot = FrameLayout(this)

        // Статус бар
        val statusBar = StatusBarView(this)

        // Навбар
        val navbar = NavbarView(this).apply {
            onBack = { onBackPressed() }
            onHome = { navigateHome() }
        }

        // Компонування
        val topMargin = (if (settings.statusbarVisible) STATUSBAR_H else 0) + TITLEBAR_H
        val bottomMargin = if (settings.navbarVisible) NAVBAR_H else 0

        root.addView(contentRoot, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).also { it.topMargin = topMargin; it.bottomMargin = bottomMargin })

        if (settings.statusbarVisible) {
            root.addView(statusBar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, STATUSBAR_H, Gravity.TOP
            ))
        }

        titleBar?.let {
            root.addView(it, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, TITLEBAR_H, Gravity.TOP
            ).also { lp -> lp.topMargin = if (settings.statusbarVisible) STATUSBAR_H else 0 })
        }

        if (settings.navbarVisible) {
            root.addView(navbar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, NAVBAR_H, Gravity.BOTTOM
            ))
        }

        setContentView(root)
        onContentReady()
    }

    protected abstract fun onContentReady()

    protected fun setTitle(title: String) {
        if (::titleView.isInitialized) titleView.text = title
    }

    private fun navigateHome() {
        finishAffinity()
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
    }

    override fun finish() {
        super.finish()
        if (SettingsManager.get(this).animations) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
