package com.onionchat.dr0id.users

import com.onionchat.localstorage.userstore.Broadcast
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
}