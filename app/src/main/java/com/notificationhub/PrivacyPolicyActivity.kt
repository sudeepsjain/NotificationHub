package com.notificationhub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
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
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.startsWith("https://teckgrow.com")) {
                        // Open external links in browser
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                        startActivity(intent)
                        return true
                    }
                }
                return false
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