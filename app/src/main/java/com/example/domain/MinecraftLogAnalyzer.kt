package com.example.domain

import com.example.data.model.AutoFixType
import com.example.data.model.LogAnalysisResult
import com.example.data.model.SampleCrashPreset

object MinecraftLogAnalyzer {

    /**
     * Rule 10: Redact sensitive credentials like tokens, passwords, session secrets.
     */
    fun sanitizeLog(rawLog: String): String {
        return rawLog
            .replace(Regex("(?i)(sessionToken|accessToken|token|auth_token|bearer)[=:]\\s*[\"']?[a-zA-Z0-9._\\-]{15,}[\"']?"), "$1=[REDACTED_BY_ZYRON_SECURITY]")
            .replace(Regex("(?i)(password|secret|key)[=:]\\s*[\"']?[^\\s,}\"]{6,}[\"']?"), "$1=[REDACTED_BY_ZYRON_SECURITY]")
            .replace(Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"), "uuid-xxxx-xxxx-xxxx-xxxx")
            .replace(Regex("(?i)(email)[=:]\\s*[\"']?[^\\s,}\"]+@[^\\s,}\"]+[\"']?"), "$1=[EMAIL_REDACTED]")
    }

    fun analyzeLog(
        rawLog: String,
        deviceModel: String = "",
        totalRamGb: Double = 6.0,
        mcVersion: String = "1.21.1",
        loader: String = "Fabric"
    ): LogAnalysisResult {
        val sanitized = sanitizeLog(rawLog)

        // 1. Check for Java class version mismatch (UnsupportedClassVersionError)
        if (sanitized.contains("UnsupportedClassVersionError", ignoreCase = true) ||
            sanitized.contains("has been compiled by a more recent version of the Java Runtime", ignoreCase = true) ||
            sanitized.contains("class file version 65.0", ignoreCase = true) ||
            sanitized.contains("class file version 61.0", ignoreCase = true)
        ) {
            val needsJava21 = sanitized.contains("65.0") || mcVersion.contains("1.20.5") || mcVersion.contains("1.20.6") || mcVersion.contains("1.21")
            val targetJava = if (needsJava21) "Java 21" else "Java 17"
            return LogAnalysisResult(
                hasLog = true,
                exceptionType = "java.lang.UnsupportedClassVersionError",
                keyErrorMessage = "Class file version mismatch: Current Java Runtime is older than required by Minecraft $mcVersion",
                confidenceStatus = "CONFIRMED INFORMATION",
                problem = "Minecraft $mcVersion crashed during launch because of a Java Runtime version mismatch.",
                cause = "Minecraft $mcVersion requires $targetJava (class version ${if (needsJava21) "65.0" else "61.0"}), but Zyron Launcher is currently launching the game with an older Java version (such as Java 8 or Java 17).",
                fixSteps = listOf(
                    "Open Zyron Launcher Settings > Java Settings.",
                    "Tap on 'Runtime Version' and select '$targetJava' (e.g. OpenJDK 21 LTS).",
                    "If $targetJava is not installed in Zyron, tap 'Download Runtime' and install $targetJava.",
                    "Return to the Minecraft profile and verify the selected Java is set to $targetJava, then launch again."
                ),
                autoFixAvailable = true,
                autoFixTitle = "Switch Java Runtime to $targetJava",
                autoFixDescription = "Sets Zyron Launcher configuration to use $targetJava for profile $mcVersion.",
                isDestructive = false
            )
        }

        // 2. Check for Missing Fabric API or Mod Dependencies
        if (sanitized.contains("net.fabricmc.loader.impl.FormattedException", ignoreCase = true) &&
            (sanitized.contains("requires", ignoreCase = true) || sanitized.contains("missing", ignoreCase = true))
        ) {
            val missingPart = Regex("Mod '(.+?)' requires (version [^ ]+ of '(.+?)')")
                .find(sanitized)?.value
                ?: "A mod requires missing dependencies or Fabric API."

            return LogAnalysisResult(
                hasLog = true,
                exceptionType = "net.fabricmc.loader.impl.FormattedException",
                keyErrorMessage = missingPart,
                confidenceStatus = "CONFIRMED INFORMATION",
                problem = "Fabric loader stopped launch because required mod dependencies are missing.",
                cause = "One or more installed mods require other library mods (such as Fabric API, Cloth Config, or Indium) that are not in your .minecraft/mods folder.",
                fixSteps = listOf(
                    "Download the matching 'Fabric API' jar for Minecraft $mcVersion from Modrinth or CurseForge.",
                    "Place the downloaded .jar file inside your Zyron Launcher mods directory (usually under /Android/data/.../files/.minecraft/mods).",
                    "Check if any of your mods requires 'Indium' (required when using Sodium with Iris or block rendering mods).",
                    "Ensure all mods match Minecraft version $mcVersion exactly."
                ),
                autoFixAvailable = false,
                isDestructive = false
            )
        }

        // 3. Check for Incompatible Mod Set (e.g. OptiFine + Sodium conflict)
        if (sanitized.contains("Incompatible mod set", ignoreCase = true) ||
            (sanitized.contains("OptiFine", ignoreCase = true) && sanitized.contains("Sodium", ignoreCase = true)) ||
            sanitized.contains("DuplicateModsFoundException", ignoreCase = true)
        ) {
            val isDuplicate = sanitized.contains("DuplicateModsFoundException", ignoreCase = true)
            return LogAnalysisResult(
                hasLog = true,
                exceptionType = if (isDuplicate) "DuplicateModsFoundException" else "ModConflictException",
                keyErrorMessage = if (isDuplicate) "Duplicate mod files discovered in mods folder" else "Incompatible mod combination detected",
                confidenceStatus = "CONFIRMED INFORMATION",
                problem = if (isDuplicate) "Multiple versions of the same mod detected." else "Incompatible mod combination crashed the loader.",
                cause = if (isDuplicate) "You have two or more versions of the same mod file in your mods folder." else "OptiFine and Sodium/Iris cannot run together; they rewrite the same rendering pipeline.",
                fixSteps = listOf(
                    "Open Zyron Launcher Mod Manager or a file explorer.",
                    "If you have OptiFine and Sodium together, remove OptiFine. Keep Sodium and Iris for better FPS and shader support on mobile.",
                    "If duplicate files exist (e.g., sodium-0.5.8.jar and sodium-0.5.11.jar), delete the older file.",
                    "Restart the profile."
                ),
                autoFixAvailable = false,
                isDestructive = false
            )
        }

        // 4. Check for Out of Memory Error (OOM)
        if (sanitized.contains("OutOfMemoryError", ignoreCase = true) ||
            sanitized.contains("Java heap space", ignoreCase = true) ||
            sanitized.contains("Exit code: 137", ignoreCase = true) ||
            sanitized.contains("Killed: 9", ignoreCase = true)
        ) {
            val safeRam = when {
                totalRamGb <= 4.0 -> "2048 MB (2.0 GB)"
                totalRamGb <= 6.0 -> "3072 MB (3.0 GB)"
                else -> "4096 MB (4.0 GB)"
            }
            return LogAnalysisResult(
                hasLog = true,
                exceptionType = "java.lang.OutOfMemoryError: Java heap space",
                keyErrorMessage = "JVM allocated memory was exhausted or terminated by Android LMK",
                confidenceStatus = "CONFIRMED INFORMATION",
                problem = "Minecraft ran out of memory (Java heap space exhaustion).",
                cause = "The allocated RAM was either set too low for Minecraft $mcVersion with your installed mods, or set too high causing Android's Low Memory Killer (LMK) to terminate the app.",
                fixSteps = listOf(
                    "Open Zyron Launcher Settings > Memory / RAM Allocation.",
                    "Set RAM allocation to $safeRam. On your ${totalRamGb.toInt()}GB device, allocating more than ${(totalRamGb - 2.0).coerceAtLeast(2.0)}GB causes Android system crashes.",
                    "Lower in-game Render Distance to 6-8 chunks in Video Settings.",
                    "Remove heavy 128x/256x resource packs which consume extreme amounts of GPU memory."
                ),
                autoFixAvailable = true,
                autoFixTitle = "Set Safe RAM Allocation ($safeRam)",
                autoFixDescription = "Configures Zyron Launcher RAM slider to $safeRam.",
                isDestructive = false
            )
        }

        // 5. Check for OpenGL / GLFW / Renderer Crash
        if (sanitized.contains("LWJGLException", ignoreCase = true) ||
            sanitized.contains("GLFW error 65542", ignoreCase = true) ||
            sanitized.contains("Failed to create GLFW window", ignoreCase = true) ||
            sanitized.contains("Could not init GLX", ignoreCase = true) ||
            sanitized.contains("EGL_BAD_ALLOC", ignoreCase = true) ||
            sanitized.contains("vkCreateInstance", ignoreCase = true)
        ) {
            return LogAnalysisResult(
                hasLog = true,
                exceptionType = "org.lwjgl.LWJGLException / GLFW Error",
                keyErrorMessage = "OpenGL surface initialization failed with graphic driver",
                confidenceStatus = "LIKELY CAUSE",
                problem = "The graphics renderer crashed when initializing the Minecraft game window.",
                cause = "Your mobile GPU driver failed with the current Zyron graphic renderer (e.g. Zink Vulkan or VirGL). Mali GPUs or certain Android 13/14 driver updates are incompatible with Zink.",
                fixSteps = listOf(
                    "Open Zyron Launcher Settings > Video & Renderer.",
                    "Change the Renderer to 'Holy GL4ES' (most stable on Android) or 'GL4ES 1.1.5'.",
                    "Disable 'Vulkan Zink' if you are on a Mali GPU device (MediaTek/Exynos/Kirin).",
                    "Turn off shaders temporarily if you have shaderpacks enabled."
                ),
                autoFixAvailable = true,
                autoFixTitle = "Switch Renderer to Holy GL4ES",
                autoFixDescription = "Changes active graphic renderer to Holy GL4ES for broad OpenGL compatibility.",
                isDestructive = false
            )
        }

        // 6. Corrupted options.txt or Resource Pack Crash
        if (sanitized.contains("Rendering overlay", ignoreCase = true) ||
            sanitized.contains("options.txt", ignoreCase = true) ||
            (sanitized.contains("NullPointerException", ignoreCase = true) && sanitized.contains("splash", ignoreCase = true))
        ) {
            return LogAnalysisResult(
                hasLog = true,
                exceptionType = "java.lang.NullPointerException: Rendering overlay",
                keyErrorMessage = "Corrupted video options or resource pack crash during splash screen",
                confidenceStatus = "LIKELY CAUSE",
                problem = "Minecraft crashed while loading the Mojang splash screen overlay.",
                cause = "A corrupted 'options.txt' or an incompatible GUI resource pack/custom font caused an unhandled NullPointerException.",
                fixSteps = listOf(
                    "Reset 'options.txt' to generate fresh vanilla graphic settings.",
                    "Ensure no active resource packs are forcing incompatible shaders.",
                    "Relaunch Zyron Launcher."
                ),
                autoFixAvailable = true,
                autoFixTitle = "Reset options.txt (Safe Graphic Reset)",
                autoFixDescription = "Resets graphical options back to default without affecting any worlds or saves.",
                isDestructive = false
            )
        }

        // 7. Generic Exit Code 1 / Unknown Log
        return LogAnalysisResult(
            hasLog = true,
            exceptionType = "Process crashed with Exit Code 1",
            keyErrorMessage = "Minecraft process terminated unexpectedly",
            confidenceStatus = "POSSIBLE CAUSE",
            problem = "Minecraft closed unexpectedly (Exit Code 1).",
            cause = "A mod or configuration error interrupted startup. More detailed stack trace lines above the crash are needed to pinpoint the exact class.",
            fixSteps = listOf(
                "Verify that your Java Runtime matches Minecraft $mcVersion (Java 21 for 1.20.5+, Java 17 for 1.18-1.20.4, Java 8 for 1.12.2).",
                "Try disabling the last mod you installed before the crashes started.",
                "In Zyron Launcher Settings, reset JVM Arguments to default.",
                "Send the full latest log if the problem persists."
            ),
            autoFixAvailable = true,
            autoFixTitle = "Reset JVM Arguments to Default",
            autoFixDescription = "Removes any custom JVM flags that might cause early JVM termination.",
            isDestructive = false
        )
    }

    /**
     * Rule 7: Compare new log with previous log.
     */
    fun compareLogs(oldLog: String, newLog: String): String {
        val oldSanitized = sanitizeLog(oldLog)
        val newSanitized = sanitizeLog(newLog)

        val oldHasJavaErr = oldSanitized.contains("UnsupportedClassVersionError")
        val newHasJavaErr = newSanitized.contains("UnsupportedClassVersionError")

        val oldHasOom = oldSanitized.contains("OutOfMemoryError")
        val newHasOom = newSanitized.contains("OutOfMemoryError")

        val oldHasGlfw = oldSanitized.contains("GLFW") || oldSanitized.contains("LWJGL")
        val newHasGlfw = newSanitized.contains("GLFW") || newSanitized.contains("LWJGL")

        val oldHasFabric = oldSanitized.contains("FormattedException")
        val newHasFabric = newSanitized.contains("FormattedException")

        return buildString {
            append("🔍 **Log Comparison Analysis**:\n")
            if (oldHasJavaErr && !newHasJavaErr) {
                append("✅ The previous Java version mismatch error has been RESOLVED!\n")
            }
            if (oldHasOom && !newHasOom) {
                append("✅ The Out Of Memory (OOM) error disappeared!\n")
            }
            if (oldHasGlfw && !newHasGlfw) {
                append("✅ The previous OpenGL window creation error is gone!\n")
            }
            if (oldHasFabric && !newHasFabric) {
                append("✅ The previous Fabric dependency error was fixed!\n")
            }

            if (newSanitized == oldSanitized || (newHasJavaErr && oldHasJavaErr) || (newHasGlfw && oldHasGlfw)) {
                append("⚠️ The new log shows the **same error signature** as the previous log. The setting change may not have been saved or applied to this profile.\n")
            } else {
                append("ℹ️ A new error signature has appeared. Let's inspect the fresh stack trace.\n")
            }
        }
    }

    val SAMPLE_PRESETS = listOf(
        SampleCrashPreset(
            id = "fabric_missing_api",
            title = "Missing Fabric API Dependency",
            subtitle = "Exit code 1 - Mod requires Fabric API 0.100+",
            tag = "Fabric",
            mcVersion = "1.21.1",
            loader = "Fabric",
            fullLog = """
[Zyron Launcher Log] Starting Minecraft 1.21.1 with Fabric Loader 0.16.5
[10:14:22] [main/INFO]: Loading Minecraft 1.21.1 with Fabric Loader 0.16.5
[10:14:23] [main/ERROR]: Minecraft has crashed!
net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
A potential solution has been determined:
	 - Mod 'sodium' (0.5.11) requires version >=0.102.0 of 'fabric-api', which is missing!
	 - Mod 'iris' (1.7.3) requires version >=0.102.0 of 'fabric-api', which is missing!
	at net.fabricmc.loader.impl.FabricLoaderImpl.load(FabricLoaderImpl.java:234)
	at net.fabricmc.loader.impl.launch.knot.Knot.init(Knot.java:146)
	at net.fabricmc.loader.impl.launch.knot.Knot.run(Knot.java:82)
	at net.fabricmc.loader.impl.launch.knot.KnotClient.main(KnotClient.java:23)
Process crashed with exit code 1
            """.trimIndent()
        ),
        SampleCrashPreset(
            id = "java_class_version",
            title = "Java 21 vs Java 17 Incompatibility",
            subtitle = "UnsupportedClassVersionError: class file version 65.0",
            tag = "Java",
            mcVersion = "1.21",
            loader = "Vanilla",
            fullLog = """
[Zyron Launcher Log] Starting JVM for Minecraft 1.21
[10:15:02] [main/INFO]: Java version: 17.0.8, vendor: Eclipse Adoptium
[10:15:03] [main/ERROR]: Exception in thread "main" java.lang.UnsupportedClassVersionError: net/minecraft/client/main/Main has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 61.0
	at java.lang.ClassLoader.defineClass1(Native Method)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:1017)
	at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:150)
	at java.net.URLClassLoader.defineClass(URLClassLoader.java:526)
	at java.net.URLClassLoader.access${'$'}100(URLClassLoader.java:86)
Process crashed with exit code 1
            """.trimIndent()
        ),
        SampleCrashPreset(
            id = "oom_heap_space",
            title = "Out of Memory (Heap Space)",
            subtitle = "java.lang.OutOfMemoryError: Java heap space",
            tag = "Memory",
            mcVersion = "1.20.4",
            loader = "Fabric",
            fullLog = """
[Zyron Launcher Log] Profile: 1.20.4 with 45 mods
[10:18:11] [main/INFO]: Allocated memory: 1024MB (-Xmx1024M)
[10:18:45] [Render thread/ERROR]: OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
	at net.minecraft.client.renderer.texture.TextureAtlas.method_18165(TextureAtlas.java:142)
	at net.minecraft.client.renderer.texture.TextureAtlas.stitch(TextureAtlas.java:89)
	at net.minecraft.client.renderer.texture.SpriteLoader.loadAndStitch(SpriteLoader.java:62)
	at net.minecraft.client.MinecraftClient.reloadResources(MinecraftClient.java:2140)
Process terminated with signal 9 (SIGKILL by Android Low Memory Killer)
            """.trimIndent()
        ),
        SampleCrashPreset(
            id = "glfw_renderer_mali",
            title = "OpenGL / Zink Window Creation Crash",
            subtitle = "GLFW error 65542: Failed to create GLFW window on Mali GPU",
            tag = "Renderer",
            mcVersion = "1.20.1",
            loader = "Fabric",
            fullLog = """
[Zyron Launcher Log] Renderer: Zink (Turnip Vulkan)
[10:20:00] [main/INFO]: Initializing LWJGL 3.3.3
[10:20:01] [main/ERROR]: [LWJGL] GLFW_PLATFORM_ERROR: Failed to create window
[10:20:01] [main/ERROR]: GLFW error 65542: WGL: The driver does not appear to support OpenGL
org.lwjgl.LWJGLException: Could not init GLX
	at org.lwjgl.glfw.GLFW.glfwCreateWindow(Native Method)
	at com.mojang.blaze3d.platform.Window.<init>(Window.java:128)
	at net.minecraft.client.Minecraft.init(Minecraft.java:512)
Process crashed with exit code 255
            """.trimIndent()
        ),
        SampleCrashPreset(
            id = "options_txt_corrupted",
            title = "Mojang Splash Screen NullPointerException",
            subtitle = "Crash while rendering overlay / corrupted options.txt",
            tag = "Config",
            mcVersion = "1.20.2",
            loader = "Fabric",
            fullLog = """
[Zyron Launcher Log] Loading Minecraft 1.20.2
[10:22:15] [Render thread/FATAL]: Crash report saved to .minecraft/crash-reports/crash-2026-09-04_10.22.15-client.txt
java.lang.NullPointerException: Rendering overlay
	at net.minecraft.client.gui.screen.SplashOverlay.render(SplashOverlay.java:152)
	at net.minecraft.client.render.GameRenderer.render(GameRenderer.java:914)
	at net.minecraft.client.MinecraftClient.render(MinecraftClient.java:1240)
	at net.minecraft.client.MinecraftClient.run(MinecraftClient.java:820)
Process crashed with exit code 1
            """.trimIndent()
        )
    )
}
