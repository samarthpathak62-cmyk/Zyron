package com.example.data.repository

import com.example.data.local.ChatMessageEntity
import com.example.data.local.CrashLogEntity
import com.example.data.local.DeviceProfileEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.ZyronDao
import kotlinx.coroutines.flow.Flow

class ZyronRepository(private val dao: ZyronDao) {

    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()
    val deviceProfile: Flow<DeviceProfileEntity?> = dao.getDeviceProfile()
    val savedLogs: Flow<List<CrashLogEntity>> = dao.getAllSavedLogs()
    val currentUser: Flow<UserAccountEntity?> = dao.getCurrentUser()

    fun getMessagesForUser(userId: String): Flow<List<ChatMessageEntity>> {
        return dao.getMessagesForUser(userId)
    }

    suspend fun addMessage(message: ChatMessageEntity): Long {
        return dao.insertMessage(message)
    }

    suspend fun clearMessagesForUser(userId: String) {
        dao.clearChatHistoryForUser(userId)
    }

    suspend fun clearMessages() {
        dao.clearChatHistory()
    }

    suspend fun updateDeviceProfile(profile: DeviceProfileEntity) {
        dao.saveDeviceProfile(profile)
    }

    suspend fun saveCrashLog(log: CrashLogEntity): Long {
        return dao.insertCrashLog(log)
    }

    suspend fun saveUser(user: UserAccountEntity) {
        dao.saveUser(user)
    }

    suspend fun logoutAllUsers() {
        dao.logoutAllUsers()
    }
}
