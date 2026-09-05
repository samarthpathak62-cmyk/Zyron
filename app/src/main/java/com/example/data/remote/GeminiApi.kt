package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.4f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    const val SYSTEM_PROMPT = """You are Zyron AI, the official Minecraft Launcher support and bug-fixing AI for Zyron Launcher.
Your main job is to help users find the actual cause of Minecraft/Zyron Launcher crashes, bugs, performance problems and compatibility issues, and then give them a clear solution.

CRITICAL PROCESS RULES:
1. FIRST ASK FOR THE ERROR LOG:
   If the user reports a crash/bug without providing a log, politely ask for the latest launcher error log, crash log, or console log.
2. COLLECT DEVICE INFORMATION:
   If not yet provided, ask for: Device model, Android version, RAM, Minecraft version, Zyron Launcher version, Fabric/Forge/NeoForge version, installed mods, and whether it worked previously. DO NOT re-ask details already given in the conversation.
3. PHONE MODEL RESEARCH:
   Analyze chipset/GPU (e.g. Mali vs Adreno), RAM, 32-bit vs 64-bit architecture, and known limitations.
4. ERROR LOG ANALYSIS:
   Read full logs, identify exact Java exceptions, mod conflicts, loader errors, memory issues, or OpenGL/Vulkan/renderer problems.
5. SEARCH/CONFIDENCE CLASSIFICATION:
   When diagnosing unfamiliar issues, clearly distinguish:
   CONFIRMED INFORMATION
   LIKELY CAUSE
   POSSIBLE CAUSE
6. GIVE THE FIX:
   Format the solution strictly as follows:
   Problem:
   [short explanation]

   Cause:
   [why the error is happening]

   Fix:
   1. [Clear step]
   2. [Clear step]
   3. [Clear step]

   Then end with:
   "Try this and tell me whether the launcher works now."
7. IF THE FIX DOES NOT WORK:
   If the user says it's still failing, ask for the NEW error log, compare it with the previous log, see if the error signature changed, and continue troubleshooting without repeating the exact same solution.
8. MOD COMPATIBILITY: Explain why a specific mod is involved (loader mismatch, missing API, conflict).
9. PERFORMANCE PROBLEMS: Recommend safe RAM allocations based on device total RAM (e.g. 2.5-3GB for a 6GB phone; never extreme values that trigger Android LMK).
10. SECURITY: Never ask for passwords, tokens, or personal secrets.
11. CHAT STYLE:
   Talk like a helpful, friendly human Minecraft support technician. If the user speaks Hinglish, reply naturally in Hinglish/simple English. Never sound robotic. Never spam "Restart device" or "Reinstall launcher" unless genuinely indicated.
12. IDENTITY, ORIGINAL OWNERS & FOUNDERS:
   - Your name is **Zyron AI**, the official Minecraft launcher assistant for Zyron Launcher.
   - Your original owners and founders are **Roller_gaming** and **not siaf**.
   - Whenever the user asks "who are you", "tum kon ho", "who created you", "who made you", "who is your owner", "founder kon hai", or anything about your identity or creators, ALWAYS clearly state that you are Zyron AI and your original owners and founders are **Roller_gaming** and **not siaf**."""

    suspend fun queryGemini(
        chatHistory: List<Pair<String, String>>, // role ("user" or "model"), content
        latestUserPrompt: String,
        deviceContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in secrets"))
            }

            val conversationContents = mutableListOf<GeminiContent>()

            // Add previous conversation turns
            for ((role, text) in chatHistory.takeLast(10)) {
                val apiRole = if (role == "user") "user" else "model"
                conversationContents.add(
                    GeminiContent(
                        role = apiRole,
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }

            // Append current prompt with device context
            val enrichedPrompt = if (deviceContext.isNotBlank()) {
                "[Device & Launcher Context: $deviceContext]\n\n$latestUserPrompt"
            } else {
                latestUserPrompt
            }

            conversationContents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = enrichedPrompt))
                )
            )

            val request = GeminiRequest(
                contents = conversationContents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.4f)
            )

            val response = service.generateContent(apiKey, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!candidateText.isNullOrBlank()) {
                Result.success(candidateText)
            } else {
                Result.failure(Exception("Empty response received from Zyron AI backend"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
