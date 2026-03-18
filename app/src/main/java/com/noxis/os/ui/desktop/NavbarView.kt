package com.noxis.os.ui.desktop

import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import com.noxis.os.system.SettingsManager
import com.noxis.os.util.dpToPx

class NavbarView(context: Context) : View(context) {

    var onBack: (() -> Unit)? = null
    var onHome: (() -> Unit)? = null
    var onRecent: (() -> Unit)? = null

    private val settings = SettingsManager.get(context)
    private var pressedBtn = -1

    private val bgPaint = Paint().apply { color = Color.parseColor("#FF1C1C1E") }
    private val linePaint = Paint().apply {
        color = Color.parseColor("#33FFFFFF")
        strokeWidth = 0.5f
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 0f
        strokeCap = Paint.Cap.ROUND
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.FILL
    }

    // □ ○ >   (Recent, Home, Back) — Samsung порядок
    private val btnCount = 3

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawLine(0f, 0f, w, 0f, linePaint)

        val positions = getBtnPositions(w)
        val iconSize = context.dpToPx(22).toFloat()

        positions.forEachIndexed { i, cx ->
            val cy = h / 2f

            if (pressedBtn == i) {
                canvas.drawCircle(cx, cy, context.dpToPx(24).toFloat(), ripplePaint)
            }

            iconPaint.strokeWidth = context.dpToPx(2).toFloat()
            iconPaint.color = Color.WHITE
            iconPaint.style = Paint.Style.STROKE

            when (i) {
                0 -> drawRecent(canvas, cx, cy, iconSize)   // □
                1 -> drawHome(canvas, cx, cy, iconSize)     // ○
                2 -> drawBack(canvas, cx, cy, iconSize)     // >
            }
        }
    }

    // □ — Останні (заокруглений квадрат)
    private fun drawRecent(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.45f
        val r = size * 0.12f
        val rect = RectF(cx - s, cy - s, cx + s, cy + s)
        canvas.drawRoundRect(rect, r, r, iconPaint)
    }

    // ○ — Додому (коло, трохи більше)
    private fun drawHome(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        canvas.drawCircle(cx, cy, size * 0.5f, iconPaint)
    }

    // > — Назад (стрілка вправо як у Samsung)
    private fun drawBack(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.4f
        val path = Path().apply {
            moveTo(cx - s * 0.3f, cy - s * 0.8f)
            lineTo(cx + s * 0.5f, cy)
            lineTo(cx - s * 0.3f, cy + s * 0.8f)
        }
        canvas.drawPath(path, iconPaint)
    }

    private fun getBtnPositions(w: Float): List<Float> {
        return listOf(w * 0.2f, w * 0.5f, w * 0.8f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val positions = getBtnPositions(width.toFloat())
        val hitZone = context.dpToPx(40).toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedBtn = positions.indexOfFirst { kotlin.math.abs(event.x - it) < hitZone }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val btn = pressedBtn
                pressedBtn = -1
                invalidate()
                if (btn >= 0 && kotlin.math.abs(event.x - positions[btn]) < hitZone) {
                    haptic()
                    when (btn) {
                        0 -> onRecent?.invoke()
                        1 -> onHome?.invoke()
                        2 -> onBack?.invoke()
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> { pressedBtn = -1; invalidate() }
        }
        return false
    }

    private fun haptic() {
        if (!settings.navbarHaptic) return
        try {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {}
    }
}
