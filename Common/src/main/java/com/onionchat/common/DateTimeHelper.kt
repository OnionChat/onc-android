package com.onionchat.common

import java.text.SimpleDateFormat
import java.util.*

object DateTimeHelper {
    fun timestampToTimeString(timestamp:Long): String {
        val timeFormat = SimpleDateFormat("HH:mm")
        return timeFormat.format(Date(timestamp))
    }
    fun timestampToString(timestamp:Long): String { // todo make smart
        val timeFormat = SimpleDateFormat("HH:mm")
        return timeFormat.format(Date(timestamp))
    }

    fun timestampToDateString(timestamp:Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd") // todo make smarter, timezone?
        return dateFormat.format(Date(timestamp))
    }
}