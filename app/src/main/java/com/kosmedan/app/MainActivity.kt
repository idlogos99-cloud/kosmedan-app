package com.kosmedan.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KosMedan)
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportMultipleWindows(true)

        // Handle link Google / OAuth -> buka di Chrome, bukan WebView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false

                // 1. Kalau URL Google Login / OAuth, lempar ke browser luar
                if (isGoogleAuthUrl(url)) {
                    openInBrowser(url)
                    return true
                }

                // 2. Kalau intent / WA / tel / mail
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

        // Handle popup window (Google biasanya buka popup)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val newWebView = WebView(this@MainActivity)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url != null && isGoogleAuthUrl(url)) {
                            openInBrowser(url)
                            return true
                        }
                        if (url != null) {
                            webView.loadUrl(url)
                        }
                        return true
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }
        }

        // Kalau balik dari Chrome setelah login, load URL-nya
        val data: Uri? = intent?.data
        if (data != null && data.toString().contains("kosmedan.my.id")) {
            webView.loadUrl(data.toString())
        } else {
            webView.loadUrl("https://kosmedan.my.id")
        }
    }

    private fun isGoogleAuthUrl(url: String): Boolean {
        return url.contains("accounts.google.com") ||
            url.contains("accounts.google.") ||
            url.contains("oauth") ||
            url.contains("google.com/o/oauth2") ||
            url.contains("firebaseapp.com/__/auth") ||
            url.contains("google.com/signin") ||
            url.contains("myaccount.google.com")
    }

    private fun openInBrowser(url: String) {
        try {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.launchUrl(this, Uri.parse(url))
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
