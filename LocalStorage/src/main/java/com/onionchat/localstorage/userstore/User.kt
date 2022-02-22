package com.onionchat.localstorage.userstore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.onionchat.common.IDGenerator

@Entity
data class User(@PrimaryKey val id: String, @ColumnInfo(name = "cert_id") val certId: String) {

    fun getName(): String {
        return IDGenerator.toVisibleId(id)
    }
}