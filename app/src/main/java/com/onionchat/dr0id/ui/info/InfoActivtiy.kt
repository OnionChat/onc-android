package com.onionchat.dr0id.ui.info

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor

class InfoActivtiy : AppCompatActivity(), OnionTaskProcessor.OnionTaskProcessorObserver {
    var statsTextView : TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_activtiy)
        val apkUrlWidget = findViewById<EditText>(R.id.info_activity_apk_url)
        statsTextView = findViewById(R.id.info_activity_stats_textview)
        apkUrlWidget?.setText(buildApkUrl(this))
        updateStatsView()
    }

    fun updateStatsView() {
        var str = ""
        OnionTaskProcessor.getStats().forEachIndexed {index, i ->
            str += "\n\n\n\nEXECUTOR ${index}\n\n"
            i.forEach {
                str += "\t${it.key}: ${it.value}\n"
            }
        }
        statsTextView?.text = str
    }

    override fun onResume() {
        super.onResume()
        OnionTaskProcessor.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        OnionTaskProcessor.removeObserver(this)

    }

    fun buildApkUrl(context: Context) : String {
        val hostname = ConnectionManager.getHostName(context)
        return hostname + ".ws/onionchat.apk"
    }

    override fun onTaskEnqueued(task: Any) {
        runOnUiThread {
            updateStatsView()
        }
    }

    override fun onTaskFinished(task: Any, result: OnionTask.Result) {
        runOnUiThread {
            updateStatsView()
        }
    }
}