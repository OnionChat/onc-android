package com.onionchat.dr0id.database

import com.onionchat.localstorage.userstore.SymAlias
import com.onionchat.localstorage.userstore.User
import java.util.concurrent.Future

object KeyManager {

    fun addSymKey(user: User, alias:SymAlias): Future<Boolean> {
        return DatabaseManager.submit {

            DatabaseManager.db.symAliasDao().insertAll(alias)
            true
        }
    }
}