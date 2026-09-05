package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ZyronDao {
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForUser(userId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatHistoryForUser(userId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    @Query("SELECT * FROM device_profile WHERE id = 1 LIMIT 1")
    fun getDeviceProfile(): Flow<DeviceProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDeviceProfile(profile: DeviceProfileEntity)

    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC")
    fun getAllSavedLogs(): Flow<List<CrashLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrashLog(log: CrashLogEntity): Long

    @Query("SELECT * FROM user_account WHERE isLoggedIn = 1 ORDER BY lastLoginTime DESC LIMIT 1")
    fun getCurrentUser(): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserAccountEntity)

    @Query("UPDATE user_account SET isLoggedIn = 0")
    suspend fun logoutAllUsers()
}

@Database(
    entities = [ChatMessageEntity::class, DeviceProfileEntity::class, CrashLogEntity::class, UserAccountEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ZyronDatabase : RoomDatabase() {
    abstract fun zyronDao(): ZyronDao
}
