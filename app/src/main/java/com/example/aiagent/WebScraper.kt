package com.example.aiagent

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WebScraper(context: Context) {
    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
    }

    suspend fun scrape(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(
                        "(function() { return document.body.innerText; })();"
                    ) { result ->
                        continuation.resume(result?.removeSurrounding("\"") ?: "")
                    }
                }
            }
            webView.loadUrl(url)
        }
    }

    suspend fun extractLinks(url: String): List<String> {
        return suspendCancellableCoroutine { continuation ->
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(
                        """
                        (function() {
                            const links = Array.from(document.getElementsByTagName('a'))
                                .map(a => a.href);
                            return JSON.stringify(links);
                        })();
                        """
                    ) { result ->
                        val links = try {
                            JSONArray(result).toList().filterIsInstance<String>()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        continuation.resume(links)
                    }
                }
            }
            webView.loadUrl(url)
        }
    }

    fun close() {
        webView.destroy()
    }
}
