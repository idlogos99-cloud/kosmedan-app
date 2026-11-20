package com.kosmedan.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout
    private lateinit var webView: WebView
    private var popupWebView: WebView? = null

    // FIX UPLOAD FOTO: Penampung callback file picker
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback == null) return@registerForActivityResult
        var results: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                // Kalau pilih banyak foto
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    val uris = ArrayList<Uri>()
                    for (i in 0 until count) {
                        uris.add(data.clipData!!.getItemAt(i).uri)
                    }
                    results = uris.toTypedArray()
                } else if (data.data != null) {
                    // Kalau pilih 1 foto
                    results = arrayOf(data.data!!)
                }
            }
        }
        // Kalau batal pilih, kirim null biar WebView ga nge-hang
        fileUploadCallback?.onReceiveValue(results)
        fileUploadCallback = null
    }

    private fun openFilePicker(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        // Kalau ada callback lama yang belum kelar, batalin dulu
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = filePathCallback
        try {
            val intent = fileChooserParams.createIntent()
            // Biar bisa pilih banyak foto sekaligus (multiple)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback kalau createIntent gagal (HP jadul)
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            filePickerLauncher.launch(intent)
        }
        return true
    }

    // FIX WA: Fungsi khusus buka link WA / intent:// biar ga halaman putih
    private fun handleExternalLink(url: String): Boolean {
        // 1. Kalau intent://send?phone=... -> parse pakai URI_INTENT_SCHEME
        if (url.startsWith("intent://")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback ke wa.me kalau parse gagal
                try {
                    val phone = url.substringAfter("phone=").substringBefore("&").substringBefore(";").substringBefore("#")
                    val textPart = if (url.contains("text=")) url.substringAfter("text=").substringBefore(";").substringBefore("#") else ""
                    val waUrl = if (phone.isNotEmpty()) "https://wa.me/$phone?text=$textPart" else "https://wa.me/6281260710087"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                } catch (e2: Exception) { }
            }
            return true
        }

        // 2. Kalau wa.me / whatsapp / tel: / mailto: -> buka di luar APK
        if (url.startsWith("https://wa.me") || url.startsWith("https://api.whatsapp.com") || url.startsWith("whatsapp://") || url.startsWith("tel:") || url.startsWith("mailto:")) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) { }
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KosMedan)
        super.onCreate(savedInstanceState)

        container = FrameLayout(this)
        setContentView(container)

        webView = WebView(this)
        container.addView(webView)

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(true)
        s.allowFileAccess = true
        s.allowContentAccess = true
        // FIX APK CACHE: Ini biangnya - LOAD_DEFAULT bikin APK load JS lama tanpa api_key
        // Ganti ke NO_CACHE biar selalu load Blogger terbaru yang udah bener
        s.cacheMode = WebSettings.LOAD_NO_CACHE
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.mediaPlaybackRequiresUserGesture = false
        s.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        // FIX APK CACHE: Hapus cache lama yang isinya kode tanpa api_key
        webView.clearCache(true)
        webView.clearHistory()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // FIX WA: Cek dulu link WA / intent://
                if (handleExternalLink(url)) {
                    return true
                }

                // Biar semua link kosmedan.my.id tetep di dalam APK, ga mental ke Chrome
                if (url.contains("kosmedan.my.id")) {
                    return false
                }
                // Link luar lainnya buka di browser luar
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) { }
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // FIX UPLOAD FOTO: Ini yang bikin Pilih File kebuka di APK
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (fileChooserParams == null) return false
                return openFilePicker(filePathCallback, fileChooserParams)
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                popupWebView = WebView(this@MainActivity)
                popupWebView!!.settings.javaScriptEnabled = true
                popupWebView!!.settings.domStorageEnabled = true
                popupWebView!!.settings.databaseEnabled = true
                popupWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
                popupWebView!!.settings.setSupportMultipleWindows(true)
                popupWebView!!.settings.allowFileAccess = true
                popupWebView!!.settings.allowContentAccess = true
                popupWebView!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                popupWebView!!.settings.userAgentString = webView.settings.userAgentString

                // FIX WA POPUP: Pakai webViewClient yang sama biar popup juga bisa buka WA
                popupWebView!!.webViewClient = webView.webViewClient
                popupWebView!!.webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        if (fileChooserParams == null) return false
                        return openFilePicker(filePathCallback, fileChooserParams)
                    }
                    override fun onCloseWindow(window: WebView?) {
                        super.onCloseWindow(window)
                        closePopup()
                    }
                }

                // DITUMPUK, BUKAN GANTIIN - WebView utama tetap ada di bawah
                container.addView(popupWebView!!)

                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = popupWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                closePopup()
            }
        }

        webView.loadUrl("https://kosmedan.my.id")
    }

    private fun closePopup() {
        if (popupWebView != null) {
            container.removeView(popupWebView)
            popupWebView?.destroy()
            popupWebView = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (popupWebView != null) {
            closePopup()
            return
        }
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
