package com.noxis.os.ui.settings

import android.app.ActivityManager
import android.app.AlertDialog
import android.graphics.Color
import android.os.Build
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.widget.*
import com.noxis.os.system.*
import com.noxis.os.ui.apps.BaseAppActivity
import com.noxis.os.util.SystemPaths
import com.noxis.os.util.dpToPx

class SettingsActivity : BaseAppActivity() {

    override val appTitle = "Налаштування"
    private lateinit var scroll: ScrollView
    private lateinit var mainList: LinearLayout

    override fun onContentReady() {
        scroll = ScrollView(this)
        mainList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D0F"))
        }
        scroll.addView(mainList)
        contentRoot.addView(scroll)

        buildMainMenu()
    }

    // ── Головне меню налаштувань ─────────────────────────────
    private fun buildMainMenu() {
        mainList.removeAllViews()

        val sections = listOf(
            Triple("🎨", "Вигляд", ::openAppearance),
            Triple("📐", "Робочий стіл", ::openDesktop),
            Triple("🔲", "Навігація", ::openNavigation),
            Triple("✨", "Анімації", ::openAnimations),
            Triple("🌐", "Мережа та оновлення", ::openNetwork),
            Triple("🌍", "Мова", ::openLanguage),
            Triple("ℹ️", "Про OS", ::openAbout)
        )

        sections.forEach { (icon, label, action) ->
            mainList.addView(menuRow(icon, label, action))
        }
    }

    // ── Вигляд ──────────────────────────────────────────────
    private fun openAppearance() {
        buildSubScreen("Вигляд") { layout ->
            val s = SettingsManager.get(this)

            layout.addView(sectionLabel("Тема"))
            layout.addView(switchRow("Темна тема", s.darkMode) { v ->
                SettingsManager.update(this) { copy(darkMode = v) }
            })

            layout.addView(sectionLabel("Акцентний колір"))
            val colors = listOf(
                "#7B5EA7" to "Фіолетовий",
                "#5E8FA7" to "Синій",
                "#5EA77B" to "Зелений",
                "#A7775E" to "Помаранчевий",
                "#A75E5E" to "Червоний"
            )
            val colorRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            }
            colors.forEach { (hex, name) ->
                val dot = View(this).apply {
                    val size = dpToPx(32)
                    layoutParams = LinearLayout.LayoutParams(size, size).also {
                        it.marginEnd = dpToPx(8)
                    }
                    background = android.graphics.drawable.ShapeDrawable(
                        android.graphics.drawable.shapes.OvalShape()
                    ).apply { paint.color = Color.parseColor(hex) }
                    isClickable = true
                    setOnClickListener {
                        SettingsManager.update(this@SettingsActivity) { copy(accentColor = hex) }
                        Toast.makeText(this@SettingsActivity, name, Toast.LENGTH_SHORT).show()
                    }
                }
                colorRow.addView(dot)
            }
            layout.addView(colorRow)

            layout.addView(sectionLabel("Шпалери"))
            layout.addView(infoRow("Шпалери", "Буде у наступній версії"))
        }
    }

    // ── Робочий стіл ────────────────────────────────────────
    private fun openDesktop() {
        buildSubScreen("Робочий стіл") { layout ->
            val s = SettingsManager.get(this)

            layout.addView(sectionLabel("Сітка"))
            layout.addView(sliderRow("Колонки", s.gridColumns, 3, 6) { v ->
                SettingsManager.update(this) { copy(gridColumns = v) }
            })
            layout.addView(sliderRow("Розмір іконок (dp)", s.iconSize, 48, 80) { v ->
                SettingsManager.update(this) { copy(iconSize = v) }
            })

            layout.addView(sectionLabel("Іконки"))
            layout.addView(switchRow("Показувати назви", s.iconLabelVisible) { v ->
                SettingsManager.update(this) { copy(iconLabelVisible = v) }
            })
        }
    }

    // ── Навігація ───────────────────────────────────────────
    private fun openNavigation() {
        buildSubScreen("Навігація") { layout ->
            val s = SettingsManager.get(this)

            layout.addView(switchRow("Показувати навбар", s.navbarVisible) { v ->
                SettingsManager.update(this) { copy(navbarVisible = v) }
            })
            layout.addView(switchRow("Вібрація при натисканні", s.navbarHaptic) { v ->
                SettingsManager.update(this) { copy(navbarHaptic = v) }
            })
            layout.addView(switchRow("Показувати статус бар", s.statusbarVisible) { v ->
                SettingsManager.update(this) { copy(statusbarVisible = v) }
            })
            layout.addView(switchRow("Годинник у статус барі", s.statusbarClock) { v ->
                SettingsManager.update(this) { copy(statusbarClock = v) }
            })
            layout.addView(switchRow("Батарея у статус барі", s.statusbarBattery) { v ->
                SettingsManager.update(this) { copy(statusbarBattery = v) }
            })
        }
    }

    // ── Анімації ────────────────────────────────────────────
    private fun openAnimations() {
        buildSubScreen("Анімації") { layout ->
            val s = SettingsManager.get(this)

            layout.addView(switchRow("Увімкнути анімації", s.animations) { v ->
                SettingsManager.update(this) { copy(animations = v) }
            })
            layout.addView(sliderRow(
                "Швидкість (×10)", (s.transitionSpeed * 10).toInt(), 5, 20
            ) { v ->
                SettingsManager.update(this) { copy(transitionSpeed = v / 10f) }
            })
        }
    }

    // ── Мережа ──────────────────────────────────────────────
    private fun openNetwork() {
        buildSubScreen("Мережа та оновлення") { layout ->
            val s = SettingsManager.get(this)

            layout.addView(switchRow("Серверне підключення", s.serverEnabled) { v ->
                SettingsManager.update(this) { copy(serverEnabled = v) }
            })
            layout.addView(inputRow("URL сервера", s.serverUrl) { v ->
                SettingsManager.update(this) { copy(serverUrl = v) }
            })
            layout.addView(spinnerRow(
                "Канал оновлень",
                listOf("Стабільний" to "stable", "Бета" to "beta"),
                s.updateChannel
            ) { v -> SettingsManager.update(this) { copy(updateChannel = v) } })

            layout.addView(divider())
            layout.addView(sectionLabel("Прошивки"))
            layout.addView(infoRow("Активна прошивка", s.activeFirmware))
            layout.addView(infoRow("Версія прошивки", s.firmwareVersion))

            val firmware = FirmwareManager.getActive(this)
            layout.addView(infoRow(
                "Root доступ",
                if (firmware.rootAccess) "Так" else "Ні"
            ))
        }
    }

    // ── Мова ────────────────────────────────────────────────
    private fun openLanguage() {
        buildSubScreen("Мова") { layout ->
            val s = SettingsManager.get(this)
            layout.addView(spinnerRow(
                "Мова інтерфейсу",
                listOf("Українська" to "uk", "English" to "en"),
                s.language
            ) { v -> SettingsManager.update(this) { copy(language = v) } })
        }
    }

    // ── Про OS ──────────────────────────────────────────────
    private fun openAbout() {
        buildSubScreen("Про OS") { layout ->
            val kernel = KernelManager.get(this)
            val settings = SettingsManager.get(this)
            val firmware = FirmwareManager.getActive(this)

            layout.addView(sectionLabel("Система"))
            layout.addView(infoRow("Noxis OS", BuildConfig.NOXIS_VERSION))
            layout.addView(infoRow("Прошивка", "${firmware.name} ${firmware.version}"))

            // Статус ОС — залежить від everUnlocked
            val statusColor = when (kernel.officialStatus) {
                KernelInfo.OfficialStatus.OFFICIAL -> "#28C840"
                KernelInfo.OfficialStatus.UNOFFICIAL -> "#FFBD2E"
            }
            val statusText = when (kernel.officialStatus) {
                KernelInfo.OfficialStatus.OFFICIAL -> "Офіційний"
                KernelInfo.OfficialStatus.UNOFFICIAL -> "Неофіційний"
            }
            layout.addView(infoRow("Статус ОС", statusText, statusColor))

            layout.addView(divider())
            layout.addView(sectionLabel("Завантажувач"))

            val bootText = if (kernel.bootloaderUnlocked) "Розблоковано" else "Заблоковано"
            val bootColor = if (kernel.bootloaderUnlocked) "#FFBD2E" else "#28C840"
            layout.addView(infoRow("Стан завантажувача", bootText, bootColor))

            kernel.unlockDate?.let { date ->
                val fmt = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                layout.addView(infoRow("Дата розблокування", fmt.format(java.util.Date(date))))
            }

            // Кнопка розблокування (тільки якщо заблоковано)
            if (!kernel.bootloaderUnlocked) {
                layout.addView(divider())
                layout.addView(actionRow("Розблокувати завантажувач", "#FFBD2E") {
                    showBootloaderUnlockDialog()
                })
            }

            layout.addView(divider())
            layout.addView(sectionLabel("Пристрій"))

            // Показуємо тільки те що точно доступно
            layout.addView(infoRow("Модель", Build.MODEL))
            layout.addView(infoRow("Android", Build.VERSION.RELEASE))
            layout.addView(infoRow("ABI", Build.SUPPORTED_ABIS.firstOrNull() ?: ""))

            // RAM
            try {
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                val totalRam = mi.totalMem / (1024 * 1024)
                layout.addView(infoRow("RAM", "$totalRam MB"))
            } catch (e: Exception) { }

            // ROM
            try {
                val stat = StatFs(SystemPaths.externalRoot.absolutePath)
                val total = stat.totalBytes / (1024 * 1024 * 1024)
                layout.addView(infoRow("Сховище", "$total GB"))
            } catch (e: Exception) { }

            // Дата встановлення Noxis
            val installFmt = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            layout.addView(infoRow(
                "Встановлено",
                installFmt.format(java.util.Date(kernel.installDate))
            ))
        }
    }

    // ── Діалог розблокування bootloader ─────────────────────
    private fun showBootloaderUnlockDialog() {
        val settings = SettingsManager.get(this)

        if (!settings.serverEnabled || settings.serverUrl.isBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Неможливо розблокувати")
                .setMessage("Для розблокування завантажувача необхідне підключення до сервера. Увімкніть серверне підключення в Мережа та оновлення.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Повноекранний діалог підтвердження
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D0F"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(32), dpToPx(48), dpToPx(32), dpToPx(48))
        }

        dialogView.addView(TextView(this).apply {
            text = "⚠"
            textSize = 64f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFBD2E"))
        })

        dialogView.addView(TextView(this).apply {
            text = "Розблокування завантажувача"
            textSize = 20f
            setTextColor(Color.parseColor("#F0F0F5"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(16), 0, dpToPx(8))
        })

        dialogView.addView(TextView(this).apply {
            text = "Ця дія незворотна.\n\nСтатус системи назавжди зміниться на «Неофіційний» навіть якщо завантажувач буде заблоковано знову.\n\nПродовжити?"
            textSize = 14f
            setTextColor(Color.parseColor("#8A8A9A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(32))
        })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val cancelBtn = TextView(this).apply {
            text = "Скасувати"
            textSize = 15f
            setTextColor(Color.parseColor("#8A8A9A"))
            setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12))
        }

        val confirmBtn = TextView(this).apply {
            text = "Розблокувати"
            textSize = 15f
            setTextColor(Color.parseColor("#FFBD2E"))
            setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12))
        }

        btnRow.addView(cancelBtn)
        btnRow.addView(confirmBtn)
        dialogView.addView(btnRow)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        cancelBtn.setOnClickListener { dialog.dismiss() }
        confirmBtn.setOnClickListener {
            dialog.dismiss()
            // TODO: запит до сервера для підтвердження
            // Після підтвердження сервера:
            KernelManager.unlock(this)
            Toast.makeText(this, "Завантажувач розблоковано. Статус: Неофіційний", Toast.LENGTH_LONG).show()
            openAbout() // оновити екран
        }

        dialog.show()
    }

    // ── Допоміжні методи побудови UI ────────────────────────

    private fun buildSubScreen(title: String, builder: (LinearLayout) -> Unit) {
        mainList.removeAllViews()

        // Кнопка назад
        val backRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            isClickable = true; isFocusable = true
            setOnClickListener { buildMainMenu() }
        }
        backRow.addView(TextView(this).apply {
            text = "◀  $title"
            textSize = 16f
            setTextColor(Color.parseColor("#C8AAFF"))
        })

        mainList.addView(backRow)
        mainList.addView(divider())

        builder(mainList)
    }

    private fun menuRow(icon: String, label: String, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        row.addView(TextView(this).apply {
            text = icon; textSize = 20f
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).also {
                it.gravity = Gravity.CENTER
            }
        })
        row.addView(TextView(this).apply {
            text = label; textSize = 15f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dpToPx(12)
            }
        })
        row.addView(TextView(this).apply {
            text = "▶"; textSize = 12f
            setTextColor(Color.parseColor("#3A3A50"))
        })

        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(divider())
        return wrap
    }

    private fun switchRow(label: String, current: Boolean, onChange: (Boolean) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(Switch(this).apply {
            isChecked = current
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7B5EA7"))
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(divider())
        return wrap
    }

    private fun sliderRow(label: String, current: Int, min: Int, max: Int, onChange: (Int) -> Unit): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val valueView = TextView(this).apply {
            text = current.toString()
            setTextColor(Color.parseColor("#C8AAFF"))
            textSize = 13f
        }
        headerRow.addView(TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(valueView)
        wrap.addView(headerRow)
        wrap.addView(SeekBar(this).apply {
            this.max = max - min
            progress = current - min
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7B5EA7"))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    val v = p + min
                    valueView.text = v.toString()
                    if (fromUser) onChange(v)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        })
        wrap.addView(divider())
        return wrap
    }

    private fun inputRow(label: String, current: String, onChange: (String) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(EditText(this).apply {
            setText(current)
            setTextColor(Color.parseColor("#F0F0F5"))
            setHintTextColor(Color.parseColor("#8A8A9A"))
            setBackgroundColor(Color.parseColor("#1A1A1F"))
            textSize = 12f
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(dpToPx(160), LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnFocusChangeListener { _, has -> if (!has) onChange(text.toString()) }
        })
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(divider())
        return wrap
    }

    private fun spinnerRow(
        label: String,
        options: List<Pair<String, String>>,
        current: String,
        onChange: (String) -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(Color.parseColor("#F0F0F5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val labels = options.map { it.first }
        val values = options.map { it.second }
        row.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, labels)
                .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(values.indexOf(current).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    onChange(values[pos])
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(divider())
        return wrap
    }

    private fun infoRow(label: String, value: String, valueColor: String = "#F0F0F5"): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 13f
            setTextColor(Color.parseColor("#8A8A9A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 13f
            setTextColor(Color.parseColor(valueColor))
        })
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(divider())
        return wrap
    }

    private fun actionRow(label: String, color: String = "#C8AAFF", onClick: () -> Unit): View {
        val row = TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(Color.parseColor(color))
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(divider())
        return wrap
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text; textSize = 11f
        setTextColor(Color.parseColor("#7B5EA7"))
        setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(4))
        isAllCaps = true
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#1A1A1F"))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
    }

    override fun onBackPressed() {
        // Якщо в підрозділі — повертаємось у головне меню
        buildMainMenu()
    }
}
