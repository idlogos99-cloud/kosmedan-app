package com.kosmedan.app

import android.os.Bundle
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var popupWebView: WebView? = null

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

                popupWebView!!.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, url: String?): Boolean {
                        // FIX: Kalau popup udah selesai dan balik ke kosmedan, tutup popup
                        if (url != null && url.contains("kosmedan.my.id")) {
                            closePopup()
                            webView.loadUrl(url)
                            return true
                        }
                        return false
                    }
                    override fun onPageFinished(v: WebView?, url: String?) {
                        super.onPageFinished(v, url)
                        // FIX UTAMA: Kalau popup udah jadi putih / blank, berarti login udah selesai
                        // Langsung tutup popup dan refresh halaman utama
                        if (url != null && (url == "about:blank" || url == "" || url.contains("__/auth/handler"))) {
                            // Kasih jeda 800ms biar Firebase sempat kirim data login ke halaman utama
                            v?.postDelayed({
                                closePopup()
                                webView.reload()
                            }, 800)
                        }
                    }
                }

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

    private fun closePopup() {
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
            return
        }
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
