package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.api.GeminiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EshaRepository(private val context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "esha_control_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    val profileDao = db.userProfileDao()
    val commandDao = db.commandHistoryDao()

    val allProfiles: Flow<List<UserProfile>> = profileDao.getAllProfilesFlow()
    val allHistory: Flow<List<CommandHistory>> = commandDao.getAllHistoryFlow()

    // ADB Connection Settings
    private val _adbIp = MutableStateFlow("192.168.1.104")
    val adbIp: StateFlow<String> = _adbIp.asStateFlow()

    private val _adbPort = MutableStateFlow("5555")
    val adbPort: StateFlow<String> = _adbPort.asStateFlow()

    private val _adbConnected = MutableStateFlow(true) // Start connected in simulator
    val adbConnected: StateFlow<Boolean> = _adbConnected.asStateFlow()

    private val _watchdogActive = MutableStateFlow(true)
    val watchdogActive: StateFlow<Boolean> = _watchdogActive.asStateFlow()

    // Session / Auth Gate state
    private val _isUserAuthenticated = MutableStateFlow(false)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _resolvedProfile = MutableStateFlow<UserProfile?>(null)
    val resolvedProfile: StateFlow<UserProfile?> = _resolvedProfile.asStateFlow()

    // Login Gate Step (1: Face Scan, 2: Fingerprint, 3: Voice Phrase Verification, 4: Unlocked)
    private val _loginStep = MutableStateFlow(1)
    val loginStep: StateFlow<Int> = _loginStep.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        repositoryScope.launch {
            try {
                val existing = profileDao.getAllProfilesFlow().first()
                if (existing.isEmpty()) {
                    profileDao.insertProfile(
                        UserProfile(
                            id = "admin",
                            name = "Swami (You)",
                            role = "Admin",
                            isFaceEnrolled = true,
                            isFingerprintEnrolled = true,
                            isVoiceEnrolled = true,
                            enrolledVoicePhrase = "Hey ESHA, it's Swami"
                        )
                    )
                    profileDao.insertProfile(
                        UserProfile(
                            id = "family",
                            name = "Sarah (Sister)",
                            role = "Family Member",
                            isFaceEnrolled = true,
                            isFingerprintEnrolled = true,
                            isVoiceEnrolled = false,
                            enrolledVoicePhrase = "Hey ESHA, Sarah here"
                        )
                    )
                    profileDao.insertProfile(
                        UserProfile(
                            id = "guest",
                            name = "Guest User",
                            role = "Guest",
                            isFaceEnrolled = true,
                            isFingerprintEnrolled = false,
                            isVoiceEnrolled = false,
                            enrolledVoicePhrase = "Hey ESHA, visitor"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAdbIp(ip: String) { _adbIp.value = ip }
    fun setAdbPort(port: String) { _adbPort.value = port }
    fun setAdbConnected(connected: Boolean) { _adbConnected.value = connected }
    fun setWatchdogActive(active: Boolean) { _watchdogActive.value = active }

    fun setLoginStep(step: Int) {
        _loginStep.value = step
    }

    fun setResolvedProfile(profile: UserProfile?) {
        _resolvedProfile.value = profile
    }

    fun setAuthenticated(authenticated: Boolean) {
        _isUserAuthenticated.value = authenticated
        if (authenticated) {
            _loginStep.value = 4
        } else {
            _loginStep.value = 1
            _resolvedProfile.value = null
        }
    }

    suspend fun executeCommand(inputText: String): CommandHistory {
        val currentRole = _resolvedProfile.value?.role ?: "Guest"
        
        // Call Gemini API (or fallback)
        val apiResponse = GeminiClient.parseCommand(inputText)

        // Risk check
        val status = if (apiResponse.riskTier == "High" && currentRole != "Admin") {
            "Unauthorized"
        } else {
            "Success"
        }

        val historyEntry = CommandHistory(
            inputText = inputText,
            parsedIntent = apiResponse.intent,
            targetDevice = apiResponse.targetDevice,
            riskTier = apiResponse.riskTier,
            executionStatus = status,
            responseText = if (status == "Unauthorized") {
                "Command Rejected: Role '$currentRole' lacks permission to run high-risk commands."
            } else {
                apiResponse.responseText
            },
            technicalLogs = if (status == "Unauthorized") {
                "SECURITY BLOCK\nTarget: ${apiResponse.targetDevice}\nIntent: ${apiResponse.intent}\nRisk Tier: High\nRequired Role: Admin\nResolved Role: $currentRole\nAction: Terminated with exit code 403."
            } else {
                apiResponse.technicalLogs
            }
        )

        commandDao.insertLog(historyEntry)
        return historyEntry
    }

    suspend fun clearLogs() {
        commandDao.clearHistory()
    }
}
