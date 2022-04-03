package com.onionchat.dr0id.database

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import androidx.room.Ignore
import com.onionchat.common.Logging
import com.onionchat.dr0id.queue.tasks.SendMessageTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.User
import java.nio.charset.Charset

enum class ConversationType {
    BROADCAST,
    CHAT,
    FEED,
    UNKOWN
}

class Conversation(
    val user: User?,
    var unreadMessages: Int = 0,
    var isOnline: Boolean = false,
    val broadcast: Broadcast? = null,
    var selected: Boolean = false,
    var lastMessage: EncryptedMessage? = null,
    var feedId:String? = null
) {

    fun getConversationType(): ConversationType {
        if(user != null) {
            return ConversationType.CHAT
        } else if(broadcast != null) {
            return ConversationType.BROADCAST
        }else if(feedId != null) {
            return ConversationType.FEED
        } else {
            Logging.e(TAG, "getConversationType [-] !! WARNING !! unknown conversation type!")
            return ConversationType.UNKOWN
        }
    }

    fun getLabel(): String {
        if(feedId == Conversation.DEFAULT_FEED_ID) {
            return "Feed"
        }
        user?.let {
            it.details?.let {
                if(it.isNotEmpty()) {
                    return@getLabel it[0].alias
                }
            }
            return@getLabel it.getHashedId()
        }
        broadcast?.let {
            return@getLabel it.real_label?:"ERROR"
        }
        return "ERROR"
    }

    fun getHashedId() : String {
        user?.let {
            return it.getHashedId()
        }
        broadcast?.let {
            return it.getHashedId()
        }
        feedId?.let {
            return it
        }
        return "ERROR"
    }

    fun getCertId() : String? {
        user?.let {
            return@getCertId it.certId
        }
        broadcast?.let {
            return@getCertId it.pub_alias
        }
        return null
    }


    fun getLastSymAlias() : String? {
        val symKeyAlias = if(user != null) {
            val updatedUser = UserManager.getUserById(user.id).get() // get latest key
            if (updatedUser?.symaliases == null || updatedUser.symaliases!!.isEmpty()) {
                Logging.d(TAG, "run [-] getLastSymAlias symmetric key found for user $user")
                null
            } else {
                updatedUser.getLastSymAlias()!!.alias // todo check if timestamp sort is correct
            }
        } else if(broadcast != null) {
            return broadcast.sym_key_alias
        }else if(feedId != null) {
            return UserManager.getMyFeedKey().get()?.alias
        } else {
            null
        }
        return symKeyAlias
    }

    fun getId(): String {
        user?.let {
            return it.id
        }
        broadcast?.let {
            return it.id
        }
        return "ERROR"
    }


    @Ignore
    var cachedBitmap: Bitmap? = null

    fun getAvatar(): Bitmap? {
        if(feedId != null) {
            return cachedBitmap
        }
        val str = if (user != null) {
            user.getHashedId()
        } else if (broadcast != null) {
            broadcast.id
        } else {
            "ERROR"
        }
        if (cachedBitmap == null) {
            cachedBitmap = getRepresentativeProfileBitmap(str)
        }
        return cachedBitmap
    }

    companion object {

        const val DEFAULT_FEED_ID = "feed"

        const val TAG = "Conversation"
        fun getRepresentativeProfileBitmap(visibleId: String): Bitmap? {
            val data = Base64.decode(visibleId, Base64.DEFAULT)
            Logging.d(TAG, "Test: " + String(data, Charset.forName("UTF-16"))[0])

            val width = 500
            val height = 500

            val alpha = 1 // change alpha as necessary
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val paint = Paint()
            val c = Canvas(bmp)
            var r = 0;
            data.forEachIndexed { i, it ->
                if ((i % 3) == 0) {
                    r += it.toInt()
                }
            }
            var g = 0;
            data.forEachIndexed { i, it ->
                if ((i % 3) == 1) {
                    g += it.toInt()
                }
            }
            var b = 0;
            data.forEachIndexed { i, it ->
                if ((i % 3) == 2) {
                    b += it.toInt()
                }
            }
            paint.setColor(Color.rgb(Math.abs(r) % 255, Math.abs(g) % 255, Math.abs(b) % 255));
            when (Math.abs(data[0].toInt()) % 4) {
                0 -> {
                    c.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), Math.abs(data[24].toInt()) + 50.toFloat(), paint)
                }
                1 -> {
                    c.drawRect(
                        (Math.abs(data[10].toInt())).toFloat(),
                        (Math.abs(data[11].toInt())).toFloat(),
                        width.toFloat() - (Math.abs(data[13].toInt())).toFloat(),
                        height.toFloat() - (Math.abs(data[14].toInt())).toFloat(),
                        paint
                    )

                }
                2 -> {
                    c.drawRoundRect(
                        Math.abs(data[11].toInt()).toFloat() + 100,
                        height.toFloat(),
                        width.toFloat() - (Math.abs(data[13].toInt())),
                        0 + Math.abs(data[6].toInt()).toFloat(),
                        70.0.toFloat(),
                        70.0.toFloat(),
                        paint
                    )
                }
                3 -> {
                    c.drawOval((Math.abs(data[10].toInt())).toFloat(), (Math.abs(data[11].toInt())).toFloat(), width.toFloat(), height.toFloat(), paint)
                }
                else -> {
                    val floats = ArrayList<Float>()
                    data.forEach {
                        floats.add((Math.abs(it.toFloat()) * 4) % width)
                    }
                    c.drawLines(floats.toFloatArray(), paint)
                }
            }
            //canvas.drawBitmap(mNotiNews[numbernews],0,0,null);
//
//            for (x in 0 until width) {
            //for (y in 0 until height) {
//                val red: Int = data[((x+y)*(x+y))%data.size].toInt()
//                val green: Int = data[(((x-y)*(x-y))+((y-x)*(y-x)))%data.size].toInt()
//                val blue: Int = data[((x*y)+(x*y))%data.size].toInt()
//                    val max = data.size - 1
//                    val k = x % (width / data[Math.abs(x) % max])
//                    val v = y % (height / data[Math.abs(y) % max])
//                    val index = ((k * k) * (v * v)) % (data.size - 1) //very nice!!
//                    val red: Int = data[Math.abs(index * index) % max].toInt() * 3
//                    val green: Int = data[Math.abs(index + index) % max].toInt() * 3
//                    val blue: Int = data[Math.abs(index or 100) % max].toInt() * 3

//                val k = data[((x * x) + y) % (data.size - 1)] %1
//                val v = data[((y * y) + x) % (data.size - 1)] %1
//                val red: Int = 1000 * k
//                val green: Int = 1000 * k
//                val blue: Int = 1000 * k


//                    var z = data[x * y % (data.size - 1)]
//                    var u = data[Math.abs((data.size - 1) - z) % (data.size - 1)]
//                    var k = 0
//                    k = (data[Math.abs(((z * z) + (u * u)) * (x * y)) % (data.size - 1)] % 2) * 255


            //val color: Int = Color.rgb(data[(x * y) % (data.size - 1)].toInt(), data[(x+2 * y+2) % (data.size - 1)].toInt(), data[(x+1 * y+1) % (data.size - 1)].toInt())

            //}

//                val k = Math.abs(data[Math.abs(x % (data.size - 1))].toInt())
//                val y = Math.abs(k * k) % height
//                bmp.setPixel(x, y, Color.rgb(k%255,k%255,k%255))
//            }
//            val PI = 3.1415926535
//
//            val r = 300
//            for (k in 0 until 3600) {
//                val angle = k.toDouble() / 10.0
//                val x1 = r * Math.cos(angle * PI / 180.0)
//                val y1 = r * Math.sin(angle * PI / 180.0)
//                bmp.setPixel(
//                    x1.toInt()+(r/2)+1,
//                    y1.toInt()+(r/2)+1,
//                    Color.rgb(
//                        data[Math.abs(x1.toInt() * y1.toInt()) % (data.size - 1)].toInt() * 2,
//                        data[Math.abs(x1.toInt() * y1.toInt()) % (data.size - 1)].toInt() * 2,
//                        data[Math.abs(x1.toInt() * y1.toInt()) % (data.size - 1)].toInt() * 2
//                    )
//                )
//            }

//        var lastByte: Byte = 0
//        data.forEach {
//            val max = data.size - 1
//            var x = 0;
//            var y = 0;
//            x = ((it * it) * (lastByte * lastByte))
//            y = ((x * x) * (it * lastByte))
//            val red: Int = 100
//            val green: Int = 100
//            val blue: Int = x
//            val color: Int = Color.argb(alpha, red, green, blue)
//            bmp.setPixel(Math.abs(x) % width, Math.abs(y) % height, color)
//            lastByte = it
//        }


            return bmp;
        }
    }
}