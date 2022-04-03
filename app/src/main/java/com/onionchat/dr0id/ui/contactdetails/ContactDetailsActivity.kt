package com.onionchat.dr0id.ui.contactdetails

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.BroadcastManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.NegotiateSymKeyTask
import com.onionchat.dr0id.ui.contactlist.ContactListAdapter
import com.onionchat.localstorage.EncryptedLocalStorage
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError

// todo refactore
class ContactDetailsActivity : OnionChatActivity(), ContactListAdapter.ItemClickListener {

    companion object {
        val USER_DELETED: Int = 101
        val TAG = ContactDetailsActivity::class.java.simpleName

        val EXTRA_CONTACT_ID = "contact_id"
        val EXTRA_BROADCAST_ID = "broadcast_id"
    }

    var conversation: Conversation? = null
    var adapter: ContactListAdapter? = null
    var conversations: ArrayList<Conversation> = ArrayList();
    lateinit var recyclerView: RecyclerView
    var keyInfoTextView :TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_details)
        setSupportActionBar(findViewById(R.id.toolbar))
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.contact_details_receivers_selection)
        recyclerView.setLayoutManager(LinearLayoutManager(this));
        adapter = ContactListAdapter(conversations, true)
        adapter?.setClickListener(this)
        //adapter?.setClickListener(this)
        recyclerView.adapter = adapter

        intent.getStringExtra(EXTRA_CONTACT_ID)?.let { uid ->
            setTitle(IDGenerator.toHashedId(uid))

            findViewById<ImageButton>(R.id.activity_contact_details_open_webbutton).let {
                it.setOnClickListener {
                    Logging.d(TAG, "onCreate [+] open contact details ${uid}")
                    openContactWebSpace(uid)
                }
            }

            findViewById<ImageView>(R.id.activity_contact_details_avatar).let { imageView ->
                Conversation.getRepresentativeProfileBitmap(IDGenerator.toHashedId(uid))?.let {
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
            findViewById<TextView>(R.id.activity_contact_details_user_label)?.let {
                UserManager.myId?.let { myId ->
                    if (uid == myId) {
                        Crypto.getMyPublicKey()?.let { cert ->
                            EncryptedLocalStorage(cert, Crypto.getMyKey(), this@ContactDetailsActivity).let { storage ->
                                it.text = storage.getValue(getString(R.string.key_user_label))
                            }
                        } ?: run {
                            it.text = "ERROR"
                        }

                    }
                } ?: kotlin.run {
                    it.text = "ERROR"
                }
            }

            keyInfoTextView = findViewById(R.id.activity_contact_details_key_info)
            keyInfoTextView?.let { textView ->
                conversation?.user?.let { user ->
                    user.getLastAlias()?.let {
                        textView.text = IDGenerator.toHashedId(it.id)
                    }

                }

            }

        }
        intent.getStringExtra(EXTRA_BROADCAST_ID)?.let {
            val broadcast = BroadcastManager.getBroadcastById(it).get()
            broadcast?.let {
                setTitle(it.label)
                findViewById<ImageView>(R.id.activity_contact_details_avatar)
                    .setImageBitmap(Conversation.getRepresentativeProfileBitmap(it.id))
                conversation = Conversation(null, broadcast = it)
                val localConversations = HashMap<String, Conversation>()
                BroadcastManager.getBroadcastUsers(it).get().forEach {
                    localConversations.put(it.id, Conversation(it, selected = true))
                }
                UserManager.getAllUsers().get().forEach {
                    localConversations.putIfAbsent(it.id, Conversation(it))
                }
                conversations.addAll(localConversations.values)
            }
        }

        findViewById<ImageButton>(R.id.activity_contact_details_block).let {

        }
        val conversation = conversation
        if(conversation == null) {
            return
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

    override fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult) {
    }

    fun showReallyDeleteUserDialog(label: String, callback: (Boolean) -> Unit) {
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

    override fun onItemClick(view: View?, position: Int) {
    }

    override fun onItemLongClick(view: View?, position: Int) {
    }

    override fun onCheckedChangeListener(position: Int) {
        conversation?.broadcast?.let {

            if (conversations[position].selected) {
                BroadcastManager.addUsersToBroadcast(it, arrayListOf(conversations[position].user!!))

            } else {
                BroadcastManager.deleteUsersFromBroadcast(it, arrayOf(conversations[position].user!!.id))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_details_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        when (id) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.open_stats -> {
                conversation?.user?.let {
                    openStatsActivity(it)
                }
            }

            R.id.refresh_keys -> {
                conversation?.user?.let {
                    Logging.d(TAG, "enqueue negotiation task")
                    OnionTaskProcessor.enqueue(NegotiateSymKeyTask(it)).then { result ->
                        Logging.d(TAG, "finished negotiation task $result")
                        if (result.status == OnionTask.Status.SUCCESS) {
                            runOnUiThread {
                                result.newAlias?.let {
                                    keyInfoTextView?.text = IDGenerator.toHashedId(it.id)
                                }
                            }
                        } else {
                            showError(this, getString(R.string.error_key_negotiation_failed), ErrorViewer.ErrorCode.KEY_NEGOTIATION_FAILED)
                        }
                    }
                }
            }

            else -> {
            }
        }
        return true
    }
}