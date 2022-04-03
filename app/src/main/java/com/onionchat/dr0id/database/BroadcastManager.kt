package com.onionchat.dr0id.database

import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.BroadcastMember
import com.onionchat.localstorage.userstore.User
import java.util.*
import java.util.concurrent.Future

object BroadcastManager {

    const val TAG = "BroadcastManager"

    fun createBroadcast(label: String): Future<Broadcast?> {
        return DatabaseManager.submit {
            UserManager.myId?.let {
                /*
                    var real_label: String? = null
    var sym_key_alias: String? = null
    var sym_key: ByteArray? = null
    var pub: ByteArray? = null
    var pub_alias: String? = null
    var signature: String? = null
                 */
                // 1. generate asym key
                val asym_key_alias = UUID.randomUUID().toString()
                Crypto.generateKey(asym_key_alias)
                val cert = Crypto.getPublicKey(asym_key_alias)
                val priv = Crypto.getKey(asym_key_alias)
                if(cert == null || priv == null) {
                    Logging.e(TAG, "createBroadcast [-] unable to get asym crypto $asym_key_alias")
                    return@submit null
                }

                // 2. generate sym key
                val sym_key_alias = UUID.randomUUID().toString()
                val sym_key_bytes = Crypto.generateSymmetricKey(sym_key_alias)

                // 3. create signature
                //val signature_bytes = Crypto.sign(priv, sym_key_bytes)


                // todo temporary hack
                val broadcast = Broadcast(Broadcast.generateId(label, it), label)
                broadcast.real_label = label
                broadcast.pub_alias = asym_key_alias
                broadcast.pub = cert.encoded
                broadcast.sym_key_alias = sym_key_alias
                broadcast.sym_key = sym_key_bytes
                broadcast.signature = byteArrayOf()


                DatabaseManager.db.broadcastDao().insertAll(Broadcast(broadcast.id, Broadcast.createPayload(broadcast)))
                return@submit broadcast
            } ?: run {
                return@submit null
            }
        }
    }

    fun addBroadcast(broadcast: Broadcast): Future<Boolean> {
        return DatabaseManager.submit {
            try {
                Crypto.storeSymmetricKey(broadcast.sym_key_alias!!, broadcast.sym_key)
                Crypto.storePublicKey(broadcast.pub_alias!!, broadcast.pub)
                DatabaseManager.db.broadcastDao().insertAll(broadcast)
                return@submit true
            }catch (exception:Exception) {
                Logging.e(TAG, "addBroadcast [-] unable to add broadcast $broadcast")
            }
            return@submit false
        }
    }

    fun getAllBroadcasts(): Future<List<Broadcast>> {
        return DatabaseManager.submit {
            DatabaseManager.db.broadcastDao().getAll()
        }
    }

    fun getBroadcastById(id: String): Future<Broadcast?> {
        return DatabaseManager.submit {
            val broadcasts = DatabaseManager.db.broadcastDao().loadAllByIds(arrayOf(id))
            if (broadcasts.size == 0) {
                null
            } else {
                broadcasts[0]
            }
        }
    }

    fun removeBroadcast(broadcast: Broadcast) {
        DatabaseManager.submit {
            DatabaseManager.db.broadcastDao().delete(broadcast)
        }
    }

    fun deleteUsersFromBroadcast(broadcast: Broadcast, users: Array<String>) {
        DatabaseManager.submit {
            DatabaseManager.db.broadcastMemberDao().deleteMembersOfBroadcast(broadcast.id, users)
        }
    }

    fun addUsersToBroadcast(broadcast: Broadcast, users: List<User>) {
        DatabaseManager.submit {
            val members = BroadcastMember.createList(broadcast, users)
            Logging.d("BroadcastManager", "Finally added <" + members.size + "> members")
            DatabaseManager.db.broadcastMemberDao().insertAllMembers(members.toTypedArray())
        }
    }

    fun getBroadcastUsers(broadcast: Broadcast): Future<List<User>> {
        return DatabaseManager.submit {
            val member_id_list = arrayListOf<String>()
            val members = DatabaseManager.db.broadcastMemberDao().loadAllByBroadcastIds(arrayOf(broadcast.id))
            Logging.d("BroadcastManager", "Finally fetched <" + members.size + "> members")
            members.forEach {
                member_id_list.add(it.user_id)
            }
            val users = DatabaseManager.db.userDao().getAllUsersByIds(member_id_list.toTypedArray()) // todo use UserManager
            Logging.d("BroadcastManager", "Finally fetched <" + users.size + "> users")
            users
        }
    }
}