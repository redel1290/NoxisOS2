package com.noxis.os.ui.desktop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.View
import com.noxis.os.system.SettingsManager
import com.noxis.os.util.dpToPx
import java.text.SimpleDateFormat
import java.util.*

class StatusBarView(context: Context) : View(context) {

    private val settings = SettingsManager.get(context)
    private val clockFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())

    private var timeStr = clockFmt.format(Date())
    private var batteryPct = 100
    private var batteryCharging = false

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC0D0D0F")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCF0F0F5")
        textAlign = Paint.Align.CENTER
    }
    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val batteryBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            timeStr = clockFmt.format(Date())
            invalidate()
            handler.postDelayed(this, 30_000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
            batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
            invalidate()
        }
    }

    init {
        handler.post(clockRunnable)
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val cy = h / 2f
        val textSize = context.dpToPx(11).toFloat()
        textPaint.textSize = textSize

        // Годинник — центр
        if (settings.statusbarClock) {
            canvas.drawText(timeStr, w / 2f, cy + textSize * 0.35f, textPaint)
        }

        // Батарея — права сторона
        if (settings.statusbarBattery) {
            drawBattery(canvas, w - context.dpToPx(32).toFloat(), cy)
        }
    }

    private fun drawBattery(canvas: Canvas, x: Float, cy: Float) {
        val bw = context.dpToPx(20).toFloat()
        val bh = context.dpToPx(10).toFloat()
        val left = x - bw / 2f
        val top = cy - bh / 2f
        val r = 2f

        // Обводка
        canvas.drawRoundRect(RectF(left, top, left + bw - 3f, top + bh), r, r, batteryBorderPaint)

        // Кінчик батареї
        batteryBorderPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(left + bw - 2f, cy - bh * 0.3f, left + bw, cy + bh * 0.3f), 1f, 1f, batteryBorderPaint)
        batteryBorderPaint.style = Paint.Style.STROKE

        // Заповнення
        val fillColor = when {
            batteryCharging -> Color.parseColor("#28C840")
            batteryPct <= 15 -> Color.parseColor("#FF5F57")
            batteryPct <= 30 -> Color.parseColor("#FFBD2E")
            else -> Color.parseColor("#AAFFFFFF")
        }
        batteryPaint.color = fillColor
        val fillW = (bw - 5f) * (batteryPct / 100f)
        if (fillW > 0) {
            canvas.drawRoundRect(RectF(left + 1.5f, top + 1.5f, left + 1.5f + fillW, top + bh - 1.5f), r * 0.5f, r * 0.5f, batteryPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(clockRunnable)
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }
}
