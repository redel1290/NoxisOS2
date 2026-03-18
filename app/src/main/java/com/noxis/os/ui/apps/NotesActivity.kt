package com.noxis.os.ui.apps

import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.noxis.os.util.SystemPaths
import com.noxis.os.util.dpToPx
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NotesActivity : BaseAppActivity() {

    override val appTitle = "Нотатки"
    private val notesDir = File(SystemPaths.homeDir, "notes")
    private var currentFile: File? = null
    private lateinit var editorLayout: LinearLayout
    private lateinit var listLayout: LinearLayout
    private lateinit var editor: EditText
    private lateinit var mainLayout: FrameLayout

    override fun onContentReady() {
        notesDir.mkdirs()
        mainLayout = contentRoot

        showList()
    }

    private fun showList() {
        mainLayout.removeAllViews()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Кнопка нова нотатка
        val newBtn = TextView(this).apply {
            text = "+ Нова нотатка"
            textSize = 14f
            setTextColor(Color.parseColor("#C8AAFF"))
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            setBackgroundColor(Color.parseColor("#1A1A1F"))
            isClickable = true; isFocusable = true
            setOnClickListener { openEditor(null) }
        }
        layout.addView(newBtn)
        layout.addView(divider())

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val files = notesDir.listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        if (files.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Нотаток немає"
                setTextColor(Color.parseColor("#8A8A9A"))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(32), 0, 0)
            })
        }

        files.forEach { f ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                isClickable = true; isFocusable = true
                setOnClickListener { openEditor(f) }
            }
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(this).apply {
                text = f.nameWithoutExtension
                textSize = 14f
                setTextColor(Color.parseColor("#F0F0F5"))
            })
            val preview = f.readText().take(60).replace('\n', ' ')
            texts.addView(TextView(this).apply {
                text = preview
                textSize = 11f
                setTextColor(Color.parseColor("#8A8A9A"))
            })
            row.addView(texts)

            // Кнопка видалення
            row.addView(TextView(this).apply {
                text = "🗑"
                textSize = 16f
                setPadding(dpToPx(8), 0, 0, 0)
                setOnClickListener {
                    f.delete()
                    showList()
                }
            })

            val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            wrap.addView(row)
            wrap.addView(divider())
            list.addView(wrap)
        }

        scroll.addView(list)
        layout.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        mainLayout.addView(layout)
    }

    private fun openEditor(file: File?) {
        currentFile = file ?: File(
            notesDir,
            "note_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
        )

        mainLayout.removeAllViews()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Тулбар редактора
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
            setBackgroundColor(Color.parseColor("#141417"))
        }

        val backBtn = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.parseColor("#8A8A9A"))
            setPadding(dpToPx(8), dpToPx(10), dpToPx(16), dpToPx(10))
            setOnClickListener { showList() }
        }

        val titleEdit = EditText(this).apply {
            setText(currentFile!!.nameWithoutExtension)
            textSize = 15f
            setTextColor(Color.parseColor("#F0F0F5"))
            setHintTextColor(Color.parseColor("#8A8A9A"))
            hint = "Назва нотатки"
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val saveBtn = TextView(this).apply {
            text = "Зберегти"
            textSize = 13f
            setTextColor(Color.parseColor("#C8AAFF"))
            setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
            setOnClickListener {
                val newFile = File(notesDir, "${titleEdit.text}.txt")
                if (newFile.absolutePath != currentFile!!.absolutePath) {
                    currentFile!!.renameTo(newFile)
                    currentFile = newFile
                }
                currentFile!!.writeText(editor.text.toString())
                Toast.makeText(this@NotesActivity, "Збережено", Toast.LENGTH_SHORT).show()
            }
        }

        toolbar.addView(backBtn)
        toolbar.addView(titleEdit)
        toolbar.addView(saveBtn)
        layout.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48)
        ))
        layout.addView(divider())

        editor = EditText(this).apply {
            setText(if (file?.exists() == true) file.readText() else "")
            setTextColor(Color.parseColor("#F0F0F5"))
            setHintTextColor(Color.parseColor("#8A8A9A"))
            hint = "Почніть писати..."
            background = null
            gravity = Gravity.TOP
            textSize = 14f
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }

        val scroll = ScrollView(this)
        scroll.addView(editor, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        layout.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        mainLayout.addView(layout)
    }

    private fun divider() = android.view.View(this).apply {
        setBackgroundColor(Color.parseColor("#1A1A1F"))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
    }

    override fun onBackPressed() {
        if (currentFile != null) showList()
        else super.onBackPressed()
    }
}
