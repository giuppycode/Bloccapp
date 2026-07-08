package com.example.bloccapp.data.repository

import com.example.bloccapp.data.db.dao.UserDao
import com.example.bloccapp.data.db.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    val user: Flow<User?> = userDao.getUser()

    suspend fun updateDisplayName(name: String) {
        val currentUser = User(displayName = name) // id = 1 by default
        userDao.insertOrUpdate(currentUser)
    }
}
