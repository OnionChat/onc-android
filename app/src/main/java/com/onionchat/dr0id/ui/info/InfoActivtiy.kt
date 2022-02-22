package com.onionchat.dr0id.ui.info

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.onionchat.connector.BackendConnector
import com.onionchat.dr0id.R

class InfoActivtiy : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_activtiy)
        val apkUrlWidget = findViewById<EditText>(R.id.info_activity_apk_url)
        apkUrlWidget?.setText(buildApkUrl(this))
    }

    fun buildApkUrl(context: Context) : String {
        val hostname = BackendConnector.getConnector().getHostName(context)
        return hostname + ".ws/onionchat.apk"
    }
}