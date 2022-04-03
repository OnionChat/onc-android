package com.onionchat.dr0id.database

import android.content.Context
import com.onionchat.common.AddUserPayload
import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.localstorage.EncryptedLocalStorage
import com.onionchat.localstorage.userstore.ContactDetails
import com.onionchat.localstorage.userstore.FeedKey
import com.onionchat.localstorage.userstore.PingInfo
import com.onionchat.localstorage.userstore.User
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

object UserManager {
    var myId: String? = null
        set(value) {
            if (myId == null) {
                Logging.d(TAG, "mId [+] myId was set 5o <${myId}>")
                field = value
            }
        }

    fun getMyHashedId(): String {
        return IDGenerator.toHashedId(myId!!)
    }

    fun getUserById(id: String): Future<User?> {
        return DatabaseManager.submit(object : Callable<User?> {
            override fun call(): User? {
                Logging.d("UserManager", "getUserById(" + id + ")")
                val users = DatabaseManager.db.userDao().getAllUsersByIds(arrayOf(id))
                if (users.size == 0) {
                    return null
                } else {
                    return users.get(0)
                }
            }
        })
    }

    fun getConversationByHashedId(hashedId: String): Future<Conversation?> {
        return DatabaseManager.submit(object : Callable<Conversation?> {
            override fun call(): Conversation? {
                Logging.d(TAG, "getUserByHashedId($hashedId)")
                if (hashedId == Conversation.DEFAULT_FEED_ID) {
                    return Conversation(user = null, feedId = hashedId)
                }
                DatabaseManager.db.userDao().getAllUsers().forEach {
                    if (hashedId == it.getHashedId()) {
                        return@call Conversation(user = it)
                    }
                }
                return null
            }
        }
        )
    }

    fun getUserByHashedId(hashedUid: String): Future<User?> {
        return DatabaseManager.submit(object : Callable<User?> {
            override fun call(): User? {
                Logging.d("UserManager", "getUserByHashedId(" + hashedUid + ")")
                DatabaseManager.db.userDao().getAllUsers().forEach {
                    if (hashedUid == it.getHashedId()) {
                        return@call it
                    }
                }
                return null
            }
        }
        )
    }

    fun addContactDetails(contactDetails: ContactDetails): Future<Boolean> {
        Logging.d(TAG, "addContactDetails [+] adding contact details $contactDetails")
        return DatabaseManager.submit(object : Callable<Boolean> {
            override fun call(): Boolean {
                DatabaseManager.db.contactDetailsDao().insertAll(contactDetails)
                return true
            }
        })
    }

    fun addUser(payload: AddUserPayload): User {
        Logging.d(TAG, "addContactDetails [+] adding user $payload")
        val pubKeyHash = Crypto.pubKeyHash(payload.pubKey)
        val user = User(payload.uid, pubKeyHash)
        Crypto.storePublicKey(pubKeyHash, payload.pubKey)
        DatabaseManager.submit {
            DatabaseManager.db.userDao().insertAll(user)
        }
        return user;
    }

    fun removeUser(user: User) {
        Logging.d(TAG, "addContactDetails [+] remove user $user")
        Crypto.storePublicKey(user.certId, null)
        DatabaseManager.submit(Callable {
            DatabaseManager.db.userDao().delete(user)
        })
    }

    fun getAllUsers(): Future<List<User>> {
        return DatabaseManager.submit(object : Callable<List<User>> {
            override fun call(): List<User> {
                return DatabaseManager.db.userDao().getAllUsers()
            }
        })
    }

    fun getAllOnlineUsers(): Future<List<User>> {
        return getAllUsers()
    }

    fun getMyLabel(context: Context): String? {
        Crypto.getMyPublicKey()?.let {
            EncryptedLocalStorage(it, Crypto.getMyKey(), context).let { storage ->
                return@getMyLabel storage.getValue(context.getString(R.string.key_user_label))
            }
        }

        return null
    }

    fun getMyFeedKey(): Future<FeedKey?> {
        return DatabaseManager.submit(object : Callable<FeedKey?> {
            override fun call(): FeedKey? {
                val lastKey = getLastFeedKey()
                if (lastKey == null) {
                    val alias = UUID.randomUUID().toString()
                    val symmetricKeyBytes = Crypto.generateSymmetricKey(alias)
                    val pub = Crypto.getMyPublicKey()
                    if (pub == null) {
                        Logging.e(TAG, "getMyFeedKey [-] unable to get my public key")
                        return@call null
                    }
                    val encryptedKey = Crypto.encryptAsym(pub, symmetricKeyBytes)
                    val feedKey = FeedKey(UUID.randomUUID().toString(), encryptedKey, alias, System.currentTimeMillis())
                    Logging.d(TAG, "getMyFeedKey [+] generated a new one(${feedKey})")

                    DatabaseManager.db.feedKeyDao().insertAll(feedKey)
                    return feedKey
                }
                Logging.d(TAG, "getMyFeedKey(${lastKey})")
                return lastKey
            }
        })
    }

    fun getFeedKey(alias: String): Future<FeedKey?> {
        return DatabaseManager.submit(object : Callable<FeedKey?> {
            override fun call(): FeedKey? {
                Logging.d(TAG, "getFeedKey($alias)")
                val keys = DatabaseManager.db.feedKeyDao().loadAllByAlias(arrayOf(alias))
                if (keys.size == 0) {
                    return null
                } else {
                    return keys.get(0)
                }
            }
        })
    }

    internal fun getLastFeedKey(): FeedKey? {
        val keys = DatabaseManager.db.feedKeyDao().getLastKey()
        if (keys.size == 0) {
            return null
        } else {
            return keys.get(0)
        }
    }

    fun storeFeedKey(feedKey: FeedKey): Future<Boolean> {
        return DatabaseManager.submit {
            Logging.d(TAG, "storeFeedKey($feedKey)")
            DatabaseManager.db.feedKeyDao().insertAll(feedKey)
            true
        }
    }

    fun storePingInfo(pingInfo: PingInfo): Future<Boolean> {
        return DatabaseManager.submit {
            Logging.d(TAG, "storePingInfo($pingInfo)")
            DatabaseManager.db.pingInfoDao().insertAll(pingInfo)
            true
        }
    }

    fun getLastSeen(user: User): Future<PingInfo?> {
        return DatabaseManager.submit {
            Logging.d(TAG, "getLastSeen($user)")
            val latest = DatabaseManager.db.pingInfoDao().loadLatestByUserIdAndStatus(user.getHashedId(), PingInfo.PingInfoStatus.SUCCESS.ordinal)
            if (latest.isEmpty()) {
                return@submit null
            } else {
                return@submit latest[0]
            }
        }
    }

    fun getPingInfos(user: User): Future<List<PingInfo>> {
        return DatabaseManager.submit {
            Logging.d(TAG, "getLastSeen($user)")
            val data = DatabaseManager.db.pingInfoDao().loadAllByUserId(user.getHashedId())
            return@submit data
        }
    }

    const val TAG = "UserManager"

//    init {
//        mContacts["25eb3d9ec4a6019c"] = User("25eb3d9ec4a6019c") // demo data
//    }
}