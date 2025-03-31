package com.example.aiagent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

class WebBrowser(private val context: Context) {
    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    @Composable
    fun WebViewComponent(url: String) {
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        })
    }
}
