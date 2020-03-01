package ru.voenvedapp.voenved.ui.main

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import ru.voenvedapp.voenved.R
import ru.voenvedapp.voenved.Settings
import ru.voenvedapp.voenved.databinding.ActivityMainBinding
import ru.voenvedapp.voenved.viewmodels.main.MainActivityViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var binding: ActivityMainBinding

    private var doubleBackToExitPressedOnce = false

    private val mWebViewClient by lazy { object : WebViewClient() {
        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            view.loadUrl(request.url.toString())
            return true
        }

        // Для старых устройств
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }


    }}

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        binding.mainWebView.webViewClient = mWebViewClient
        binding.mainWebView.settings.javaScriptEnabled = true
        binding.mainWebView.settings.allowUniversalAccessFromFileURLs = true
        binding.mainWebView.settings.allowFileAccessFromFileURLs = true
        binding.mainWebView.settings.userAgentString = Settings.CUSTOM_UA

        binding.mainWebView.loadUrl(Settings.MAIN_URL)
    }

    override fun onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        if (binding.mainWebView.canGoBack()) {
            binding.mainWebView.goBack()
        }

        this.doubleBackToExitPressedOnce = true
        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)

    }

}
