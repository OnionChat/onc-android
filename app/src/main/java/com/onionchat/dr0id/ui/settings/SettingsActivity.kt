package com.onionchat.dr0id.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.onionchat.dr0id.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_activity_fragment_container, SettingsFragment())
            .commit()
    }
}
