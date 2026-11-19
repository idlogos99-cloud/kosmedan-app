package com.kosmedan.app

import android.os.Bundle
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout
    private lateinit var webView: WebView
    private var popupWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KosMedan)
        super.onCreate(savedInstanceState)

        // FIX UTAMA: Pake FrameLayout, jadi WebView utama TETAP nempel di layar
        // Popup cuma ditumpuk di atasnya, bukan gantiin
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
                        // Firebase yang nutup sendiri kalau udah berhasil - ini yang ditunggu-tunggu
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
            // Ga usah reload paksa, biar Firebase yang ngasih tau halaman utama kalau udah login
            // Kalau 2 detik masih belum login, baru reload
            webView.postDelayed({
                // Cek masih di halaman login atau engga, kalau masih, reload sekali
                if (webView.url?.contains("dashboard") == true || webView.url?.contains("pemilik") == true) {
                    // Biarin, halaman utama lagi proses login
                }
            }, 2000)
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
