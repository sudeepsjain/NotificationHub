package com.notificationhub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create WebView programmatically
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true // Enable JS for link functionality
        webView.settings.allowFileAccess = true
        webView.settings.domStorageEnabled = false
        webView.settings.setSupportMultipleWindows(true)
        
        // Set WebViewClient to handle link clicks
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                return if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
                    // Open external links in browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where no browser is available
                        e.printStackTrace()
                    }
                    true
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
            
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                } else {
                    false
                }
            }
        }
        
        // Load the privacy policy HTML from assets
        webView.loadUrl("file:///android_asset/privacy_policy.html")
        
        setContentView(webView)
        
        // Set up action bar
        supportActionBar?.apply {
            title = "Privacy Policy"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}