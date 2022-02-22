package com.onionchat.common

import android.content.Context
import androidx.preference.PreferenceManager

object SettingsManager {

    @JvmStatic
    fun getBooleanSetting(key: String, context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isChecked: Boolean = sharedPreferences.getBoolean(key, true)
        return isChecked
    }

    @JvmStatic
    fun setBooleanSetting(key: String, value: Boolean, context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = sharedPreferences.edit()
        edit.putBoolean(key, value)
        edit.apply()
    }
}