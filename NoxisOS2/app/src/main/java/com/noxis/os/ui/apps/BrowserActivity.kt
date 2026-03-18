package com.noxis.os.ui.apps

import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.noxis.os.util.dpToPx

class BrowserActivity : BaseAppActivity() {

    override val appTitle = "Браузер"
    override val showTitle = false
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar

    override fun onContentReady() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // URL бар
        val urlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            setBackgroundColor(Color.parseColor("#141417"))
        }

        val backBtn = TextView(this).apply {
            text = "◀"
            textSize = 16f
            setTextColor(Color.parseColor("#8A8A9A"))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(10), dpToPx(6))
            setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        }

        urlBar = EditText(this).apply {
            hint = "Введіть адресу..."
            setTextColor(Color.parseColor("#F0F0F5"))
            setHintTextColor(Color.parseColor("#8A8A9A"))
            textSize = 13f
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            setBackgroundColor(Color.parseColor("#1A1A1F"))
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI or android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dpToPx(4)
                it.marginEnd = dpToPx(4)
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    loadUrl(text.toString())
                    true
                } else false
            }
        }

        val reloadBtn = TextView(this).apply {
            text = "↻"
            textSize = 18f
            setTextColor(Color.parseColor("#8A8A9A"))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener { webView.reload() }
        }

        urlRow.addView(backBtn)
        urlRow.addView(urlBar)
        urlRow.addView(reloadBtn)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7B5EA7"))
            visibility = android.view.View.GONE
        }

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    urlBar.setText(url)
                    progressBar.visibility = android.view.View.VISIBLE
                }
                override fun onPageFinished(view: WebView, url: String) {
                    progressBar.visibility = android.view.View.GONE
                    urlBar.setText(url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.progress = newProgress
                }
                override fun onReceivedTitle(view: WebView, title: String) {
                    setTitle(title.take(30))
                }
            }
        }

        layout.addView(urlRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48)
        ))
        layout.addView(progressBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(3)
        ))
        layout.addView(webView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        contentRoot.addView(layout)
        loadUrl("https://duckduckgo.com")
    }

    private fun loadUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://duckduckgo.com/?q=${android.net.Uri.encode(input)}"
        }
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
