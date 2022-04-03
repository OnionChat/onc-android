package com.onionchat.dr0id.ui.feed

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.ui.chat.ChatAdapter
import com.onionchat.dr0id.ui.web.OnionWebClient
import com.onionchat.localstorage.userstore.User

class FeedWebViewHolder(viewGroup: ViewGroup, clickListener: MessageAdapter.ItemClickListener?) :
    FeedViewHolder(R.layout.feed_fragment_item_web, viewGroup, clickListener) {
    var webView: WebView? = null

    init {
        webView = baseView.findViewById(R.id.feed_fragment_item_web_webview)
    }

    override fun bind(message: IMessage, user: User?): Boolean {
        if (message !is ITextMessage) {
            Logging.e(FeedTextViewHolder.TAG, "bind [-] invalid message type <${message}>")
            return false
        }
        val webView = webView
        if (webView == null) {
            Logging.e(FeedTextViewHolder.TAG, "bind [-] ui initialization error [-] webView is null")
            return false
        }
        webView.webViewClient = OnionWebClient(viewGroup.context)
        webView.webChromeClient = WebChromeClient()
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.settings.javaScriptEnabled = true
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.loadUrl(message.getText().text)
        return true
    }
}