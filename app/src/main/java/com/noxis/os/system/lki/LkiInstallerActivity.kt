package com.noxis.os.system.lki

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.noxis.os.util.SystemPaths
import com.noxis.os.util.dpToPx
import java.io.File
import java.util.zip.ZipInputStream

class LkiInstallerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data ?: run { finish(); return }
        handleInstall(uri)
    }

    private fun handleInstall(uri: Uri) {
        val tmp = copyToCache(uri) ?: run { toast("Помилка читання файлу"); finish(); return }
        val manifest = readManifest(tmp) ?: run {
            tmp.delete(); toast("Невалідний .lki пакет"); finish(); return
        }
        if (!manifest.isValid()) {
            tmp.delete(); toast("Маніфест не містить обов'язкових полів"); finish(); return
        }
        showDialog(manifest, tmp)
    }

    private fun showDialog(manifest: LkiManifest, tmp: File) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A1F"))
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
        }

        // Іконка
        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(72), dpToPx(72)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.bottomMargin = dpToPx(12)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        try {
            ZipInputStream(tmp.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == manifest.icon) {
                        iconView.setImageBitmap(BitmapFactory.decodeStream(zip))
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) { }
        root.addView(iconView)

        root.addView(TextView(this).apply {
            text = manifest.name
            textSize = 18f
            setTextColor(Color.parseColor("#F0F0F5"))
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "v${manifest.version} · ${manifest.author}"
            textSize = 12f
            setTextColor(Color.parseColor("#8A8A9A"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, dpToPx(12))
        })
        if (manifest.description.isNotBlank()) {
            root.addView(TextView(this).apply {
                text = manifest.description
                textSize = 13f
                setTextColor(Color.parseColor("#8A8A9A"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dpToPx(16))
            })
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val cancelBtn = TextView(this).apply {
            text = "Скасувати"
            textSize = 14f
            setTextColor(Color.parseColor("#8A8A9A"))
            setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))
            setOnClickListener { tmp.delete(); finish() }
        }
        val installBtn = TextView(this).apply {
            text = "Встановити"
            textSize = 14f
            setTextColor(Color.parseColor("#C8AAFF"))
            setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))
            setOnClickListener { performInstall(manifest, tmp) }
        }
        btnRow.addView(cancelBtn)
        btnRow.addView(installBtn)
        root.addView(btnRow)

        setContentView(root)
    }

    private fun performInstall(manifest: LkiManifest, tmp: File) {
        val installDir = File(SystemPaths.appsDir, manifest.id)
        if (installDir.exists()) installDir.deleteRecursively()
        installDir.mkdirs()

        try {
            ZipInputStream(tmp.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val out = File(installDir, entry.name)
                    if (entry.isDirectory) out.mkdirs()
                    else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            // Зберегти meta.json
            File(installDir, "meta.json").writeText(
                Gson().toJson(mapOf(
                    "id" to manifest.id, "name" to manifest.name,
                    "version" to manifest.version, "author" to manifest.author,
                    "description" to manifest.description, "mainScript" to manifest.main
                ))
            )
            toast("Встановлено: ${manifest.name}")
            setResult(Activity.RESULT_OK, Intent().putExtra("app_id", manifest.id))
        } catch (e: Exception) {
            installDir.deleteRecursively()
            toast("Помилка: ${e.message}")
        } finally {
            tmp.delete()
            finish()
        }
    }

    private fun readManifest(file: File): LkiManifest? {
        return try {
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.json")
                        return Gson().fromJson(zip.bufferedReader().readText(), LkiManifest::class.java)
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        } catch (e: Exception) { null }
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val name = getFileName(uri) ?: "package.lki"
            val f = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { it.copyTo(f.outputStream()) }
            f
        } catch (e: Exception) { null }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = it.getString(i)
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
