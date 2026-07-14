package com.example.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class EshaCommandResponse(
    val intent: String,
    val targetDevice: String, // "Phone", "Laptop", "Both"
    val riskTier: String, // "Low", "Medium", "High"
    val responseText: String,
    val technicalLogs: String,
    val entities: Map<String, String>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun parseCommand(command: String): EshaCommandResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackResponse(command, "API Key is missing or invalid. Please configure GEMINI_API_KEY in the Secrets panel.")
        }

        val systemPrompt = """
            You are ESHA, the advanced AI brain for an Android + Linux companion control suite.
            Your task is to parse a natural language command and output a strictly valid JSON object matching the schema.
            
            Schema keys:
            - "intent": The identified goal (e.g., "send_message", "play_music", "open_camera", "window_mgmt", "dangerous_command", "git_commit", "unknown")
            - "targetDevice": "Phone", "Laptop", or "Both"
            - "riskTier": "Low", "Medium", or "High"
            - "responseText": What you (ESHA) would speak back to the user in a natural, cool, assistant tone.
            - "technicalLogs": Detailed multi-line string representing the command execution pipeline. Include the exact ADB commands, shell scripts, or Linux commands sent over the IP bridge.
            - "entities": A flat string-to-string dictionary of extracted key-value pairs (e.g., {"contact": "mom", "content": "I am safe", "app": "whatsapp"}).

            Risk Tier Guidelines:
            - Low: general queries, opening apps, checking weather, play music.
            - Medium: sending messages, making calls, triggering camera, reading notifications.
            - High: terminal root/sudo commands, destructive actions, file deletes, wiping data, system shut downs.

            Provide realistic and authentic technical details in the technicalLogs. E.g. for phone SMS: "adb shell service call phone..." or "adb shell am start -a android.intent.action.SENDTO..."
            For Linux laptop: "ssh -t user@host 'wmctrl -c :ACTIVE:'" or "xdotool key Super+d".
            For dangerous command, warn about risks and show the commands with red-alert safety tags.
            
            Output ONLY valid JSON. No markdown backticks, no other text outside the JSON.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Parse this user command: \"$command\"")))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse EshaCommandResponse
                val adapter = moshi.adapter(EshaCommandResponse::class.java)
                adapter.fromJson(jsonText) ?: getFallbackResponse(command, "Failed to decode ESHA Brain response.")
            } else {
                getFallbackResponse(command, "ESHA Brain returned an empty response.")
            }
        } catch (e: Exception) {
            getFallbackResponse(command, "ESHA Communication Error: ${e.localizedMessage}")
        }
    }

    private fun getFallbackResponse(command: String, errorMsg: String): EshaCommandResponse {
        val lowercaseCmd = command.lowercase()
        val intent: String
        val targetDevice: String
        val riskTier: String
        val responseText: String
        val technicalLogs: String
        val entities = mutableMapOf<String, String>()

        if (lowercaseCmd.contains("rm ") || lowercaseCmd.contains("sudo") || lowercaseCmd.contains("wipe") || lowercaseCmd.contains("delete")) {
            intent = "dangerous_command"
            targetDevice = if (lowercaseCmd.contains("phone")) "Phone" else "Laptop"
            riskTier = "High"
            responseText = "Warning: This command has been flagged as High Risk. Authorization is strictly required."
            technicalLogs = "CRITICAL RISK BLOCK\nCommand: $command\nAction: Intercepted before execution.\nRequires: Admin biometric approval."
        } else if (lowercaseCmd.contains("message") || lowercaseCmd.contains("text") || lowercaseCmd.contains("whatsapp") || lowercaseCmd.contains("sms")) {
            intent = "send_message"
            targetDevice = "Phone"
            riskTier = "Medium"
            responseText = "Processing local request to dispatch a message."
            technicalLogs = "ADB Connection: Established\nSending broadcast intent:\nadb shell am start -a android.intent.action.SENDTO -d sms: --es sms_body \"$command\""
            entities["type"] = "SMS"
        } else if (lowercaseCmd.contains("spotify") || lowercaseCmd.contains("play") || lowercaseCmd.contains("music")) {
            intent = "play_music"
            targetDevice = "Phone"
            riskTier = "Low"
            responseText = "Command dispatched to music client on the phone."
            technicalLogs = "ADB Connection: Connected\nadb shell am start -a android.media.action.MEDIA_PLAY_FROM_SEARCH -e query \"music\""
        } else if (lowercaseCmd.contains("window") || lowercaseCmd.contains("vscode") || lowercaseCmd.contains("chrome") || lowercaseCmd.contains("laptop")) {
            intent = "window_mgmt"
            targetDevice = "Laptop"
            riskTier = "Low"
            responseText = "Laptop window manager command executed."
            technicalLogs = "Establishing SSH bridge over tailscale...\nExecuting: xdotool key alt+tab or wmctrl actions"
        } else {
            intent = "unknown"
            targetDevice = "Both"
            riskTier = "Low"
            responseText = "Offline fallback engine: Command '$command' registered. $errorMsg"
            technicalLogs = "Local Parser Run:\nInput: $command\nStatus: Fallback simulation active."
        }

        return EshaCommandResponse(
            intent = intent,
            targetDevice = targetDevice,
            riskTier = riskTier,
            responseText = responseText,
            technicalLogs = technicalLogs,
            entities = entities
        )
    }
}
