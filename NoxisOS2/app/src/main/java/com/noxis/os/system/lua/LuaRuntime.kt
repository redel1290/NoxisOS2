package com.noxis.os.system.lua

import android.content.Context
import android.util.Log
import com.noxis.os.system.PermissionGate
import com.noxis.os.system.lki.AppInfo
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

class LuaRuntime(private val context: Context) {

    fun launch(app: AppInfo, windowApi: LuaWindowApi): LuaSession? {
        val scriptFile = File(app.installDir, app.mainScript)
        if (!scriptFile.exists()) return null

        return try {
            val globals = JsePlatform.standardGlobals()
            globals.set("noxis", buildApi(globals, app, windowApi))
            globals.set("APP_DIR", LuaValue.valueOf(app.installDir))

            val session = LuaSession(globals, scriptFile, app)
            session.start()
            session
        } catch (e: Exception) {
            Log.e("LuaRuntime", "launch error: ${e.message}")
            null
        }
    }

    private fun buildApi(globals: org.luaj.vm2.Globals, app: AppInfo, win: LuaWindowApi): LuaTable {
        val api = LuaTable()

        api.set("print", object : OneArgFunction() {
            override fun call(msg: LuaValue): LuaValue {
                Log.i("Lua[${app.id}]", msg.tojstring())
                return LuaValue.NONE
            }
        })

        api.set("setTitle", object : OneArgFunction() {
            override fun call(title: LuaValue): LuaValue {
                win.onSetTitle(title.tojstring()); return LuaValue.NONE
            }
        })

        api.set("setContent", object : OneArgFunction() {
            override fun call(content: LuaValue): LuaValue {
                win.onSetContent(content.tojstring()); return LuaValue.NONE
            }
        })

        api.set("readFile", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val file = PermissionGate.resolvePath(context, app.id, path.tojstring())
                    ?: return LuaValue.NIL
                return try {
                    if (file.exists()) LuaValue.valueOf(file.readText()) else LuaValue.NIL
                } catch (e: Exception) { LuaValue.NIL }
            }
        })

        api.set("writeFile", object : TwoArgFunction() {
            override fun call(path: LuaValue, data: LuaValue): LuaValue {
                val file = PermissionGate.resolvePath(context, app.id, path.tojstring())
                    ?: return LuaValue.FALSE
                return try {
                    file.parentFile?.mkdirs()
                    file.writeText(data.tojstring())
                    LuaValue.TRUE
                } catch (e: Exception) { LuaValue.FALSE }
            }
        })

        api.set("close", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                win.onClose(); return LuaValue.NONE
            }
        })

        api.set("appId", LuaValue.valueOf(app.id))
        api.set("appDir", LuaValue.valueOf(app.installDir))

        return api
    }
}

interface LuaWindowApi {
    fun onSetTitle(title: String)
    fun onSetContent(content: String)
    fun onClose()
}

class LuaSession(
    private val globals: org.luaj.vm2.Globals,
    private val scriptFile: File,
    val app: AppInfo
) {
    private var running = false

    fun start() {
        running = true
        Thread {
            try {
                globals.loadfile(scriptFile.absolutePath).call()
            } catch (e: LuaError) {
                Log.e("LuaSession", "[${app.id}] ${e.message}")
            } finally { running = false }
        }.apply { isDaemon = true; name = "lua-${app.id}"; start() }
    }

    fun stop() { running = false }
    fun isRunning() = running
}
