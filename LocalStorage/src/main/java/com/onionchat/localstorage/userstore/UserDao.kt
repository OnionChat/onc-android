package com.onionchat.localstorage.userstore

import androidx.room.*

@Dao
abstract class UserDao {
    @Query("SELECT * FROM user")
    abstract fun getAll(): List<User>

    @Query("SELECT * FROM user")
    abstract fun getAllJoined(): List<UserJoined>


    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        getAllJoined().forEach {
            it.user.symaliases = it.symaliases
            it.user.details = it.details
            users.add(it.user)
        }
        return users
    }

    @Transaction
    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    abstract fun loadAllByIds(userIds: Array<String>): List<UserJoined>


    fun getAllUsersByIds(userIds: Array<String>): List<User> {
        val users = mutableListOf<User>()
        loadAllByIds((userIds)).forEach {
            it.user.symaliases = it.symaliases
            it.user.details = it.details
            users.add(it.user)
        }
        return users
    }

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Transaction
    @Insert
    abstract fun insertAll(vararg users: User)

    @Delete
    abstract fun delete(user: User)
}
