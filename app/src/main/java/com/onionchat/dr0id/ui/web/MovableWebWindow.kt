package com.onionchat.dr0id.ui.web

import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import com.onionchat.dr0id.R
import java.lang.reflect.Method


class MovableWebWindow(val view: RelativeLayout, private val context: Context, private val url: String) {


    fun show() {
        // inflate the layout of the popup window
        // inflate the layout of the popup window
        val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.webview_dialog, null)
        val webview = popupView.findViewById<WebView>(R.id.webview_dialog_webview)
        val close_button = popupView.findViewById<ImageButton>(R.id.webview_dialog_close_button)
        val title = popupView.findViewById<TextView>(R.id.webview_dialog_drag)

        close_button?.setOnClickListener {
            view.removeView(popupView)
        }
        //title.setText(url)
        // create the popup window
        webview?.let {
            it.webViewClient = OnionWebClient(context)
            it.webChromeClient = WebChromeClient()
            it.getSettings().setDomStorageEnabled(true);
            it.getSettings().setMediaPlaybackRequiresUserGesture(false);
            it.settings.javaScriptEnabled = true
            it.getSettings().setAllowContentAccess(true);
            it.getSettings().setAllowFileAccess(true);
            it.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            it.getSettings().setPluginState(WebSettings.PluginState.ON);
            it.getSettings().setLoadsImagesAutomatically(true);
            it.getSettings().setMediaPlaybackRequiresUserGesture(false);
            it.loadUrl(url)
        }
        view.addView(popupView)
        /*
        MAKE THE DIALOG MOVABLE
         */
        var mDx: Float = 0.0.toFloat();
        var mDy: Float = 0.0.toFloat();
        var mCurrentX = 10
        var mCurrentY = 10
        popupView.x = mCurrentX.toFloat()
        popupView.y = mCurrentY.toFloat()
        val touchListener = View.OnTouchListener { v, event ->
            val action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                mDx = mCurrentX - event.getRawX();
                mDy = mCurrentY - event.getRawY();
            } else
                if (action == MotionEvent.ACTION_MOVE) {
                    mCurrentX = (event.getRawX() + mDx).toInt();
                    mCurrentY = (event.getRawY() + mDy).toInt();
                    popupView.x = mCurrentX.toFloat()
                    popupView.y = mCurrentY.toFloat()

                }
            true;
        };
        title.setOnTouchListener(touchListener);

        mCurrentX = 20;
        mCurrentY = 50;

    }

}
