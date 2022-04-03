package com.onionchat.dr0id.ui

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface IComposeAbleActivity {
    fun getActivityResultLauncher() : ActivityResultLauncher<Intent>
}