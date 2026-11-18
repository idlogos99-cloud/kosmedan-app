package com.kosmedan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Kalo link WhatsApp / intent / telpon, lempar ke aplikasi luar
                if (url.startsWith("intent://") || url.startsWith("whatsapp://") || url.startsWith("tel:") || url.startsWith("mailto:")) {
                    try {
                        Intent intent;
                        if (url.startsWith("intent://")) {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            // Kalo WA belum ke-install, buka via browser wa.me
                            if (intent.getData() != null && intent.getData().toString().contains("send")) {
                                // ambil nomornya, fallback ke wa.me
                            }
                        } else {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        }
                        startActivity(intent);
                    } catch (Exception e) {
                        // Fallback paling aman: buka wa.me
                        try {
                            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6285182445887?text=Halo%20Alif"));
                            startActivity(fallback);
                        } catch (Exception ex) {}
                    }
                    return true;
                }
                // Kalo link web biasa, buka di dalam aplikasi
                view.loadUrl(url);
                return true;
            }
        });

        webView.loadUrl("https://kosmedan.my.id");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
