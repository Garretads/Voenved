package ru.voenvedapp.voenved.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import ru.voenvedapp.voenved.R
import ru.voenvedapp.voenved.Settings
import ru.voenvedapp.voenved.Settings.FILECHOOSER_REQUEST_CODE
import ru.voenvedapp.voenved.databinding.ActivityMainBinding
import ru.voenvedapp.voenved.viewmodels.main.MainActivityViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    val TAG = MainActivity::class.java.simpleName

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var binding: ActivityMainBinding

    private var doubleBackToExitPressedOnce = false

    // Переменные для работы с данными file интентов

    // data/header received after file selection
    private var file_data: ValueCallback<Uri>? = null
    // received file(s) temp. location
    private var file_path: ValueCallback<Array<Uri>>? = null
    private var cam_file_data: String? = null // for storing camera file information
    private val file_type = "image/*" // file types to be allowed for upload
    private val multiple_files = true // allowing multiple file upload

    // -----------------------------------------------------

    private val webViewClient by lazy { object : WebViewClient() {
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

    private val webChromeClient by lazy { object : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            return if (requestFilePermission() && Build.VERSION.SDK_INT >= 21) {
                file_path = filePathCallback
                var takePictureIntent: Intent? = null
                var includePhoto = false
                /*-- checking the accept parameter to determine which intent(s) to include --*/paramCheck@ for (acceptTypes in fileChooserParams!!.acceptTypes) {
                    val splitTypes = acceptTypes.split(", ?+")
                        .toTypedArray() // although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values
                    for (acceptType in splitTypes) {
                        when (acceptType) {
                            "*/*" -> {
                                includePhoto = true
                                break@paramCheck
                            }
                            "image/*" -> includePhoto = true
                        }
                    }
                }
                if (fileChooserParams.acceptTypes.isEmpty()) { //no `accept` parameter was specified, allow both photo and video
                    includePhoto = true
                }
                if (includePhoto) {
                    takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                        var photoFile: File? = null
                        try {
                            photoFile = createImage()
                            takePictureIntent.putExtra("PhotoPath", cam_file_data)
                        } catch (ex: IOException) {
                            Log.e(TAG, "Image file creation failed", ex)
                        }
                        if (photoFile != null) {
                            cam_file_data = "file:" + photoFile.absolutePath
                            takePictureIntent.putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile)
                            )
                        } else {
                            cam_file_data = null
                            takePictureIntent = null
                        }
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = file_type

                if (multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }


                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser")

                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, takePictureIntent)
                startActivityForResult(chooserIntent, FILECHOOSER_REQUEST_CODE)
                true
            } else {
                false
            }
        }
    }}

    private var mUploadMessage: ValueCallback<*>? = null



    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        binding.mainWebView.webViewClient = webViewClient
        binding.mainWebView.webChromeClient = webChromeClient
        binding.mainWebView.settings.javaScriptEnabled = true
        binding.mainWebView.settings.allowUniversalAccessFromFileURLs = true
        binding.mainWebView.settings.allowFileAccessFromFileURLs = true
        binding.mainWebView.settings.userAgentString = Settings.CUSTOM_UA
        binding.mainWebView.settings.setAppCacheEnabled(false);
        binding.mainWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE;

        binding.mainWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        binding.mainWebView.loadUrl(Settings.MAIN_URL)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 21) {
            var results: Array<Uri>? = null
            /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/if (resultCode == Activity.RESULT_CANCELED) {
                if (requestCode == FILECHOOSER_REQUEST_CODE) {
                    file_path?.onReceiveValue(null)
                    return
                }
            }
            /*-- continue if response is positive --*/
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FILECHOOSER_REQUEST_CODE) {
                    if (null == file_path) {
                        return
                    }
                    var clipData: ClipData?
                    var stringData: String?
                    try {
                        clipData = intent!!.clipData
                        stringData = intent.dataString
                    } catch (e: Exception) {
                        clipData = null
                        stringData = null
                    }
                    if (clipData == null && stringData == null && cam_file_data != null) {
                        results = arrayOf(Uri.parse(cam_file_data))
                    } else {
                        if (clipData != null) { // checking if multiple files selected or not
                            val numSelectedFiles = clipData.itemCount
                            results = Array(numSelectedFiles) { Uri.EMPTY }
                            for (i in 0 until clipData.itemCount) {
                                results[i] = clipData.getItemAt(i).uri
                            }
                        } else {
                            results = arrayOf(Uri.parse(stringData))
                        }
                    }
                }
            }
            results?.let {
                file_path?.onReceiveValue(it)
            }

            file_path = null
        } else {
            if (requestCode == FILECHOOSER_REQUEST_CODE) {
                if (null == file_data) return
                val result =
                    if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                file_data?.onReceiveValue(result)
                file_data = null
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    /*-- checking and asking for required file permissions --*/
    fun requestFilePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ),
                1
            )
            false
        } else {
            true
        }
    }

    /*-- creating new image file here --*/
    @Throws(IOException::class)
    private fun createImage(): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
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
