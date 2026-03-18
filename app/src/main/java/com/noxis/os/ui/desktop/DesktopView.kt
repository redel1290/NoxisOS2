package com.noxis.os.ui.desktop

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.system.NoxisSettings
import com.noxis.os.system.SettingsManager
import com.noxis.os.util.dpToPx
import kotlin.math.abs
import kotlin.math.hypot

class DesktopView(context: Context) : View(context) {

    private var settings: NoxisSettings = SettingsManager.get(context)
    private val apps = mutableListOf<DesktopIcon>()

    private val iconSizePx get() = context.dpToPx(settings.iconSize)
    private val cellSizePx get() = context.dpToPx(settings.iconSize + 24)  // іконка + відступ
    private val labelHeight = context.dpToPx(14)
    private val columns get() = settings.gridColumns

    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = false
    }
    private val accentColor get() = Color.parseColor(settings.accentColor)

    // Drag стан
    private var dragging: DesktopIcon? = null
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var isDragging = false
    private val dragThreshold = context.dpToPx(10).toFloat()

    var onAppClick: ((AppInfo) -> Unit)? = null
    var onAppLongClick: ((AppInfo) -> Unit)? = null

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
        invalidate()
    }

    private fun cellToPixel(col: Int, row: Int): Pair<Float, Float> {
        val totalCellWidth = width.toFloat() / columns
        val x = col * totalCellWidth + (totalCellWidth - iconSizePx) / 2
        val y = row * cellSizePx.toFloat() + context.dpToPx(8)
        return x to y
    }

    private fun pixelToCell(x: Float, y: Float): Pair<Int, Int> {
        val totalCellWidth = width.toFloat() / columns
        val col = (x / totalCellWidth).toInt().coerceIn(0, columns - 1)
        val row = (y / cellSizePx).toInt().coerceAtLeast(0)
        return col to row
    }

    override fun onDraw(canvas: Canvas) {
        apps.forEach { icon ->
            if (icon == dragging && isDragging) return@forEach
            drawIcon(canvas, icon, icon.drawX, icon.drawY, 255)
        }
        // Drag preview
        if (isDragging && dragging != null) {
            val ox = dragCurrentX - (dragging!!.drawX + iconSizePx / 2)
            val oy = dragCurrentY - (dragging!!.drawY + iconSizePx / 2)
            drawIcon(canvas, dragging!!, dragging!!.drawX + ox, dragging!!.drawY + oy, 180)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: DesktopIcon, x: Float, y: Float, alpha: Int) {
        val size = iconSizePx.toFloat()
        val radius = size * 0.22f

        // Фон іконки
        iconBgPaint.apply {
            color = Color.argb((0xCC * alpha / 255), 30, 30, 45)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(x, y, x + size, y + size), radius, radius, iconBgPaint)

        // Зображення або літера
        icon.app.icon?.let { bmp ->
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alpha }
            canvas.drawBitmap(bmp, null, RectF(x + 6, y + 6, x + size - 6, y + size - 6), bmpPaint)
        } ?: run {
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, 200, 170, 255)
                textSize = size * 0.42f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText(
                icon.app.name.take(1).uppercase(),
                x + size / 2,
                y + size / 2 + letterPaint.textSize * 0.35f,
                letterPaint
            )
        }

        // Назва під іконкою
        if (settings.iconLabelVisible) {
            labelPaint.apply {
                this.alpha = alpha
                textSize = context.dpToPx(10).toFloat()
            }
            // Тінь для читабельності
            labelPaint.setShadowLayer(3f, 0f, 1f, Color.argb(180, 0, 0, 0))
            canvas.drawText(
                icon.app.name,
                x + size / 2,
                y + size + context.dpToPx(13),
                labelPaint
            )
            labelPaint.clearShadowLayer()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val icon = findIconAt(event.x, event.y)
                dragging = icon
                dragStartX = event.x
                dragStartY = event.y
                dragCurrentX = event.x
                dragCurrentY = event.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                dragCurrentX = event.x
                dragCurrentY = event.y
                if (!isDragging && dragging != null) {
                    val d = hypot(abs(event.x - dragStartX), abs(event.y - dragStartY))
                    if (d > dragThreshold) isDragging = true
                }
                if (isDragging) invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging && dragging != null) {
                    snapIcon(dragging!!, dragCurrentX, dragCurrentY)
                } else if (dragging != null) {
                    onAppClick?.invoke(dragging!!.app)
                }
                dragging = null
                isDragging = false
                invalidate()
                return true
            }
        }
        return false
    }

    private fun snapIcon(icon: DesktopIcon, x: Float, y: Float) {
        val (newCol, newRow) = pixelToCell(x, y)
        // Перевіряємо чи комірка зайнята
        val occupied = apps.find { it != icon && it.col == newCol && it.row == newRow }
        if (occupied != null) {
            // Міняємо місцями
            occupied.col = icon.col
            occupied.row = icon.row
            val (ox, oy) = cellToPixel(icon.col, icon.row)
            occupied.drawX = ox
            occupied.drawY = oy
        }
        icon.col = newCol
        icon.row = newRow
        val (nx, ny) = cellToPixel(newCol, newRow)
        icon.drawX = nx
        icon.drawY = ny
        invalidate()
    }

    private fun findIconAt(x: Float, y: Float): DesktopIcon? {
        val size = iconSizePx.toFloat()
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
