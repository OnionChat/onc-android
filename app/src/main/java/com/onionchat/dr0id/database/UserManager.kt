package com.onionchat.dr0id.users

import android.content.Context
import androidx.room.Room
import com.onionchat.common.Crypto
import com.onionchat.common.QrPayload
import com.onionchat.common.Logging
import com.onionchat.localstorage.userstore.User
import com.onionchat.localstorage.userstore.UsersDatabase
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

object UserManager {
    var myId: String? = null
        set(value) {
            if (myId == null) {
                field = value
            }
        }


    fun getUserById(id: String): Future<User?> {
        return DatabaseManager.submit(object : Callable<User?> {
            override fun call(): User? {
                Logging.d("UserManager", "getUserById(" + id + ")")
                val users = DatabaseManager.db.userDao().loadAllByIds(arrayOf(id))
                if(users.size == 0) {
                    return null
                } else {
                    return users.get(0)
                }
            }
        })
    }

    fun addUser(payload: QrPayload): User {
        val pubKeyHash = Crypto.pubKeyHash(payload.pubKey)
        val user = User(payload.uid, pubKeyHash)
        Crypto.storePublicKey(pubKeyHash, payload.pubKey)
        DatabaseManager.submit(Callable {
            DatabaseManager.db.userDao().insertAll(user)
        })
        return user;
    }

    fun removeUser(user: User) {
        Crypto.storePublicKey(user.certId, null)
        DatabaseManager.submit(Callable {
            DatabaseManager.db.userDao().delete(user)
        })
    }

    fun getAllUsers(): Future<List<User>> {
        return DatabaseManager.submit(object : Callable<List<User>> {
            override fun call(): List<User> {
                return DatabaseManager.db.userDao().getAll()
            }
        })
    }


//    init {
//        mContacts["25eb3d9ec4a6019c"] = User("25eb3d9ec4a6019c") // demo data
//    }
}