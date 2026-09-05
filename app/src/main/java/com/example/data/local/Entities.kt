package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "guest",
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedLog: String? = null,
    val logSnippet: String? = null,
    val problemTitle: String? = null,
    val probableCause: String? = null,
    val solutionSteps: String? = null, // serialized or bullet points
    val confidenceStatus: String? = null, // "CONFIRMED INFORMATION", "LIKELY CAUSE", "POSSIBLE CAUSE"
    val isFollowUpPrompt: Boolean = false
)

@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isLoggedIn: Boolean = true,
    val lastLoginTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "device_profile")
data class DeviceProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val model: String = "Auto-Detected",
    val manufacturer: String = "Android",
    val androidVersion: String = "Android 14",
    val totalRamGb: Double = 6.0,
    val availableRamGb: Double = 3.2,
    val cpuArchitecture: String = "arm64-v8a",
    val freeStorageGb: Double = 25.0,
    val minecraftVersion: String = "1.21.1",
    val loader: String = "Fabric",
    val loaderVersion: String = "0.16.5",
    val zyronVersion: String = "Zyron Launcher 2.4.2",
    val installedMods: String = "Sodium 0.5.11, Iris 1.7.3, Fabric API 0.102.0",
    val shadersOrResourcePacks: String = "None",
    val renderer: String = "Holy GL4ES (Recommended for Mali/Adreno)",
    val allocatedRamMb: Int = 3072,
    val workedPreviously: Boolean = true,
    val whatHappenedBefore: String = "Crashed during loading Mojang splash screen"
)

@Entity(tableName = "crash_logs")
data class CrashLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val logContent: String,
    val detectedException: String,
    val detectedCause: String,
    val timestamp: Long = System.currentTimeMillis()
)
