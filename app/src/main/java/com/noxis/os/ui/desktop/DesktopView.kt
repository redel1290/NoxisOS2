package com.noxis.os.ui.desktop

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.noxis.os.system.NoxisSettings
import com.noxis.os.system.SettingsManager
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.util.dpToPx
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class DesktopView(context: Context) : View(context) {

    private var settings: NoxisSettings = SettingsManager.get(context)
    private val apps = mutableListOf<DesktopIcon>()

    private val iconSizePx get() = context.dpToPx(settings.iconSize).toFloat()
    private val cellPaddingPx get() = context.dpToPx(16).toFloat()
    private val columns get() = settings.gridColumns

    // Long press для drag
    private val LONG_PRESS_MS = 400L
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    // Drag стан
    private var dragging: DesktopIcon? = null
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var isDragging = false
    private var dragScale = 1f  // анімація підйому

    var onAppClick: ((AppInfo) -> Unit)? = null

    // Іконки — унікальні кольори для кожного застосунку
    private val appColors = mapOf(
        "com.noxis.files"    to intArrayOf(0xFF1E88E5.toInt(), 0xFF42A5F5.toInt()),
        "com.noxis.notes"    to intArrayOf(0xFFF9A825.toInt(), 0xFFFFD54F.toInt()),
        "com.noxis.browser"  to intArrayOf(0xFF43A047.toInt(), 0xFF66BB6A.toInt()),
        "com.noxis.terminal" to intArrayOf(0xFF212121.toInt(), 0xFF424242.toInt()),
        "com.noxis.settings" to intArrayOf(0xFF546E7A.toInt(), 0xFF78909C.toInt())
    )
    private val appSymbols = mapOf(
        "com.noxis.files"    to "📁",
        "com.noxis.notes"    to "📝",
        "com.noxis.browser"  to "🌐",
        "com.noxis.terminal" to ">_",
        "com.noxis.settings" to "⚙"
    )

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60000000")
        maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 1f, Color.parseColor("#AA000000"))
    }
    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val dragBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA7B5EA7")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun reload(appList: List<AppInfo>) {
        apps.clear()
        appList.forEachIndexed { i, app ->
            val col = i % columns
            val row = i / columns
            val (x, y) = cellToPixel(col, row)
            apps.add(DesktopIcon(app, col, row, x, y))
        }
        invalidate()
    }

    fun reloadSettings() {
        settings = SettingsManager.get(context)
        // Перерахувати позиції
        apps.forEachIndexed { i, icon ->
            icon.col = i % columns
            icon.row = i / columns
            val (x, y) = cellToPixel(icon.col, icon.row)
            icon.drawX = x
            icon.drawY = y
        }
        invalidate()
    }

    private fun cellToPixel(col: Int, row: Int): Pair<Float, Float> {
        if (width == 0) return 0f to 0f
        val cellW = width.toFloat() / columns
        val x = col * cellW + (cellW - iconSizePx) / 2f
        val y = row * (iconSizePx + cellPaddingPx + context.dpToPx(18)) + cellPaddingPx
        return x to y
    }

    private fun pixelToCell(x: Float, y: Float): Pair<Int, Int> {
        if (width == 0) return 0 to 0
        val cellW = width.toFloat() / columns
        val cellH = iconSizePx + cellPaddingPx + context.dpToPx(18)
        val col = (x / cellW).toInt().coerceIn(0, columns - 1)
        val row = (y / cellH).toInt().coerceAtLeast(0)
        return col to row
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Перерахувати позиції після зміни розміру
        apps.forEachIndexed { i, icon ->
            icon.col = i % columns
            icon.row = i / columns
            val (x, y) = cellToPixel(icon.col, icon.row)
            icon.drawX = x
            icon.drawY = y
        }
    }

    override fun onDraw(canvas: Canvas) {
        apps.forEach { icon ->
            if (icon == dragging && isDragging) return@forEach
            drawIcon(canvas, icon, icon.drawX, icon.drawY, 1f)
        }

        // Drag: snap preview
        if (isDragging && dragging != null) {
            val (sc, sr) = pixelToCell(dragCurrentX, dragCurrentY)
            val (sx, sy) = cellToPixel(sc, sr)
            val size = iconSizePx
            val r = size * 0.22f
            val rect = RectF(sx, sy, sx + size, sy + size)
            canvas.drawRoundRect(rect, r, r, dragBorderPaint)

            // Іконка слідує за пальцем з підйомом
            val drawX = dragCurrentX - dragOffsetX
            val drawY = dragCurrentY - dragOffsetY
            drawIcon(canvas, dragging!!, drawX, drawY, dragScale)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: DesktopIcon, x: Float, y: Float, scale: Float) {
        val size = iconSizePx
        val cx = x + size / 2f
        val cy = y + size / 2f
        val scaledSize = size * scale
        val left = cx - scaledSize / 2f
        val top = cy - scaledSize / 2f
        val right = cx + scaledSize / 2f
        val bottom = cy + scaledSize / 2f
        val rect = RectF(left, top, right, bottom)
        val r = scaledSize * 0.22f

        // Кастомна іконка
        if (icon.app.icon != null) {
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(icon.app.icon, null, rect, bmpPaint)
        } else {
            drawGeneratedIcon(canvas, icon, rect, r, scale)
        }

        // Назва
        if (settings.iconLabelVisible) {
            labelPaint.textSize = context.dpToPx(10).toFloat()
            canvas.drawText(
                icon.app.name,
                cx,
                bottom + context.dpToPx(14),
                labelPaint
            )
        }
    }

    private fun drawGeneratedIcon(canvas: Canvas, icon: DesktopIcon, rect: RectF, r: Float, scale: Float) {
        val colors = appColors[icon.app.id]
            ?: generateColors(icon.app.id)

        // Тінь (тільки без drag scale щоб не виглядало дивно)
        if (scale == 1f) {
            val shadowRect = RectF(rect.left + 3, rect.top + 6, rect.right + 3, rect.bottom + 6)
            canvas.drawRoundRect(shadowRect, r, r, shadowPaint)
        }

        // Градієнтний фон
        val gradient = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            colors[0], colors[1], Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, r, r, bgPaint)

        // Легкий блик зверху
        val gloss = LinearGradient(
            rect.left, rect.top, rect.left, rect.top + rect.height() * 0.5f,
            Color.argb(60, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gloss
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, r, r, glossPaint)

        // Символ
        val symbol = appSymbols[icon.app.id] ?: icon.app.name.take(2)
        symbolPaint.textSize = rect.width() * 0.38f
        canvas.drawText(
            symbol,
            rect.centerX(),
            rect.centerY() + symbolPaint.textSize * 0.35f,
            symbolPaint
        )
    }

    private fun generateColors(appId: String): IntArray {
        // Детермінований колір з id
        val hash = appId.hashCode()
        val hue = (Math.abs(hash) % 360).toFloat()
        val color1 = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f))
        val color2 = Color.HSVToColor(floatArrayOf((hue + 30) % 360, 0.5f, 0.95f))
        return intArrayOf(color1, color2)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val icon = findIconAt(event.x, event.y)
                if (icon != null) {
                    // Запускаємо long press таймер
                    longPressRunnable = Runnable {
                        startDrag(icon, event.x, event.y)
                    }
                    handler.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && dragging != null) {
                    dragCurrentX = event.x
                    dragCurrentY = event.y
                    invalidate()
                } else {
                    // Якщо рухнули до long press — скасовуємо
                    val dx = abs(event.x - (dragging?.drawX ?: event.x))
                    val dy = abs(event.y - (dragging?.drawY ?: event.y))
                    if (hypot(dx, dy) > context.dpToPx(8)) {
                        cancelDragLongPress()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging && dragging != null) {
                    dropIcon(dragging!!, dragCurrentX, dragCurrentY)
                } else {
                    cancelDragLongPress()
                    // Звичайний тап
                    val icon = findIconAt(event.x, event.y)
                    icon?.let { onAppClick?.invoke(it.app) }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelDragLongPress()
                if (isDragging) dropIcon(dragging!!, dragging!!.drawX, dragging!!.drawY)
                return true
            }
        }
        return false
    }

    private fun startDrag(icon: DesktopIcon, x: Float, y: Float) {
        isDragging = true
        dragging = icon
        dragCurrentX = x
        dragCurrentY = y
        dragOffsetX = x - icon.drawX
        dragOffsetY = y - icon.drawY

        // Вібрація
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {}

        // Анімація підйому (scale 1.0 → 1.15)
        ValueAnimator.ofFloat(1f, 1.15f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { dragScale = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun dropIcon(icon: DesktopIcon, x: Float, y: Float) {
        val (newCol, newRow) = pixelToCell(x, y)

        // Swap якщо зайнято
        val target = apps.find { it != icon && it.col == newCol && it.row == newRow }
        if (target != null) {
            target.col = icon.col; target.row = icon.row
            val (ox, oy) = cellToPixel(icon.col, icon.row)
            target.drawX = ox; target.drawY = oy
        }

        icon.col = newCol; icon.row = newRow
        val (nx, ny) = cellToPixel(newCol, newRow)

        // Анімація посадки
        ValueAnimator.ofFloat(dragScale, 1f).apply {
            duration = 120
            addUpdateListener {
                dragScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Анімація до snap позиції
        val startX = dragCurrentX - dragOffsetX
        val startY = dragCurrentY - dragOffsetY
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                icon.drawX = startX + (nx - startX) * t
                icon.drawY = startY + (ny - startY) * t
                invalidate()
            }
            doOnEnd {
                icon.drawX = nx; icon.drawY = ny
                isDragging = false; dragging = null; dragScale = 1f
                invalidate()
            }
            start()
        }
    }

    private fun cancelDragLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun findIconAt(x: Float, y: Float): DesktopIcon? {
        val size = iconSizePx
        return apps.firstOrNull { icon ->
            x >= icon.drawX && x <= icon.drawX + size &&
            y >= icon.drawY && y <= icon.drawY + size
        }
    }

    data class DesktopIcon(
        val app: AppInfo,
        var col: Int, var row: Int,
        var drawX: Float, var drawY: Float
    )
}

// Extension для ValueAnimator
private fun ValueAnimator.doOnEnd(action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = action()
    })
}
