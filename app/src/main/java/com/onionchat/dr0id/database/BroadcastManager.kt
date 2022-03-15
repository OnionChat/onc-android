package com.onionchat.dr0id.database

import com.onionchat.common.Logging
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.BroadcastMember
import com.onionchat.localstorage.userstore.User
import java.util.concurrent.Future

object BroadcastManager {

    fun createBroadcast(label: String): Future<Broadcast?> {
        return DatabaseManager.submit {
            UserManager.myId?.let {
                val broadcast = Broadcast(Broadcast.generateId(label, it), label)
                DatabaseManager.db.broadcastDao().insertAll(broadcast)
                broadcast
            } ?: run {
                null
            }
        }
    }

    fun addBroadcast(broadcast: Broadcast) {
        DatabaseManager.submit {
            DatabaseManager.db.broadcastDao().insertAll(broadcast)
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