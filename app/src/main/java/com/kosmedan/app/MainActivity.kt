package com.kosmedan.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var popupWebView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var popupCheckRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KosMedan)
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(true)
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                popupWebView = WebView(this@MainActivity)
                popupWebView!!.settings.javaScriptEnabled = true
                popupWebView!!.settings.domStorageEnabled = true
                popupWebView!!.settings.databaseEnabled = true
                popupWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
                popupWebView!!.settings.setSupportMultipleWindows(true)
                popupWebView!!.settings.userAgentString = webView.settings.userAgentString
                popupWebView!!.webViewClient = WebViewClient()
                popupWebView!!.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        super.onCloseWindow(window)
                        closePopup()
                        webView.reload()
                    }
                }
                setContentView(popupWebView!!)
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = popupWebView
                resultMsg.sendToTarget()

                // MULAI PANTAU POPUP TIAP 500ms
                startPopupWatcher()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                closePopup()
                webView.reload()
            }
        }

        webView.loadUrl("https://kosmedan.my.id")
    }

    private fun startPopupWatcher() {
        popupCheckRunnable = object : Runnable {
            override fun run() {
                val popup = popupWebView
                if (popup == null) return
                val url = popup.url ?: ""
                // Kalau popup udah blank / handler / udah ga ada isinya, berarti login selesai
                // Tunggu 1 detik biar Firebase sempat kirim token, baru tutup
                if (url == "about:blank" || url == "" || url.contains("__/auth/handler")) {
                    handler.postDelayed({
                        closePopup()
                        webView.reload()
                    }, 1000)
                    return
                }
                // Kalau belum, cek lagi 500ms kemudian
                handler.postDelayed(this, 500)
            }
        }
        handler.postDelayed(popupCheckRunnable!!, 1000)
    }

    private fun closePopup() {
        popupCheckRunnable?.let { handler.removeCallbacks(it) }
        popupCheckRunnable = null
        if (popupWebView != null) {
            setContentView(webView)
            popupWebView?.destroy()
            popupWebView = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (popupWebView != null) {
            closePopup()
            webView.reload()
            return
        }
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
