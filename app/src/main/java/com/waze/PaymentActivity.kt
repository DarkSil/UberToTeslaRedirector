package com.waze

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.waze.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPaymentBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.root.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
        binding.root.settings.allowFileAccess = true
        binding.root.settings.javaScriptEnabled = true
        binding.root.settings.allowContentAccess = true
        binding.root.settings.cacheMode = WebSettings.LOAD_DEFAULT
        binding.root.settings.databaseEnabled = true
        binding.root.settings.domStorageEnabled = true
        binding.root.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.root.loadUrl("https://www.youtube.com")

        // TODO Insert needed url
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onSuccess()
    }

    private fun onSuccess() {
        val intent = Intent(this, FreeMapAppActivity::class.java)
        intent.action = this.intent.action
        intent.data = this.intent.data
        startActivity(intent)
        finish()
    }

}