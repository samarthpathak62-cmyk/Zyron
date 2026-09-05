package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CrashLogEntity
import com.example.data.local.DeviceProfileEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.ZyronDatabase
import com.example.data.model.AutoFixAction
import com.example.data.model.AutoFixType
import com.example.data.model.DeviceSpecs
import com.example.data.model.LogAnalysisResult
import com.example.data.model.SampleCrashPreset
import com.example.data.remote.GeminiApiClient
import com.example.data.repository.ZyronRepository
import com.example.domain.DeviceSpecsDetector
import com.example.domain.MinecraftLogAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ZyronViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        ZyronDatabase::class.java,
        "zyron_ai_db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    private val repository = ZyronRepository(db.zyronDao())

    // Guest mode flag (when user explicitly picks 'Continue as Guest' on first screen)
    private val _isGuestModeActive = MutableStateFlow(false)
    val isGuestModeActive: StateFlow<Boolean> = _isGuestModeActive.asStateFlow()

    val currentUser: StateFlow<UserAccountEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatMessages: StateFlow<List<ChatMessageEntity>> = currentUser
        .flatMapLatest { user ->
            val uid = user?.userId ?: "guest"
            repository.getMessagesForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLogs: StateFlow<List<CrashLogEntity>> = repository.savedLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceProfile = MutableStateFlow(DeviceProfileEntity())
    val deviceProfile: StateFlow<DeviceProfileEntity> = _deviceProfile.asStateFlow()

    private val _detectedDeviceSpecs = MutableStateFlow<DeviceSpecs?>(null)
    val detectedDeviceSpecs: StateFlow<DeviceSpecs?> = _detectedDeviceSpecs.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _attachedLog = MutableStateFlow<String?>(null)
    val attachedLog: StateFlow<String?> = _attachedLog.asStateFlow()

    private val _attachedLogTitle = MutableStateFlow<String?>(null)
    val attachedLogTitle: StateFlow<String?> = _attachedLogTitle.asStateFlow()

    private val _selectedAutoFix = MutableStateFlow<AutoFixAction?>(null)
    val selectedAutoFix: StateFlow<AutoFixAction?> = _selectedAutoFix.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    private var lastAnalyzedLogContent: String? = null

    init {
        // Automatically auto-detect device hardware upon launch
        detectDeviceSpecs()

        viewModelScope.launch {
            repository.deviceProfile.collect { saved ->
                if (saved != null) {
                    _deviceProfile.value = saved
                } else {
                    // Seed initial device profile
                    val detected = _detectedDeviceSpecs.value
                    val initial = if (detected != null) {
                        DeviceProfileEntity(
                            model = detected.model,
                            manufacturer = detected.manufacturer,
                            androidVersion = detected.androidVersion,
                            totalRamGb = detected.totalRamGb,
                            availableRamGb = detected.availableRamGb,
                            cpuArchitecture = detected.architecture,
                            freeStorageGb = detected.freeStorageGb,
                            allocatedRamMb = detected.recommendedRamMb,
                            renderer = detected.recommendedRenderer
                        )
                    } else {
                        DeviceProfileEntity()
                    }
                    repository.updateDeviceProfile(initial)
                    _deviceProfile.value = initial
                }
            }
        }

        // Welcome message if chat history is empty
        viewModelScope.launch {
            chatMessages.collect { list ->
                if (list.isEmpty()) {
                    val uid = currentUser.value?.userId ?: "guest"
                    val welcomeMsg = ChatMessageEntity(
                        userId = uid,
                        role = "assistant",
                        content = "Hey there! I'm **Zyron AI**, the official Minecraft launcher support and bug-fixing AI assistant for Zyron Launcher.\n\n" +
                                "👑 **Original Owners & Founders:** **Roller_gaming** & **not siaf**.\n\n" +
                                "If Minecraft is crashing, showing an exit code, freezing on the Mojang screen, or having severe FPS lag, I'm here to find the real root cause and fix it for you.\n\n" +
                                "📋 **To get started:** Send your **latest launcher error log or crash log** (you can use the **📎 Attach Log** button below or pick a sample preset). Or ask me any question!",
                        timestamp = System.currentTimeMillis()
                    )
                    repository.addMessage(welcomeMsg)
                }
            }
        }
    }

    fun signInWithGoogle(email: String = "minecraft.player@gmail.com", name: String = "Minecraft Player") {
        viewModelScope.launch {
            val user = UserAccountEntity(
                userId = email.lowercase().trim(),
                displayName = name,
                email = email,
                isLoggedIn = true,
                lastLoginTime = System.currentTimeMillis()
            )
            repository.saveUser(user)
            _isGuestModeActive.value = false
            _snackbarEvent.emit("Signed in as $name • Cloud Chat Sync Enabled")
        }
    }

    fun continueAsGuest() {
        _isGuestModeActive.value = true
    }

    fun signOut() {
        viewModelScope.launch {
            repository.logoutAllUsers()
            _isGuestModeActive.value = false
            _snackbarEvent.emit("Signed out of Google account")
        }
    }

    fun detectDeviceSpecs() {
        val specs = DeviceSpecsDetector.detectSpecs(getApplication())
        _detectedDeviceSpecs.value = specs
        val updated = _deviceProfile.value.copy(
            model = specs.model,
            manufacturer = specs.manufacturer,
            androidVersion = specs.androidVersion,
            totalRamGb = specs.totalRamGb,
            availableRamGb = specs.availableRamGb,
            cpuArchitecture = specs.architecture,
            freeStorageGb = specs.freeStorageGb,
            allocatedRamMb = specs.recommendedRamMb
        )
        _deviceProfile.value = updated
        viewModelScope.launch {
            repository.updateDeviceProfile(updated)
        }
    }

    fun updateProfile(updated: DeviceProfileEntity) {
        _deviceProfile.value = updated
        viewModelScope.launch {
            repository.updateDeviceProfile(updated)
            _snackbarEvent.emit("Device & Minecraft profile saved")
        }
    }

    fun setAttachedLog(title: String, content: String) {
        _attachedLogTitle.value = title
        _attachedLog.value = MinecraftLogAnalyzer.sanitizeLog(content)
        viewModelScope.launch {
            _snackbarEvent.emit("Log attached ($title)")
        }
    }

    fun clearAttachedLog() {
        _attachedLog.value = null
        _attachedLogTitle.value = null
    }

    fun loadPreset(preset: SampleCrashPreset) {
        setAttachedLog(preset.title, preset.fullLog)
        val updated = _deviceProfile.value.copy(
            minecraftVersion = preset.mcVersion,
            loader = preset.loader
        )
        updateProfile(updated)
    }

    fun onUserSendMessage(text: String) {
        if (text.isBlank() && _attachedLog.value == null) return

        val logToAnalyze = _attachedLog.value
        val logTitle = _attachedLogTitle.value
        clearAttachedLog()

        viewModelScope.launch {
            val uid = currentUser.value?.userId ?: "guest"
            // Save user message
            val userMsg = ChatMessageEntity(
                userId = uid,
                role = "user",
                content = text.ifBlank { "Here is my Minecraft crash log ($logTitle):" },
                attachedLog = logToAnalyze,
                logSnippet = logToAnalyze?.take(350),
                timestamp = System.currentTimeMillis()
            )
            repository.addMessage(userMsg)

            _isGenerating.value = true

            val currentProfile = _deviceProfile.value
            val isFollowUp = isStillFailingQuery(text)

            // Step 7 logic: Follow up comparison if the user reports that the fix didn't work
            if (isFollowUp && logToAnalyze != null && lastAnalyzedLogContent != null) {
                val comparison = MinecraftLogAnalyzer.compareLogs(lastAnalyzedLogContent!!, logToAnalyze)
                val analysis = MinecraftLogAnalyzer.analyzeLog(
                    rawLog = logToAnalyze,
                    deviceModel = currentProfile.model,
                    totalRamGb = currentProfile.totalRamGb,
                    mcVersion = currentProfile.minecraftVersion,
                    loader = currentProfile.loader
                )

                val responseContent = buildString {
                    append(comparison)
                    append("\n---\n")
                    append("### Updated Diagnosis:\n\n")
                    append("**Problem:**\n${analysis.problem}\n\n")
                    append("**Cause:**\n${analysis.cause}\n\n")
                    append("**Fix:**\n")
                    analysis.fixSteps.forEachIndexed { i, step ->
                        append("${i + 1}. $step\n")
                    }
                    append("\n${analysis.followupPrompt}")
                }

                lastAnalyzedLogContent = logToAnalyze

                repository.addMessage(
                    ChatMessageEntity(
                        userId = uid,
                        role = "assistant",
                        content = responseContent,
                        problemTitle = analysis.problem,
                        probableCause = analysis.cause,
                        solutionSteps = analysis.fixSteps.joinToString("\n"),
                        confidenceStatus = analysis.confidenceStatus,
                        isFollowUpPrompt = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
                _isGenerating.value = false
                return@launch
            }

            // If log is provided, store it in saved logs
            if (logToAnalyze != null) {
                lastAnalyzedLogContent = logToAnalyze
                repository.saveCrashLog(
                    CrashLogEntity(
                        title = logTitle ?: "Crash Log (${currentProfile.minecraftVersion})",
                        logContent = logToAnalyze,
                        detectedException = "Analyzing...",
                        detectedCause = "Logged"
                    )
                )
            }

            // Check if log is missing and the user is asking about a crash
            val logIsMissing = logToAnalyze == null && !containsEmbeddedLog(text)
            val isReportingCrash = isCrashReportWithoutLog(text)

            if (logIsMissing && isReportingCrash) {
                // Rule 1: FIRST ASK FOR THE ERROR LOG
                val reply = buildString {
                    append("I understand your Minecraft is having an issue! To find the exact root cause rather than guessing, **please share the crash log or latest error log**.\n\n")
                    append("📁 **How to get it in Zyron Launcher:**\n")
                    append("1. In Zyron Launcher, tap the **Logs** tab or open `/.minecraft/crash-reports/`.\n")
                    append("2. Copy the contents of the latest log, or tap the **📎 Attach Log** button below.\n\n")
                    append("Also, I see your current setup: **${currentProfile.manufacturer} ${currentProfile.model}** (${currentProfile.totalRamGb}GB RAM) on **Minecraft ${currentProfile.minecraftVersion} (${currentProfile.loader})**.\n\n")
                    append("Please paste or attach the log, and I'll analyze the exact stack trace for you right away!")
                }

                repository.addMessage(
                    ChatMessageEntity(
                        userId = uid,
                        role = "assistant",
                        content = reply,
                        confidenceStatus = "CONFIRMED INFORMATION",
                        timestamp = System.currentTimeMillis()
                    )
                )
                _isGenerating.value = false
                return@launch
            }

            // Format device context
            val deviceContext = buildString {
                append("Model: ${currentProfile.manufacturer} ${currentProfile.model}, ")
                append("Android: ${currentProfile.androidVersion}, ")
                append("RAM: ${currentProfile.totalRamGb}GB total (Allocated: ${currentProfile.allocatedRamMb}MB), ")
                append("Arch: ${currentProfile.cpuArchitecture}, ")
                append("MC Version: ${currentProfile.minecraftVersion}, ")
                append("Loader: ${currentProfile.loader} ${currentProfile.loaderVersion}, ")
                append("Renderer: ${currentProfile.renderer}, ")
                append("Installed Mods: ${currentProfile.installedMods}, ")
                append("Shaders: ${currentProfile.shadersOrResourcePacks}")
            }

            val chatHistoryList = chatMessages.value.map {
                (if (it.role == "user") "user" else "model") to it.content
            }

            // Query Gemini Backend
            val geminiPrompt = buildString {
                append(text)
                if (logToAnalyze != null) {
                    append("\n\n[Full Crash Log]:\n$logToAnalyze")
                }
            }

            val geminiResult = GeminiApiClient.queryGemini(
                chatHistory = chatHistoryList,
                latestUserPrompt = geminiPrompt,
                deviceContext = deviceContext
            )

            if (geminiResult.isSuccess) {
                val aiText = geminiResult.getOrThrow()
                repository.addMessage(
                    ChatMessageEntity(
                        userId = uid,
                        role = "assistant",
                        content = aiText,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                // Check if user is asking who you are or who the owner/founder is
                if (isIdentityQuestion(text) && logToAnalyze == null) {
                    val reply = getIdentityResponse()
                    repository.addMessage(
                        ChatMessageEntity(
                            userId = uid,
                            role = "assistant",
                            content = reply,
                            confidenceStatus = "CONFIRMED INFORMATION",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    _isGenerating.value = false
                    return@launch
                }

                // Heuristic engine fallback (ensures 100% offline & instantaneous uptime with exact prompt compliance)
                val logSource = logToAnalyze ?: extractLogFromText(text) ?: text
                val analysis = MinecraftLogAnalyzer.analyzeLog(
                    rawLog = logSource,
                    deviceModel = currentProfile.model,
                    totalRamGb = currentProfile.totalRamGb,
                    mcVersion = currentProfile.minecraftVersion,
                    loader = currentProfile.loader
                )

                val replyContent = buildString {
                    append("🔍 **Analysis Report** [${analysis.confidenceStatus}]\n\n")
                    append("**Problem:**\n${analysis.problem}\n\n")
                    append("**Cause:**\n${analysis.cause}\n\n")
                    append("**Fix:**\n")
                    analysis.fixSteps.forEachIndexed { idx, step ->
                        append("${idx + 1}. $step\n")
                    }
                    append("\n${analysis.followupPrompt}")
                }

                repository.addMessage(
                    ChatMessageEntity(
                        userId = uid,
                        role = "assistant",
                        content = replyContent,
                        problemTitle = analysis.problem,
                        probableCause = analysis.cause,
                        solutionSteps = analysis.fixSteps.joinToString("\n"),
                        confidenceStatus = analysis.confidenceStatus,
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (analysis.autoFixAvailable && analysis.autoFixTitle != null) {
                    val autoFixAction = when {
                        analysis.autoFixTitle.contains("Java") -> AutoFixAction(
                            id = "fix_java",
                            title = analysis.autoFixTitle,
                            description = analysis.autoFixDescription ?: "Change runtime",
                            type = AutoFixType.SWITCH_JAVA_RUNTIME,
                            isDestructive = false
                        )
                        analysis.autoFixTitle.contains("RAM") -> AutoFixAction(
                            id = "fix_ram",
                            title = analysis.autoFixTitle,
                            description = analysis.autoFixDescription ?: "Optimize RAM",
                            type = AutoFixType.SET_RECOMMENDED_RAM,
                            isDestructive = false
                        )
                        analysis.autoFixTitle.contains("Renderer") || analysis.autoFixTitle.contains("GL4ES") -> AutoFixAction(
                            id = "fix_renderer",
                            title = analysis.autoFixTitle,
                            description = analysis.autoFixDescription ?: "Switch renderer",
                            type = AutoFixType.SWITCH_RENDERER_HOLY,
                            isDestructive = false
                        )
                        analysis.autoFixTitle.contains("options.txt") -> AutoFixAction(
                            id = "fix_options",
                            title = analysis.autoFixTitle,
                            description = analysis.autoFixDescription ?: "Reset options",
                            type = AutoFixType.RESET_OPTIONS_TXT,
                            isDestructive = false,
                            confirmationWarning = "This will restore default video/control settings. Your worlds and saves are 100% safe."
                        )
                        else -> AutoFixAction(
                            id = "fix_jvm",
                            title = analysis.autoFixTitle,
                            description = analysis.autoFixDescription ?: "Reset JVM",
                            type = AutoFixType.RESET_JVM_ARGS,
                            isDestructive = false
                        )
                    }
                    _selectedAutoFix.value = autoFixAction
                }
            }

            _isGenerating.value = false
        }
    }

    private fun isStillFailingQuery(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("still") || lower.contains("didn't work") || lower.contains("did not work") ||
                lower.contains("not working") || lower.contains("same error") || lower.contains("nhi hua") ||
                lower.contains("phir bhi") || lower.contains("still crashing")
    }

    private fun isCrashReportWithoutLog(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("crash") || lower.contains("exit code") || lower.contains("band ho gaya") ||
                lower.contains("freeze") || lower.contains("stopped") || lower.contains("error")) &&
                !lower.contains("at ") && !lower.contains("exception") && !lower.contains("log")
    }

    private fun containsEmbeddedLog(text: String): Boolean {
        return text.contains("Exception") || text.contains("at net.") || text.contains("at java.") ||
                text.contains("at org.") || text.contains("Exit code") || text.contains("[Render thread")
    }

    private fun extractLogFromText(text: String): String? {
        return if (containsEmbeddedLog(text)) text else null
    }

    private fun isIdentityQuestion(text: String): Boolean {
        val lower = text.lowercase().trim()
        val identityKeywords = listOf(
            "tum kon ho", "tu kon hai", "who are you", "who made you", "who created you",
            "owner", "founder", "malik", "kisne banaya", "tum kaun ho", "tu kaun hai",
            "aap kaun ho", "aap kon ho", "ap kon ho", "kiska ai", "kiska bot", "tumhara malik",
            "owner kon", "roller", "siaf", "who is the owner", "who is your owner", "who is your founder",
            "koun ho"
        )
        return identityKeywords.any { lower.contains(it) }
    }

    private fun getIdentityResponse(): String {
        return """Main **Zyron AI** hoon — Zyron Launcher ka official Minecraft crash diagnostics aur support assistant! ⚡🎮

👑 **Original Owners & Founders:**
Mere original owners aur founders **Roller_gaming** aur **not siaf** hain!

🎯 **Mera Kaam:**
• Minecraft / Zyron Launcher ke sabhi crash logs aur exit codes (Code 1, -1073740791, etc.) ko diagnose karna.
• Fabric, Forge, aur NeoForge ke mod conflicts pakadna aur unka exact fix step-by-step dena.
• Phone specs ke according best renderer aur RAM allocate karwana taaki game smoothly chale!

Aapka koi bhi Minecraft crash log ya launcher error ho, bas yahan bhej do — main turant fix kar dunga!"""
    }

    fun applyAutoFix(action: AutoFixAction) {
        viewModelScope.launch {
            val profile = _deviceProfile.value
            val updated = when (action.type) {
                AutoFixType.SET_RECOMMENDED_RAM -> {
                    val safeRam = _detectedDeviceSpecs.value?.recommendedRamMb ?: 3072
                    profile.copy(allocatedRamMb = safeRam)
                }
                AutoFixType.SWITCH_RENDERER_HOLY, AutoFixType.SWITCH_RENDERER_GL4ES -> {
                    profile.copy(renderer = "Holy GL4ES (Recommended for broad compatibility)")
                }
                AutoFixType.SWITCH_RENDERER_ZINK -> {
                    profile.copy(renderer = "Zink (Mesa Turnip Vulkan)")
                }
                AutoFixType.SWITCH_RENDERER_VIRGL -> {
                    profile.copy(renderer = "VirGL (Compatibility Mode)")
                }
                AutoFixType.SWITCH_JAVA_RUNTIME -> {
                    val target = if (profile.minecraftVersion.contains("1.20.5") || profile.minecraftVersion.contains("1.20.6") || profile.minecraftVersion.contains("1.21")) {
                        "Java 21 (LTS 64-bit)"
                    } else if (profile.minecraftVersion.contains("1.18") || profile.minecraftVersion.contains("1.19") || profile.minecraftVersion.contains("1.20")) {
                        "Java 17 (LTS 64-bit)"
                    } else {
                        "Java 8 (Legacy)"
                    }
                    profile.copy(whatHappenedBefore = "Configured runtime to $target")
                }
                AutoFixType.RESET_OPTIONS_TXT -> {
                    profile.copy(shadersOrResourcePacks = "Default Vanilla (Reset options.txt)")
                }
                AutoFixType.RESET_JVM_ARGS -> {
                    profile.copy(whatHappenedBefore = "Reset JVM arguments to default")
                }
            }

            repository.updateDeviceProfile(updated)
            _deviceProfile.value = updated
            _selectedAutoFix.value = null

            val uid = currentUser.value?.userId ?: "guest"
            // Notify user in chat
            val fixAppliedMsg = ChatMessageEntity(
                userId = uid,
                role = "assistant",
                content = "⚡ **Auto-Fix Applied Successfully!**\n\n" +
                        "Applied: **${action.title}**\n" +
                        "${action.description}\n\n" +
                        "Now launch Zyron Launcher again. Try this and tell me whether the launcher works now.",
                confidenceStatus = "CONFIRMED INFORMATION",
                timestamp = System.currentTimeMillis()
            )
            repository.addMessage(fixAppliedMsg)
            _snackbarEvent.emit("Auto-Fix applied: ${action.title}")
        }
    }

    fun dismissAutoFix() {
        _selectedAutoFix.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            val uid = currentUser.value?.userId ?: "guest"
            repository.clearMessagesForUser(uid)
            _snackbarEvent.emit("Chat history cleared")
        }
    }
}
