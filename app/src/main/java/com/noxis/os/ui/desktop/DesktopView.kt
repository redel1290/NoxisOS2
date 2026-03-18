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
import android.view.animation.OvershootInterpolator
import com.noxis.os.system.NoxisSettings
import com.noxis.os.system.SettingsManager
import com.noxis.os.system.lki.AppInfo
import com.noxis.os.util.dpToPx
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class DesktopView(context: Context) : View(context) {

    private var settings: NoxisSettings = SettingsManager.get(context)
    private val desktopApps = mutableListOf<DesktopIcon>()
    private val allApps = mutableListOf<AppInfo>()

    // Drawer стан
    private var drawerProgress = 0f   // 0=закрито, 1=відкрито
    private var drawerAnimator: ValueAnimator? = null
    private var drawerScrollY = 0f
    private var drawerMaxScroll = 0f

    // Свайп детекція
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartDrawerScroll = 0f
    private var isSwipingUp = false
    private var isScrollingDrawer = false

    // Drag стан
    private var dragIcon: DesktopIcon? = null
    private var dragX = 0f
    private var dragY = 0f
    private var dragOffX = 0f
    private var dragOffY = 0f
    private var isDragging = false
    private var dragScale = 1f
    private val LONG_PRESS_MS = 500L
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    private val columns get() = settings.gridColumns
    private val iconSz get() = context.dpToPx(settings.iconSize).toFloat()
    private val DRAWER_COLS = 4

    // Кольори іконок One UI стиль
    private val iconColors = mapOf(
        "com.noxis.files"    to intArrayOf(0xFF1E88E5.toInt(), 0xFF42A5F5.toInt()),
        "com.noxis.notes"    to intArrayOf(0xFFFDD835.toInt(), 0xFFFFEE58.toInt()),
        "com.noxis.browser"  to intArrayOf(0xFF1565C0.toInt(), 0xFF1976D2.toInt()),
        "com.noxis.terminal" to intArrayOf(0xFF263238.toInt(), 0xFF37474F.toInt()),
        "com.noxis.settings" to intArrayOf(0xFF757575.toInt(), 0xFF9E9E9E.toInt())
    )
    private val iconSymbols = mapOf(
        "com.noxis.files"    to "Files",
        "com.noxis.notes"    to "Notes",
        "com.noxis.browser"  to "Web",
        "com.noxis.terminal" to ">_",
        "com.noxis.settings" to "Set"
    )

    // Пейнти
    private val wallpaperPaint = Paint()
    private val drawerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 1f, Color.parseColor("#99000000"))
    }
    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val drawerHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFFFFF")
        style = Paint.Style.FILL
    }
    private val drawerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val snapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#557B5EA7")
        style = Paint.Style.FILL
    }

    var onAppClick: ((AppInfo) -> Unit)? = null

    fun reload(appList: List<AppInfo>) {
        allApps.clear()
        allApps.addAll(appList)
        desktopApps.clear()
        appList.forEachIndexed { i, app ->
            val col = i % columns
            val row = i / columns
            val (x, y) = cellToPixel(col, row)
            desktopApps.add(DesktopIcon(app, col, row, x, y))
        }
        invalidate()
    }

    fun reloadSettings() {
        settings = SettingsManager.get(context)
        desktopApps.forEachIndexed { i, icon ->
            icon.col = i % columns; icon.row = i / columns
            val (x, y) = cellToPixel(icon.col, icon.row)
            icon.drawX = x; icon.drawY = y
        }
        invalidate()
    }

    // ── Позиції ──────────────────────────────────────────────

    private fun cellToPixel(col: Int, row: Int): Pair<Float, Float> {
        if (width == 0) return 0f to 0f
        val cellW = width.toFloat() / columns
        val cellH = iconSz + context.dpToPx(32)
        val x = col * cellW + (cellW - iconSz) / 2f
        val y = row * cellH + context.dpToPx(24)
        return x to y
    }

    private fun pixelToCell(x: Float, y: Float): Pair<Int, Int> {
        if (width == 0) return 0 to 0
        val cellW = width.toFloat() / columns
        val cellH = iconSz + context.dpToPx(32)
        return (x / cellW).toInt().coerceIn(0, columns - 1) to
               max(0, (y / cellH).toInt())
    }

    private fun drawerCellToPixel(col: Int, row: Int): Pair<Float, Float> {
        if (width == 0) return 0f to 0f
        val cellW = width.toFloat() / DRAWER_COLS
        val drawerIconSz = context.dpToPx(56).toFloat()
        val cellH = drawerIconSz + context.dpToPx(36)
        val x = col * cellW + (cellW - drawerIconSz) / 2f
        val y = context.dpToPx(80) + row * cellH - drawerScrollY
        return x to y
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        desktopApps.forEachIndexed { i, icon ->
            icon.col = i % columns; icon.row = i / columns
            val (x, y) = cellToPixel(icon.col, icon.row)
            icon.drawX = x; icon.drawY = y
        }
    }

    // ── Draw ─────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        // Фон
        canvas.drawColor(Color.parseColor("#0D0D0F"))

        // Робочий стіл
        drawDesktop(canvas)

        // Drawer поверх
        if (drawerProgress > 0f) {
            drawDrawer(canvas)
        }

        // Drag preview
        if (isDragging && dragIcon != null) {
            val (sc, sr) = pixelToCell(dragX, dragY)
            val (sx, sy) = cellToPixel(sc, sr)
            canvas.drawRoundRect(
                RectF(sx, sy, sx + iconSz, sy + iconSz),
                iconSz * 0.22f, iconSz * 0.22f, snapPaint
            )
            drawIcon(canvas, dragIcon!!, dragX - dragOffX, dragY - dragOffY, dragScale,
                     iconSz, labelPaint, symbolPaint)
        }
    }

    private fun drawDesktop(canvas: Canvas) {
        desktopApps.forEach { icon ->
            if (icon == dragIcon && isDragging) return@forEach
            drawIcon(canvas, icon, icon.drawX, icon.drawY, 1f, iconSz, labelPaint, symbolPaint)
        }
    }

    private fun drawDrawer(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val drawerTop = h * (1f - drawerProgress)

        // Фон drawer — темне скло One UI стиль
        val alpha = (drawerProgress * 245).toInt().coerceIn(0, 245)
        drawerBgPaint.color = Color.argb(alpha, 18, 18, 24)
        canvas.drawRect(0f, drawerTop, w, h, drawerBgPaint)

        // Handle зверху
        val handleW = context.dpToPx(40).toFloat()
        val handleH = context.dpToPx(4).toFloat()
        canvas.drawRoundRect(
            RectF(w / 2f - handleW / 2f, drawerTop + context.dpToPx(10),
                  w / 2f + handleW / 2f, drawerTop + context.dpToPx(10) + handleH),
            handleH / 2f, handleH / 2f, drawerHandlePaint
        )

        // Заголовок "Всі застосунки"
        drawerLabelPaint.textSize = context.dpToPx(13).toFloat()
        drawerLabelPaint.color = Color.parseColor("#88FFFFFF")
        if (drawerProgress > 0.3f) {
            canvas.drawText(
                "Всі застосунки",
                w / 2f,
                drawerTop + context.dpToPx(44),
                drawerLabelPaint
            )
        }

        // Іконки застосунків
        val drawerIconSz = context.dpToPx(56).toFloat()
        val drawerSymPaint = Paint(symbolPaint).apply { textSize = drawerIconSz * 0.36f }
        val drawerLblPaint = Paint(labelPaint).apply {
            textSize = context.dpToPx(11).toFloat()
        }

        canvas.save()
        canvas.clipRect(0f, drawerTop, w, h)

        allApps.forEachIndexed { i, app ->
            val col = i % DRAWER_COLS
            val row = i / DRAWER_COLS
            val (x, y) = drawerCellToPixel(col, row)
            val absY = drawerTop + (y - context.dpToPx(80) + drawerScrollY) *
                       drawerProgress + context.dpToPx(80) * drawerProgress -
                       drawerScrollY * drawerProgress

            // Спрощено: малюємо відносно drawerTop
            val realY = drawerTop + context.dpToPx(80) +
                        row * (drawerIconSz + context.dpToPx(36)) - drawerScrollY

            if (realY + drawerIconSz + context.dpToPx(36) < drawerTop) return@forEachIndexed
            if (realY > height.toFloat()) return@forEachIndexed

            val fakeIcon = DesktopIcon(app, col, row, x, realY)
            drawIcon(canvas, fakeIcon, x, realY, 1f, drawerIconSz, drawerLblPaint, drawerSymPaint)
        }

        canvas.restore()

        // Обчислити макс скрол
        val rows = (allApps.size + DRAWER_COLS - 1) / DRAWER_COLS
        val drawerIconSz2 = context.dpToPx(56).toFloat()
        val totalH = rows * (drawerIconSz2 + context.dpToPx(36)) + context.dpToPx(80).toFloat()
        drawerMaxScroll = max(0f, totalH - (h - drawerTop))
    }

    private fun drawIcon(
        canvas: Canvas, icon: DesktopIcon,
        x: Float, y: Float, scale: Float,
        size: Float,
        lPaint: Paint, sPaint: Paint
    ) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val s = size * scale
        val left = cx - s / 2f
        val top = cy - s / 2f
        val rect = RectF(left, top, left + s, top + s)
        val r = s * 0.22f  // One UI радіус заокруглення

        if (icon.app.icon != null) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val path = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(icon.app.icon, null, rect, p)
            canvas.restore()
        } else {
            // Тінь
            if (scale <= 1.05f) {
                shadowPaint.color = Color.argb(60, 0, 0, 0)
                canvas.drawRoundRect(
                    RectF(left + 2, top + 4, left + s + 2, top + s + 4),
                    r, r, shadowPaint
                )
            }

            // Градієнт фон
            val colors = getIconColors(icon.app.id)
            val gradient = LinearGradient(
                left, top, left + s, top + s,
                colors[0], colors[1], Shader.TileMode.CLAMP
            )
            iconBgPaint.shader = gradient
            canvas.drawRoundRect(rect, r, r, iconBgPaint)

            // Блик (One UI стиль — легкий зверху)
            val gloss = LinearGradient(
                left, top, left, top + s * 0.4f,
                Color.argb(50, 255, 255, 255),
                Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            iconGlossPaint.shader = gloss
            canvas.drawRoundRect(rect, r, r, iconGlossPaint)

            // Символ
            val sym = iconSymbols[icon.app.id] ?: icon.app.name.take(3)
            sPaint.textSize = s * 0.32f
            sPaint.color = Color.WHITE
            canvas.drawText(sym, cx, cy + sPaint.textSize * 0.35f, sPaint)
        }

        // Назва
        if (settings.iconLabelVisible) {
            lPaint.textSize = context.dpToPx(11).toFloat()
            lPaint.color = Color.WHITE
            canvas.drawText(icon.app.name, cx, top + s + context.dpToPx(16), lPaint)
        }
    }

    private fun getIconColors(id: String): IntArray {
        return iconColors[id] ?: run {
            val h = (abs(id.hashCode()) % 360).toFloat()
            intArrayOf(
                Color.HSVToColor(floatArrayOf(h, 0.75f, 0.75f)),
                Color.HSVToColor(floatArrayOf((h + 25) % 360, 0.55f, 0.92f))
            )
        }
    }

    // ── Touch ────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchStartDrawerScroll = drawerScrollY
                isSwipingUp = false
                isScrollingDrawer = false

                if (drawerProgress < 0.5f) {
                    // Десктоп — long press для drag
                    val icon = findDesktopIconAt(event.x, event.y)
                    if (icon != null) {
                        longPressRunnable = Runnable { startDrag(icon, event.x, event.y) }
                        handler.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY

                if (isDragging && dragIcon != null) {
                    dragX = event.x; dragY = event.y; invalidate()
                    return true
                }

                if (!isSwipingUp && !isScrollingDrawer && abs(dy) > context.dpToPx(8)) {
                    cancelDrag()
                    if (drawerProgress > 0.5f && dy > 0) {
                        // Скрол вниз в drawer — може бути закриття
                        isSwipingUp = true
                    } else if (dy < -context.dpToPx(8)) {
                        isSwipingUp = true
                    } else if (drawerProgress > 0.5f) {
                        isScrollingDrawer = true
                    }
                }

                if (isSwipingUp) {
                    val swipeProg = -dy / height.toFloat()
                    val newProg = (drawerProgress + swipeProg * 2f).coerceIn(0f, 1f)
                    drawerProgress = newProg
                    invalidate()
                }

                if (isScrollingDrawer && drawerProgress > 0.9f) {
                    drawerScrollY = (touchStartDrawerScroll - dy).coerceIn(0f, drawerMaxScroll)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                cancelDrag()

                if (isDragging && dragIcon != null) {
                    dropIcon(dragIcon!!, dragX, dragY)
                    return true
                }

                val dy = event.y - touchStartY
                val dx = event.x - touchStartX

                if (isSwipingUp) {
                    // Snap to open/close
                    val target = if (drawerProgress > 0.4f) 1f else 0f
                    animateDrawer(target)
                } else if (!isDragging && abs(dx) < context.dpToPx(8) && abs(dy) < context.dpToPx(8)) {
                    // Тап
                    if (drawerProgress > 0.5f) {
                        // Тап в drawer
                        val icon = findDrawerIconAt(event.x, event.y)
                        icon?.let { onAppClick?.invoke(it) }
                    } else {
                        val icon = findDesktopIconAt(event.x, event.y)
                        icon?.let { onAppClick?.invoke(it.app) }
                    }
                }

                isDragging = false
                dragIcon = null
                dragScale = 1f
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
                if (isDragging && dragIcon != null) dropIcon(dragIcon!!, dragIcon!!.drawX, dragIcon!!.drawY)
                isDragging = false; dragIcon = null; dragScale = 1f
                invalidate()
            }
        }
        return false
    }

    private fun startDrag(icon: DesktopIcon, x: Float, y: Float) {
        isDragging = true
        dragIcon = icon
        dragX = x; dragY = y
        dragOffX = x - icon.drawX
        dragOffY = y - icon.drawY
        try {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {}
        ValueAnimator.ofFloat(1f, 1.18f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { dragScale = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun dropIcon(icon: DesktopIcon, x: Float, y: Float) {
        val (nc, nr) = pixelToCell(x, y)
        val target = desktopApps.find { it != icon && it.col == nc && it.row == nr }
        if (target != null) {
            val oc = icon.col; val or = icon.row
            target.col = oc; target.row = or
            val (ox, oy) = cellToPixel(oc, or)
            target.drawX = ox; target.drawY = oy
        }
        icon.col = nc; icon.row = nr
        val (nx, ny) = cellToPixel(nc, nr)
        val fromX = x - dragOffX; val fromY = y - dragOffY
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                icon.drawX = fromX + (nx - fromX) * t
                icon.drawY = fromY + (ny - fromY) * t
                dragScale = dragScale + (1f - dragScale) * t
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    icon.drawX = nx; icon.drawY = ny
                    isDragging = false; dragIcon = null; dragScale = 1f
                    invalidate()
                }
            })
            start()
        }
    }

    private fun cancelDrag() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun animateDrawer(target: Float) {
        drawerAnimator?.cancel()
        drawerAnimator = ValueAnimator.ofFloat(drawerProgress, target).apply {
            duration = 350
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { drawerProgress = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    if (target == 0f) drawerScrollY = 0f
                }
            })
            start()
        }
    }

    fun closeDrawer() { if (drawerProgress > 0f) animateDrawer(0f) }
    fun isDrawerOpen() = drawerProgress > 0.5f

    private fun findDesktopIconAt(x: Float, y: Float): DesktopIcon? {
        val s = iconSz
        return desktopApps.firstOrNull { ic ->
            x >= ic.drawX && x <= ic.drawX + s && y >= ic.drawY && y <= ic.drawY + s
        }
    }

    private fun findDrawerIconAt(x: Float, y: Float): AppInfo? {
        val drawerIconSz = context.dpToPx(56).toFloat()
        val drawerTop = height * (1f - drawerProgress)
        allApps.forEachIndexed { i, app ->
            val col = i % DRAWER_COLS
            val row = i / DRAWER_COLS
            val realY = drawerTop + context.dpToPx(80) +
                        row * (drawerIconSz + context.dpToPx(36)) - drawerScrollY
            val cellW = width.toFloat() / DRAWER_COLS
            val iconX = col * cellW + (cellW - drawerIconSz) / 2f
            if (x >= iconX && x <= iconX + drawerIconSz &&
                y >= realY && y <= realY + drawerIconSz) return app
        }
        return null
    }

    data class DesktopIcon(
        val app: AppInfo,
        var col: Int, var row: Int,
        var drawX: Float, var drawY: Float
    )
}
