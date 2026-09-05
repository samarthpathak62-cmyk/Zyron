package com.example.data.model

data class DeviceSpecs(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val totalRamGb: Double,
    val availableRamGb: Double,
    val architecture: String,
    val freeStorageGb: Double,
    val is64Bit: Boolean,
    val recommendedRamMb: Int,
    val recommendedRenderer: String,
    val knownLimitations: String
)

data class LogAnalysisResult(
    val hasLog: Boolean,
    val exceptionType: String? = null,
    val keyErrorMessage: String? = null,
    val involvedMods: List<String> = emptyList(),
    val confidenceStatus: String, // "CONFIRMED INFORMATION", "LIKELY CAUSE", "POSSIBLE CAUSE"
    val problem: String,
    val cause: String,
    val fixSteps: List<String>,
    val autoFixAvailable: Boolean = false,
    val autoFixTitle: String? = null,
    val autoFixDescription: String? = null,
    val isDestructive: Boolean = false,
    val followupPrompt: String = "Try this and tell me whether the launcher works now."
)

enum class AutoFixType {
    RESET_JVM_ARGS,
    SWITCH_RENDERER_GL4ES,
    SWITCH_RENDERER_HOLY,
    SWITCH_RENDERER_ZINK,
    SWITCH_RENDERER_VIRGL,
    SET_RECOMMENDED_RAM,
    RESET_OPTIONS_TXT,
    SWITCH_JAVA_RUNTIME
}

data class AutoFixAction(
    val id: String,
    val title: String,
    val description: String,
    val type: AutoFixType,
    val isDestructive: Boolean,
    val confirmationWarning: String? = null,
    val targetParameter: String? = null
)

data class SampleCrashPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val mcVersion: String,
    val loader: String,
    val fullLog: String
)
