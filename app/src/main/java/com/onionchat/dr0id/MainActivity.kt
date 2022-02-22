package com.onionchat.dr0id

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.common.SettingsManager
import com.onionchat.connector.BackendConnector
import com.onionchat.dr0id.service.OnionChatConnectionService
import com.onionchat.dr0id.ui.contactlist.ContactListWindow
import com.onionchat.dr0id.ui.onboarding.OnBoardingActivtiy
import com.onionchat.dr0id.users.UserManager

class MainActivity : OnionChatActivity() {

    companion object {
        val TASK = "task"
        val TASK_RECONNECTED = "reconnect"
    }

    lateinit var textView: TextView
    var task: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        task = intent.extras?.getString(TASK); // TODO accses via intent ?
        Logging.d("MainActivity", "onCreate [+] task=$task")
        textView = findViewById(R.id.loading_screen_status)
        textView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                //connect()
            }
        })
        checkOnBoarding()
    }

    override fun onResume() {
        super.onResume()
        checkCrypto()
    }

    fun checkOnBoarding() {
        val doOnboarding = SettingsManager.getBooleanSetting(getString(R.string.key_onboarding), this)
//        val doOnboarding = true
        if (doOnboarding) {
            startActivity(Intent(this, OnBoardingActivtiy::class.java))
            finish()
        } else {
            updateText("Connecting...")
            startForegroundService(Intent(this, OnionChatConnectionService::class.java));
        }
    }

    fun checkCrypto() {
        Logging.d("MainActivity", "check crypto")
        if (Crypto.getMyPublicKey() == null) {
            Logging.d("MainActivity", "init new crypto")
            updateText("Generate keys...")
            Crypto.generateKey()
            if (Crypto.getMyPublicKey() == null) {
                updateText("Error while init crypto... you're f** up")
                Logging.e("MainActivity", "Error while init crypto... you're f** up")
                return
            }
        }

        updateText("Connecting...")
        //connect()
    }

    override fun onConnected(success: Boolean) {
        if (success) {
            UserManager.myId = BackendConnector.getConnector().getHostName(this)

            if (task == null || !task.equals(TASK_RECONNECTED)) {
                Logging.d("MainActivity", "Going to start contactlist")
                val intent = Intent(this@MainActivity, ContactListWindow::class.java)
                startActivity(intent)
            }
            finish()
            //task = "reconnect"
        } else {
            updateText("Unable to connect.. tap to retry")
        }
    }

    fun updateText(str: String) {
        runOnUiThread {
            textView.setText(str)
        }
    }
}