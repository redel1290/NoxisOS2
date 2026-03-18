package com.noxis.os.ui.apps

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.noxis.os.system.PermissionGate
import com.noxis.os.util.SystemPaths
import com.noxis.os.util.dpToPx
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform

class TerminalActivity : BaseAppActivity() {

    override val appTitle = "Термінал"
    private lateinit var outputLayout: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scroll: ScrollView
    private val history = mutableListOf<String>()
    private var historyIndex = -1

    private val globals by lazy {
        JsePlatform.standardGlobals().also { g ->
            // noxis.print
            g.set("print", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    appendOutput(arg.tojstring(), "#F0F0F5")
                    return LuaValue.NONE
                }
            })
            // noxis.ls(path)
            g.set("ls", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    val path = if (arg.isnil()) SystemPaths.homeDir.absolutePath
                               else arg.tojstring()
                    val dir = java.io.File(path)
                    if (!dir.exists()) { appendOutput("ls: $path: немає такого файлу", "#FF5F57"); return LuaValue.NONE }
                    val files = dir.listFiles() ?: emptyArray()
                    files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                        .forEach { f ->
                            val color = if (f.isDirectory) "#C8AAFF" else "#F0F0F5"
                            val suffix = if (f.isDirectory) "/" else ""
                            appendOutput("${f.name}$suffix", color)
                        }
                    return LuaValue.NONE
                }
            })
            // noxis.cat(path)
            g.set("cat", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    val file = java.io.File(arg.tojstring())
                    if (!file.exists()) { appendOutput("cat: файл не знайдено", "#FF5F57"); return LuaValue.NONE }
                    appendOutput(file.readText(), "#F0F0F5")
                    return LuaValue.NONE
                }
            })
            // noxis.clear()
            g.set("clear", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    runOnUiThread { outputLayout.removeAllViews() }
                    return LuaValue.NONE
                }
            })
        }
    }

    override fun onContentReady() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A0C"))
        }

        scroll = ScrollView(this)
        outputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
        }
        scroll.addView(outputLayout)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            setBackgroundColor(Color.parseColor("#0F0F12"))
        }

        val prompt = TextView(this).apply {
            text = "lua> "
            setTextColor(Color.parseColor("#7B5EA7"))
            typeface = Typeface.MONOSPACE
            textSize = 13f
        }

        inputField = EditText(this).apply {
            setTextColor(Color.parseColor("#F0F0F5"))
            setHintTextColor(Color.parseColor("#3A3A50"))
            hint = "введіть Lua код..."
            background = null
            typeface = Typeface.MONOSPACE
            textSize = 13f
            imeOptions = EditorInfo.IME_ACTION_DONE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    execute(text.toString()); true
                } else false
            }
        }

        val runBtn = TextView(this).apply {
            text = "▶"
            setTextColor(Color.parseColor("#7B5EA7"))
            textSize = 18f
            setPadding(dpToPx(8), 0, 0, 0)
            setOnClickListener { execute(inputField.text.toString()) }
        }

        inputRow.addView(prompt)
        inputRow.addView(inputField)
        inputRow.addView(runBtn)

        layout.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        layout.addView(inputRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        contentRoot.addView(layout)

        appendOutput("Noxis Lua Terminal v1.0", "#7B5EA7")
        appendOutput("Доступні функції: print(), ls(), cat(), clear()", "#8A8A9A")
        appendOutput("", "#F0F0F5")
    }

    private fun execute(code: String) {
        if (code.isBlank()) return
        history.add(0, code)
        historyIndex = -1

        appendOutput("lua> $code", "#C8AAFF")
        inputField.setText("")

        Thread {
            try {
                val result = globals.load(code).call()
                if (!result.isnil() && result != LuaValue.NONE) {
                    appendOutput(result.tojstring(), "#28C840")
                }
            } catch (e: LuaError) {
                appendOutput("Помилка: ${e.message}", "#FF5F57")
            } catch (e: Exception) {
                appendOutput("Помилка: ${e.message}", "#FF5F57")
            }
            runOnUiThread { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }.apply { isDaemon = true; start() }
    }

    private fun appendOutput(text: String, color: String) {
        runOnUiThread {
            outputLayout.addView(TextView(this).apply {
                this.text = text
                setTextColor(Color.parseColor(color))
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setPadding(0, 1, 0, 1)
            })
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }
}
