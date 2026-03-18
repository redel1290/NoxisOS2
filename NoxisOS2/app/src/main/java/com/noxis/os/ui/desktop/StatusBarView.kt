package com.noxis.os.ui.desktop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.noxis.os.system.SettingsManager
import com.noxis.os.util.dpToPx
import java.text.SimpleDateFormat
import java.util.*

class StatusBarView(context: Context) : FrameLayout(context) {

    private val settings = SettingsManager.get(context)
    private val clockFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())

    private val clockView: TextView
    private val batteryView: TextView

    private val clockRunnable = object : Runnable {
        override fun run() {
            clockView.text = clockFmt.format(Date())
            handler.postDelayed(this, 30_000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                val pct = (level * 100 / scale)
                batteryView.text = "$pct%"
                batteryView.setTextColor(when {
                    pct <= 15 -> Color.parseColor("#FF5F57")
                    pct <= 30 -> Color.parseColor("#FFBD2E")
                    else -> Color.parseColor("#8A8A9A")
                })
            }
        }
    }

    init {
        setBackgroundColor(Color.parseColor("#CC0D0D0F"))

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpToPx(12), 0, context.dpToPx(12), 0)
        }

        // Ліво — порожньо (місце для сповіщень у майбутньому)
        val spacer = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Центр — годинник
        clockView = TextView(context).apply {
            setTextColor(Color.parseColor("#F0F0F5"))
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Право — батарея
        batteryView = TextView(context).apply {
            setTextColor(Color.parseColor("#8A8A9A"))
            textSize = 11f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        if (settings.statusbarClock) row.addView(spacer)
        if (settings.statusbarClock) row.addView(clockView)
        if (settings.statusbarBattery) row.addView(batteryView)

        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        clockView.text = clockFmt.format(Date())
        handler.post(clockRunnable)

        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(clockRunnable)
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) { }
    }
}
