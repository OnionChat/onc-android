package com.onionchat.dr0id.ui

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface IAddUserAbleActivity {

    fun getActivityResultLauncher() : ActivityResultLauncher<Intent>
    fun getUserScanRequestCode() : Int
}