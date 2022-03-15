package com.onionchat.dr0id.ui.broadcast

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.BroadcastManager
import com.onionchat.dr0id.databinding.ActivityCreateBroadCastBinding
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.localstorage.userstore.User

class CreateBroadCastActivity : AppCompatActivity(), IBroadcastCreateListener {

    companion object {
        val BROADCAST_CREATED: Int = 201
        val TAG = ContactDetailsActivity::class.java.simpleName

        val EXTRA_BROADCAST_ID = "broadcast_id"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityCreateBroadCastBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateBroadCastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_create_broad_cast)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_create_broad_cast)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    var broadcastUsers: List<User> = ArrayList<User>()
    var broadcastLabel = ""
    override fun onLabelChoosen(label: String) {
        broadcastLabel = label
    }

    override fun onUsersSelected(users: List<User>) {
        broadcastUsers = users
    }

    override fun onCreateBroadcast() {
        Thread {
            BroadcastManager.createBroadcast(broadcastLabel).get()?.let {
                BroadcastManager.addUsersToBroadcast(it, broadcastUsers)
                runOnUiThread {
                    val i = Intent()
                    i.putExtra(EXTRA_BROADCAST_ID, it.id)
                    setResult(BROADCAST_CREATED, i)
                    finish()
                }
            }

        }.start()

    }
}