package com.onionchat.localstorage.userstore

import androidx.room.Embedded
import androidx.room.Relation

data class UserJoined(
    @Embedded val user: User,
    @Relation(parentColumn = "id", entityColumn = "userid", entity = SymAlias::class) val symaliases: List<SymAlias>
) {
}