package com.onionchat.dr0id.ui.adduser

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.onionchat.common.AddUserPayload
import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.keyexchange.ResponsePubMessage
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask

class UserAddConfirmationActivity : OnionChatActivity() {

    companion object {
        const val PAYLOAD_QR = "payload_qr"

        const val RESULT_USER_ADDED = 1200

        const val RESULT_EXTRA_UID = "uid"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_add_confirmation)

        val payload = intent.extras?.getString(PAYLOAD_QR)
        payload?.let {
            AddUserPayload.decode(it)?.let { adduserPayload ->
                findViewById<TextView>(R.id.user_add_confirmation_user_label)?.text = adduserPayload.label
                findViewById<TextView>(R.id.user_add_confirmation_user_id)?.text = IDGenerator.toHashedId(adduserPayload.uid)
                findViewById<ImageView>(R.id.user_add_confirmation__avatar)?.let { imagiteView ->
                    Conversation.getRepresentativeProfileBitmap(IDGenerator.toHashedId(adduserPayload.uid))?.let {
                        imagiteView.setImageBitmap(it)
                    }
                }
                findViewById<ImageButton>(R.id.user_add_confirmation_button_positive)?.setOnClickListener {
                    val intent = Intent()
                    intent.putExtra(RESULT_EXTRA_UID, adduserPayload.uid)
                    val partner = UserManager.addUser(adduserPayload)
                    setResult(RESULT_USER_ADDED, intent)
                    UserManager.myId?.let {
                        val pub = Crypto.getMyPublicKey()
                        if(pub == null) {
                            showError()
                            return@setOnClickListener
                        }

                        val label = UserManager.getMyLabel(this@UserAddConfirmationActivity)?:""

                        sendMessage(
                            ResponsePubMessage(
                                IDGenerator.toHashedId(it),
                                partner.getHashedId(),
                                addUserPayload = AddUserPayload(
                                    it,
                                    pub.encoded,
                                    label
                                )
                            ), it, partner
                        )?.then {
                            if (it.status == OnionTask.Status.FAILURE) {
                                runOnUiThread {
                                    Toast.makeText(this, getString(R.string.unable_to_send_response), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    finish()
                }

                findViewById<ImageButton>(R.id.user_add_confirmation_button_negative)?.setOnClickListener {
                    finish()
                }
            }
        }
    }

    fun showError() {
        runOnUiThread {
            Toast.makeText(this, getString(R.string.unable_to_send_response), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult) {
    }
}