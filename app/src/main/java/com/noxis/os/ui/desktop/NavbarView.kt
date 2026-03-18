package com.noxis.os.ui.desktop

import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import com.noxis.os.system.SettingsManager
import com.noxis.os.util.dpToPx

class NavbarView(context: Context) : View(context) {

    var onBack: (() -> Unit)? = null
    var onHome: (() -> Unit)? = null
    var onRecent: (() -> Unit)? = null

    private val settings = SettingsManager.get(context)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFFFFF")
        style = Paint.Style.FILL
    }
    private val btnPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA7B5EA7")
        style = Paint.Style.FILL
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        
    }

    private var pressedBtn = -1  // 0=back, 1=home, 2=recent

    init {
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Фон з blur ефектом (імітація)
        bgPaint.color = Color.parseColor("#E80F0F12")
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Тонка лінія зверху
        val linePaint = Paint().apply {
            color = Color.parseColor("#222230")
            strokeWidth = 1f
        }
        canvas.drawLine(0f, 0f, w, 0f, linePaint)

        val btnSize = context.dpToPx(36).toFloat()
        val btnR = btnSize / 2f
        val cy = h / 2f
        val positions = listOf(w * 0.2f, w * 0.5f, w * 0.8f)

        positions.forEachIndexed { i, cx ->
            val paint = if (pressedBtn == i) btnPressedPaint else btnPaint
            // Кнопка — напівпрозоре коло
            canvas.drawCircle(cx, cy, btnR, paint)
            // Іконка
            drawNavIcon(canvas, i, cx, cy, btnR * 0.5f)
        }
    }

    private fun drawNavIcon(canvas: Canvas, type: Int, cx: Float, cy: Float, size: Float) {
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = context.dpToPx(2).toFloat()

        when (type) {
            0 -> { // Назад — стрілка вліво
                val path = Path().apply {
                    moveTo(cx + size * 0.4f, cy - size * 0.7f)
                    lineTo(cx - size * 0.4f, cy)
                    lineTo(cx + size * 0.4f, cy + size * 0.7f)
                }
                canvas.drawPath(path, iconPaint)
            }
            1 -> { // Додому — коло
                iconPaint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, size * 0.6f, iconPaint)
                iconPaint.color = Color.parseColor("#0F0F12")
                canvas.drawCircle(cx, cy, size * 0.3f, iconPaint)
                iconPaint.color = Color.parseColor("#CCFFFFFF")
            }
            2 -> { // Останні — квадрат
                iconPaint.style = Paint.Style.STROKE
                val rect = RectF(cx - size * 0.55f, cy - size * 0.55f,
                                 cx + size * 0.55f, cy + size * 0.55f)
                canvas.drawRoundRect(rect, size * 0.2f, size * 0.2f, iconPaint)
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        val w = width.toFloat()
        val positions = listOf(w * 0.2f, w * 0.5f, w * 0.8f)
        val btnSize = context.dpToPx(36).toFloat()

        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                pressedBtn = positions.indexOfFirst {
                    Math.abs(event.x - it) < btnSize
                }
                invalidate()
                return true
            }
            android.view.MotionEvent.ACTION_UP -> {
                val btn = pressedBtn
                pressedBtn = -1
                invalidate()
                if (btn >= 0 && Math.abs(event.x - positions[btn]) < btnSize) {
                    triggerHaptic()
                    when (btn) {
                        0 -> onBack?.invoke()
                        1 -> onHome?.invoke()
                        2 -> onRecent?.invoke()
                    }
                }
                return true
            }
            android.view.MotionEvent.ACTION_CANCEL -> {
                pressedBtn = -1; invalidate()
            }
        }
        return false
    }

    private fun triggerHaptic() {
        if (!settings.navbarHaptic) return
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {}
    }
}
