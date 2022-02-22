package com.onionchat.dr0id.ui.web

import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.onionchat.dr0id.R
import java.lang.Exception
import java.lang.reflect.Method


class MovableWebWindow(val view: View, private val context: Context, private val url: String) {

    fun setPopupWindowTouchModal(popupWindow: PopupWindow?, touchModal: Boolean) {
        if (null == popupWindow) {
            return
        }
        val method: Method
        try {
            method = PopupWindow::class.java.getDeclaredMethod("setTouchModal", Boolean::class.javaPrimitiveType)
            method.setAccessible(true)
            method.invoke(popupWindow, touchModal)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun show() {
        // inflate the layout of the popup window
        // inflate the layout of the popup window
        val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.webview_dialog, null)
        val webview = popupView.findViewById<WebView>(R.id.webview_dialog_webview)
        val close_button = popupView.findViewById<ImageButton>(R.id.webview_dialog_close_button)
        //val title = popupView.findViewById<TextView>(R.id.webview_dialog_drag)
        //title.setText(url)
        // create the popup window
        webview?.let {
            it.webViewClient = OnionWebClient()
            it.webChromeClient = WebChromeClient()
            it.getSettings().setDomStorageEnabled(true);
            it.settings.javaScriptEnabled = true

            it.loadUrl(url)
        }
        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it

        val popupWindow = PopupWindow(popupView, width, height, focusable)

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.TOP, 0, 0)
        popupWindow.setOutsideTouchable(false)
        //popupWindow.setFocusable(false)
        setPopupWindowTouchModal(popupWindow,false)
        popupWindow.setOnDismissListener {

        }
        popupWindow.update()
        // dismiss the popup window when touched

        // dismiss the popup window when touched

        close_button.setOnClickListener {
            popupWindow.dismiss()
        }
//        popupView.setOnTouchListener(object : View.OnTouchListener {
//            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                //popupWindow.dismiss()
//                return true
//            }
//        })

        /* make it movable?
        final View cv = new View(this);
    setContentView(cv);

    TextView tv = new TextView(this);
    tv.setBackgroundColor(0xffeeeeee);
    tv.setTextColor(0xff000000);
    tv.setTextSize(24);
    tv.setText("click me\nthen drag me");
    tv.setPadding(8, 8, 8, 8);
    mPopup = new PopupWindow(tv, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    OnTouchListener otl = new OnTouchListener() {
        private float mDx;
        private float mDy;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                mDx = mCurrentX - event.getRawX();
                mDy = mCurrentY - event.getRawY();
            } else
            if (action == MotionEvent.ACTION_MOVE) {
                mCurrentX = (int) (event.getRawX() + mDx);
                mCurrentY = (int) (event.getRawY() + mDy);
                mPopup.update(mCurrentX, mCurrentY, -1, -1);
            }
            return true;
        }
    };
    tv.setOnTouchListener(otl);

    mCurrentX = 20;
    mCurrentY = 50;
    cv.post(new Runnable() {
        @Override
        public void run() {
            mPopup.showAtLocation(cv, Gravity.NO_GRAVITY, mCurrentX, mCurrentY);
        }
    });
         */

    }

}
