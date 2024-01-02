package com.waze

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.waze.databinding.ActivityPaymentBinding

class RoutesActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPaymentBinding.inflate(layoutInflater) }
    private val id by lazy { Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val uri = Uri.parse(url)
                if (uri.path.toString().replace("/", "") == "paymentReceived.php") {
                    binding.webView.isVisible = false
                    binding.progressBar.isVisible = true
                    if (uri.getQueryParameter("userId") == id) {
                        onSuccess()
                    } else {
                        var customUrl = url?.replace("http://", "https://")
                        customUrl += "?userId=$id"
                        view?.loadUrl(customUrl.toString())
                    }
                }
                super.onPageFinished(view, url)
            }
        }
        binding.webView.settings.allowFileAccess = true
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.allowContentAccess = true
        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        binding.webView.settings.databaseEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.webView.loadUrl("https://www.teslagpsconnection.space?payment=true")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onSuccess()
    }

    private fun onSuccess() {
        val intent = Intent(this, FreeMapAppActivity::class.java)
        intent.action = this.intent.action
        intent.data = this.intent.data
        intent.putExtra("reload", true)
        startActivity(intent)
        finish()
    }

}