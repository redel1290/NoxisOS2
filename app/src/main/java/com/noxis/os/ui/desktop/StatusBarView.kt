package com.noxis.os.ui.desktop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
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
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())

    private var timeStr = timeFmt.format(Date())
    private var batteryPct = 100
    private var charging = false

    private val bgPaint = Paint().apply { color = Color.parseColor("#FF1C1C1E") }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
    }
    private val batteryBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 1.5f
    }
    private val batteryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val batteryCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            timeStr = timeFmt.format(Date())
            invalidate()
            handler.postDelayed(this, 30_000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val lvl = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scl = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (lvl >= 0 && scl > 0) batteryPct = lvl * 100 / scl
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL
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

        val pad = context.dpToPx(14).toFloat()
        val cy = h / 2f

        // Час — зліва (Samsung стиль)
        timePaint.textSize = context.dpToPx(12).toFloat()
        canvas.drawText(timeStr, pad, cy + timePaint.textSize * 0.35f, timePaint)

        // Права сторона — відсоток + іконка батареї
        val rightEdge = w - pad
        drawBatteryIcon(canvas, rightEdge, cy)

        // Відсоток батареї перед іконкою
        pctPaint.textSize = context.dpToPx(11).toFloat()
        val iconW = context.dpToPx(24).toFloat()
        canvas.drawText("$batteryPct%", rightEdge - iconW - context.dpToPx(4), cy + pctPaint.textSize * 0.35f, pctPaint)
    }

    private fun drawBatteryIcon(canvas: Canvas, rightX: Float, cy: Float) {
        val bw = context.dpToPx(22).toFloat()
        val bh = context.dpToPx(11).toFloat()
        val capW = context.dpToPx(2).toFloat()
        val capH = context.dpToPx(5).toFloat()
        val r = context.dpToPx(2).toFloat()

        val left = rightX - bw
        val top = cy - bh / 2f

        // Тіло батареї
        batteryBodyPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, top, left + bw - capW, top + bh), r, r, batteryBodyPaint)

        // Кінчик
        canvas.drawRoundRect(
            RectF(left + bw - capW + 1f, cy - capH / 2f, left + bw + 1f, cy + capH / 2f),
            1f, 1f, batteryCapPaint
        )

        // Заповнення
        val fillColor = when {
            charging -> Color.parseColor("#4CD964")
            batteryPct <= 15 -> Color.parseColor("#FF3B30")
            batteryPct <= 25 -> Color.parseColor("#FF9500")
            else -> Color.WHITE
        }
        batteryFillPaint.color = fillColor
        val fillW = (bw - capW - 3f) * (batteryPct / 100f)
        if (fillW > 0) {
            canvas.drawRoundRect(
                RectF(left + 1.5f, top + 1.5f, left + 1.5f + fillW, top + bh - 1.5f),
                r * 0.5f, r * 0.5f, batteryFillPaint
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(clockRunnable)
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }
}
