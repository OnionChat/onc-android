package com.onionchat.dr0id.ui.contactdetails

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import com.onionchat.common.IDGenerator
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.users.BroadcastManager
import com.onionchat.dr0id.users.UserManager
import com.onionchat.localstorage.userstore.Conversation


class ContactDetailsActivity : OnionChatActivity() {

    companion object {
        val USER_DELETED: Int = 101
        val TAG = ContactDetailsActivity::class.java.simpleName

        val EXTRA_CONTACT_ID = "contact_id"
        val EXTRA_BROADCAST_ID = "broadcast_id"
    }

    lateinit var conversation: Conversation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_details)
        setSupportActionBar(findViewById(R.id.toolbar))

        intent.getStringExtra(EXTRA_CONTACT_ID)?.let { uid ->
            setTitle(IDGenerator.toVisibleId(uid))
            findViewById<ImageView>(R.id.activity_contact_details_avatar).let { imageView ->
                Conversation.getRepresentativeProfileBitmap(IDGenerator.toVisibleId(uid))?.let {
                    imageView.setImageBitmap(it)
                }
            }
            UserManager.getUserById(uid)?.let {
                val u = it.get()
                if (u == null) {
                    return@onCreate
                }
                conversation = Conversation(u)
            }
        }
        intent.getStringExtra(EXTRA_BROADCAST_ID)?.let {
            val broadcast = BroadcastManager.getBroadcastById(it).get()
            broadcast?.let {
                setTitle(it.label)
                findViewById<ImageView>(R.id.activity_contact_details_avatar)
                    .setImageBitmap(Conversation.getRepresentativeProfileBitmap(it.id))
                conversation = Conversation(null, broadcast = it)
            }
        }

        findViewById<ImageButton>(R.id.activity_contact_details_open_webbutton).let {
            it.setOnClickListener {
                conversation.user?.let {
                    openContactWebSpace(it)
                }
            }
        }
        findViewById<ImageButton>(R.id.activity_contact_details_block).let {

        }
        findViewById<ImageButton>(R.id.activity_contact_details_delete_button).let {
            it.setOnClickListener {
                showReallyDeleteUserDialog(conversation.getLabel()) {
                    if (it) {
                        conversation.user?.let { user ->
                            UserManager.removeUser(user)
                        } ?: run {
                            conversation.broadcast?.let {
                                BroadcastManager.removeBroadcast(it)
                            }
                        }
                        val i = Intent()
                        i.putExtra(EXTRA_CONTACT_ID, conversation.getId())
                        setResult(USER_DELETED, i)
                        finish()
                    }
                }
            }
        }
    }

    override fun onConnected(success: Boolean) {
    }

    fun showReallyDeleteUserDialog(label:String, callback: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.really_delete_message) + "\n\n" + label).setTitle(R.string.really_delete_title)
            .setCancelable(false)
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                callback(true)
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialog, id -> //  Action for 'NO' Button
                callback(false)
            })
        //Creating dialog box
        //Creating dialog box
        val alert: AlertDialog = builder.create()
        //Setting the title manually
        //Setting the title manually
        alert.show()
    }


    fun delete() {
//        UserManager.removeUser(conversations[position].user)
//        conversations.removeAt(position)
    }
}