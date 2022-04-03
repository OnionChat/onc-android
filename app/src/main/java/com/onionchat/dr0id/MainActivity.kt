package com.onionchat.dr0id

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.common.SettingsManager
import com.onionchat.dr0id.broadcast.AlarmReceiver
import com.onionchat.dr0id.broadcast.AlarmReceiver.Companion.alarmQuestion
import com.onionchat.dr0id.broadcast.AlarmReceiver.Companion.scheduleAlarmReceiver
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.service.OnionChatConnectionService
import com.onionchat.dr0id.ui.onboarding.OnBoardingActivtiy
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import com.onionchat.dr0id.ui.conversationList.ConversationListWindow
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError

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
        if(checkCrypto()) {
            checkOnBoarding()
        }
//        AlarmReceiver.powerSafeQuestion(this)
//        alarmQuestion(this)
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
            updateText("Starting...")
            scheduleAlarmReceiver(this) // todo right place?
            startService(Intent(this, OnionChatConnectionService::class.java))

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                //startForegroundService(Intent(this, OnionChatConnectionService::class.java))
//                scheduleBackgroundService(this) // todo right place?
//            } else {
//                startService(Intent(this, OnionChatConnectionService::class.java))
//            }
            launchContactList()
        }
    }

    fun checkCrypto() : Boolean {
        Logging.d("MainActivity", "check crypto")


        if (Crypto.getMyPublicKey() == null ) {
            Logging.d("MainActivity", "init new crypto")
            updateText("Generate keys...")
            Crypto.generateKey()
            if (Crypto.getMyPublicKey() == null) {
                updateText("Error while init crypto... you're f** up")
                Logging.e("MainActivity", "Error while init crypto... you're f** up")
                return false
            }
        } else {
            try{
                Crypto.getMyKey()
            }catch (exception:Exception) {
                showError(this, getString(R.string.crypto_error), ErrorViewer.ErrorCode.INVALID_PRIVATE_KEY)
                updateText(getString(R.string.crypto_error)+ " ($exception)")
                return false
            }
        }
        return true

        //updateText("Connecting...")
        //connect()
    }

    override fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult) {
//        if (success) {
//
//            //task = "reconnect"
//        } else {
//            updateText("Unable to connect.. tap to retry")
//        }
    }

    fun launchContactList() {
        UserManager.myId = ConnectionManager.getHostName(this)

        if (task == null || !task.equals(TASK_RECONNECTED)) {
            Logging.d(TAG, "launchContactList [+] Going to start contactlist")
            val intent = Intent(this@MainActivity, ConversationListWindow::class.java)
            startActivity(intent)
        }
        finish()
    }

    fun updateText(str: String) {
        runOnUiThread {
            textView.setText(str)
        }
    }
}