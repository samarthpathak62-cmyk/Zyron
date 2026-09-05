package com.example.domain

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.model.DeviceSpecs
import java.io.File
import kotlin.math.roundToInt

object DeviceSpecsDetector {

    fun detectSpecs(context: Context): DeviceSpecs {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availRamGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)

        val freeStorageGb = getAvailableInternalMemorySize()

        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

        val model = Build.MODEL ?: "Unknown Device"
        val manufacturer = Build.MANUFACTURER ?: "Android"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        // Calculate safe recommended RAM allocation
        val recommendedRamMb = when {
            totalRamGb <= 3.2 -> 1536 // 3GB devices
            totalRamGb <= 4.3 -> 2048 // 4GB devices: 2GB safe
            totalRamGb <= 6.3 -> 3072 // 6GB devices: 3GB safe
            totalRamGb <= 8.3 -> 4096 // 8GB devices: 4GB safe
            else -> 4608 // 12GB+ devices: 4.5GB safe
        }

        // Hardware heuristics & renderer recommendations
        val hardwareLower = (Build.HARDWARE + " " + Build.BOARD + " " + Build.SOC_MODEL).lowercase()
        val isMali = hardwareLower.contains("mali") || hardwareLower.contains("mt") ||
                hardwareLower.contains("helio") || hardwareLower.contains("dimensity") ||
                hardwareLower.contains("exynos") || hardwareLower.contains("kirin")
        val isAdreno = hardwareLower.contains("qcom") || hardwareLower.contains("qualcomm") ||
                hardwareLower.contains("snapdragon") || hardwareLower.contains("adreno")

        val recommendedRenderer = when {
            isMali -> "Holy GL4ES (Mali Optimized, avoids Zink shader crash)"
            isAdreno && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> "Zink (Mesa Turnip Vulkan) or Holy GL4ES"
            isAdreno -> "Holy GL4ES 1.1.5"
            else -> "Holy GL4ES (Universal Compatibility)"
        }

        val knownLimitations = buildString {
            if (!is64Bit) {
                append("⚠️ 32-bit Architecture detected. Modern Minecraft 1.18+ and Java 17/21 cannot run on 32-bit. 1.16.5 is the highest recommended version. ")
            }
            if (totalRamGb <= 4.0) {
                append("⚠️ 4GB or less RAM. High-res resource packs and heavy modpacks (>50 mods) will trigger Android LMK (Low Memory Killer). Keep render distance at 6-8 chunks. ")
            }
            if (isMali) {
                append("ℹ️ Mali GPU detected. Mali drivers do not support full Vulkan extensions required by Turnip Zink. Stick to Holy GL4ES or ANGLE to prevent black screens. ")
            }
            if (Build.VERSION.SDK_INT >= 30) {
                append("ℹ️ Scoped Storage on Android 11+ requires granting storage/files management permission for Zyron Launcher .minecraft access. ")
            }
        }.ifEmpty { "No severe device hardware bottlenecks detected. 64-bit modern SoC." }

        return DeviceSpecs(
            model = model,
            manufacturer = manufacturer.replaceFirstChar { it.uppercase() },
            androidVersion = androidVersion,
            sdkInt = Build.VERSION.SDK_INT,
            totalRamGb = (totalRamGb * 10.0).roundToInt() / 10.0,
            availableRamGb = (availRamGb * 10.0).roundToInt() / 10.0,
            architecture = "$primaryAbi (${if (is64Bit) "64-bit" else "32-bit"})",
            freeStorageGb = (freeStorageGb * 10.0).roundToInt() / 10.0,
            is64Bit = is64Bit,
            recommendedRamMb = recommendedRamMb,
            recommendedRenderer = recommendedRenderer,
            knownLimitations = knownLimitations
        )
    }

    private fun getAvailableInternalMemorySize(): Double {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {
            10.0
        }
    }
}
