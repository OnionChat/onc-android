package com.onionchat.dr0id.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.onionchat.dr0id.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settigns_screen, rootKey)
    }
}
