package ru.voenvedapp.voenved.viewmodels.main

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.*
import androidx.lifecycle.ViewModel
import ru.voenvedapp.voenved.Settings
import java.io.File
import java.io.IOException

class MainActivityViewModel : ViewModel() {

    var listener: WebViewCallbacksListener? = null

    val webViewClient by lazy { object : WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            listener?.onPageFinished(view,url)
        }

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

    val webChromeClient by lazy { object : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            return listener?.onShowFileChooser(webView, filePathCallback, fileChooserParams) ?: false
        }
    }}

    interface WebViewCallbacksListener {
        fun onPageFinished(view: WebView?, url: String?)
        fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean
    }

}
