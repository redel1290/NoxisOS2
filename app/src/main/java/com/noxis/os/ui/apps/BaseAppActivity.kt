package com.noxis.os.ui.apps

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.noxis.os.system.SettingsManager
import com.noxis.os.ui.desktop.NavbarView
import com.noxis.os.ui.desktop.StatusBarView
import com.noxis.os.util.dpToPx

abstract class BaseAppActivity : AppCompatActivity() {

    protected lateinit var contentRoot: FrameLayout
    private lateinit var titleTextView: TextView

    protected abstract val appTitle: String
    protected open val showTitle: Boolean = true

    private val STATUSBAR_H get() = dpToPx(28)
    private val NAVBAR_H get() = dpToPx(56)
    private val TITLEBAR_H get() = if (showTitle) dpToPx(52) else 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val settings = SettingsManager.get(this)
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0D0D0F"))

        // Тайтлбар
        val titleBar = if (showTitle) {
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#12121A"))
                setPadding(dpToPx(16), 0, dpToPx(16), 0)

                titleTextView = TextView(this@BaseAppActivity).apply {
                    text = appTitle
                    textSize = 17f
                    setTextColor(Color.parseColor("#F0F0F5"))
                    letterSpacing = 0.02f
                }
                addView(titleTextView)
            }
        } else null

        contentRoot = FrameLayout(this)

        val topOffset = STATUSBAR_H + TITLEBAR_H
        root.addView(contentRoot, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).also { it.topMargin = topOffset; it.bottomMargin = NAVBAR_H })

        root.addView(StatusBarView(this), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, STATUSBAR_H, Gravity.TOP
        ))

        titleBar?.let {
            root.addView(it, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, TITLEBAR_H, Gravity.TOP
            ).also { lp -> lp.topMargin = STATUSBAR_H })
        }

        root.addView(NavbarView(this).apply {
            onBack = { onBackPressed() }
            onHome = {
                finishAffinity()
                startActivity(packageManager.getLaunchIntentForPackage(packageName))
            }
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, NAVBAR_H, Gravity.BOTTOM
        ))

        setContentView(root)
        onContentReady()
    }

    protected abstract fun onContentReady()

    protected fun updateTitle(title: String) {
        if (::titleTextView.isInitialized) titleTextView.text = title
    }

    override fun onResume() {
        super.onResume()
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun finish() {
        super.finish()
        if (SettingsManager.get(this).animations)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
