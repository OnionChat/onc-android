package com.onionchat.dr0id.ui.web

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import android.widget.VideoView

import android.widget.FrameLayout





class OnionWebActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        val EXTRA_URL = "onion_web_activity_url"

        @JvmStatic
        val EXTRA_USERNAME = "onion_web_activity_username"

        @JvmStatic
        val TAG = "OnionWebActivity"
    }

    private val chromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onShowCustomView(view: View, callback: CustomViewCallback?) {
            // TODO Auto-generated method stub
            super.onShowCustomView(view, callback)
            if (view is FrameLayout) {
                val frame = view as FrameLayout
                if (frame.focusedChild is VideoView) {
                    val video = frame.focusedChild as VideoView
                    frame.removeView(video)
                    video.start()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onion_web)
        var url = intent.getStringExtra(EXTRA_URL)
        val username = intent.getStringExtra(EXTRA_USERNAME)
        username?.let {
            setTitle(it)
        }
        if (savedInstanceState == null) {
            val os_url = intent.dataString?.replace("onionchat://", "http://")
            if(url == null) {
                url = os_url
            }
            // do something with this URL.
        }
        url?.let {
            findViewById<WebView>(R.id.activity_onion_web_webview)?.let {


                val webViewClient = OnionWebClient()
                //webViewClient.setRequestCounterListener { requestCount -> runOnUiThread { statusTextView.text = "Request Count: $requestCount" } }
                it.webViewClient = webViewClient
                it.webChromeClient = WebChromeClient()
                it.getSettings().setDomStorageEnabled(true);
                it.settings.javaScriptEnabled = true

                Logging.d(TAG, "Going to open url <" + url + ">")
                it.loadUrl(url)
            }


        } ?: run {
            finish() // todo error handling
        }
    }
}