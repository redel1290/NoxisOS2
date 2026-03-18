package com.noxis.os.ui.apps

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import com.noxis.os.util.SystemPaths
import com.noxis.os.util.dpToPx
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileManagerActivity : BaseAppActivity() {

    override val appTitle = "Файли"
    private lateinit var pathView: TextView
    private lateinit var listLayout: LinearLayout
    private var currentDir = SystemPaths.homeDir
    private val dateFmt = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

    override fun onContentReady() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D0F"))
        }

        pathView = TextView(this).apply {
            setTextColor(Color.parseColor("#C8AAFF"))
            textSize = 11f
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundColor(Color.parseColor("#0F0F12"))
        }
        layout.addView(pathView)
        layout.addView(divider())

        val scroll = ScrollView(this)
        listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listLayout)
        layout.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        contentRoot.addView(layout)
        SystemPaths.homeDir.mkdirs()
        navigate(currentDir)
    }

    private fun navigate(dir: File) {
        currentDir = dir
        pathView.text = dir.absolutePath
            .replace(SystemPaths.externalRoot.absolutePath, "~/NoxisOS")
        listLayout.removeAllViews()

        if (dir.absolutePath != SystemPaths.homeDir.absolutePath && dir.parentFile != null) {
            listLayout.addView(row("⬆", "..", "Вгору") { navigate(dir.parentFile!!) })
        }

        val files = dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

        if (files.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "Порожньо"
                setTextColor(Color.parseColor("#8A8A9A"))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(32), 0, 0)
            })
            return
        }

        files.forEach { f ->
            listLayout.addView(row(
                if (f.isDirectory) "📁" else fileIcon(f.name),
                f.name,
                if (f.isDirectory) "${f.listFiles()?.size ?: 0} ел."
                else "${fmtSize(f.length())} · ${dateFmt.format(Date(f.lastModified()))}"
            ) { if (f.isDirectory) navigate(f) })
        }
    }

    private fun row(icon: String, name: String, sub: String, onClick: () -> Unit): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        row.addView(TextView(this).apply {
            text = icon; textSize = 22f
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).also {
                it.gravity = Gravity.CENTER
            }
        })
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dpToPx(12)
            }
        }
        texts.addView(TextView(this).apply {
            text = name; textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
        })
        texts.addView(TextView(this).apply {
            text = sub; textSize = 11f
            setTextColor(Color.parseColor("#8A8A9A"))
        })
        row.addView(texts)
        wrap.addView(row)
        wrap.addView(divider().also {
            (it.layoutParams as? LinearLayout.LayoutParams)?.marginStart = dpToPx(64)
        })
        return wrap
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#1A1A1F"))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
    }

    private fun fileIcon(name: String) = when {
        name.endsWith(".lua") -> "📜"
        name.endsWith(".lki") -> "📦"
        name.endsWith(".txt") || name.endsWith(".md") -> "📄"
        name.endsWith(".png") || name.endsWith(".jpg") -> "🖼"
        name.endsWith(".json") -> "⚙"
        else -> "📄"
    }

    private fun fmtSize(b: Long) = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "${b / 1024} KB"
        else -> "${b / (1024 * 1024)} MB"
    }

    override fun onBackPressed() {
        if (currentDir.absolutePath != SystemPaths.homeDir.absolutePath && currentDir.parentFile != null)
            navigate(currentDir.parentFile!!)
        else super.onBackPressed()
    }
}
