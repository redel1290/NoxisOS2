package com.noxis.os.ui.desktop

import android.content.Context
import android.graphics.Color
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.noxis.os.system.SettingsManager
import com.noxis.os.util.dpToPx

class NavbarView(context: Context) : FrameLayout(context) {

    var onBack: (() -> Unit)? = null
    var onHome: (() -> Unit)? = null
    var onRecent: (() -> Unit)? = null

    private val settings = SettingsManager.get(context)

    init {
        setBackgroundColor(Color.parseColor("#EE0F0F12"))

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnBack = navBtn("◀", "Назад") { triggerHaptic(); onBack?.invoke() }
        val btnHome = navBtn("⬤", "Додому") { triggerHaptic(); onHome?.invoke() }
        val btnRecent = navBtn("▣", "Останні") { triggerHaptic(); onRecent?.invoke() }

        row.addView(btnBack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        row.addView(btnHome, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        row.addView(btnRecent, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun navBtn(icon: String, label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = icon
            textSize = 18f
            setTextColor(Color.parseColor("#8A8A9A"))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener {
                setTextColor(Color.parseColor("#C8AAFF"))
                postDelayed({ setTextColor(Color.parseColor("#8A8A9A")) }, 150)
                onClick()
            }
        }
    }

    private fun triggerHaptic() {
        if (!settings.navbarHaptic) return
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) { }
    }
}
