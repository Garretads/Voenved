package ru.voenvedapp.voenved.viewmodels.main

import android.annotation.TargetApi
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import ru.voenvedapp.voenved.Settings
import java.io.File
import java.io.IOException

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    var listener: WebViewCallbacksListener? = null

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
