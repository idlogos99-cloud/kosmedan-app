package com.kosmedan.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KosMedan)
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false

                // Kalau ini awal alur Firebase Auth, lempar SEKALIGUS ke Chrome Full
                // Jangan dipotong di tengah, harus dari awal di Chrome
                if (isAuthUrl(url)) {
                    openInFullChrome(url)
                    return true
                }

                if (url.startsWith("intent://") || url.startsWith("whatsapp://") || url.startsWith("tel:") || url.startsWith("mailto:")) {
                    try {
                        val intent = if (url.startsWith("intent://")) {
                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6285182445887?text=Halo%20Alif")))
                        } catch (ex: Exception) {}
                    }
                    return true
                }
                view?.loadUrl(url)
                return true
            }
        }

        val data: Uri? = intent?.data
        if (data != null && data.toString().contains("kosmedan.my.id")) {
            webView.loadUrl(data.toString())
        } else {
            webView.loadUrl("https://kosmedan.my.id")
        }
    }

    private fun isAuthUrl(url: String): Boolean {
        return url.contains("accounts.google.com") ||
            url.contains("firebaseapp.com/__/auth") ||
            url.contains("kosmedan.my.id/__/auth") ||
            url.contains("__/auth/handler") ||
            url.contains("google.com/o/oauth2") ||
            url.contains("oauth")
    }

    private fun openInFullChrome(url: String) {
        try {
            // PENTING: Pake ACTION_VIEW biasa, JANGAN CustomTabsIntent
            // Biar sessionStorage Firebase ga kepotong
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setPackage("com.android.chrome")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (ex: Exception) {}
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.data
        if (data != null && data.toString().contains("kosmedan.my.id")) {
            webView.loadUrl(data.toString())
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
