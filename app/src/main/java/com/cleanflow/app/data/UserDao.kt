package com.cleanflow.app.data

import androidx.room.*
import com.cleanflow.app.models.User

@Dao
interface UserDao {
    @Upsert
    suspend fun upsertUser(user: User)

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): User?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}