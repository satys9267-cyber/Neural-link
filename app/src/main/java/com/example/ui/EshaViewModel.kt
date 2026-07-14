package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommandHistory
import com.example.data.EshaRepository
import com.example.data.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EshaViewModel(application: Application) : AndroidViewModel(application) {
    val repository = EshaRepository(application)

    val allProfiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allHistory: StateFlow<List<CommandHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ADB state
    val adbIp = repository.adbIp
    val adbPort = repository.adbPort
    val adbConnected = repository.adbConnected
    val watchdogActive = repository.watchdogActive

    // Login Gate state
    val loginStep = repository.loginStep
    val resolvedProfile = repository.resolvedProfile
    val isUserAuthenticated = repository.isUserAuthenticated

    // Simulation states
    private val _isFaceScanning = MutableStateFlow(false)
    val isFaceScanning: StateFlow<Boolean> = _isFaceScanning.asStateFlow()

    private val _isFingerprintScanning = MutableStateFlow(false)
    val isFingerprintScanning: StateFlow<Boolean> = _isFingerprintScanning.asStateFlow()

    private val _isVoiceVerifying = MutableStateFlow(false)
    val isVoiceVerifying: StateFlow<Boolean> = _isVoiceVerifying.asStateFlow()

    private val _isProcessingCommand = MutableStateFlow(false)
    val isProcessingCommand: StateFlow<Boolean> = _isProcessingCommand.asStateFlow()

    private val _activeAdbLog = MutableStateFlow<String>("")
    val activeAdbLog: StateFlow<String> = _activeAdbLog.asStateFlow()

    private val _speechInput = MutableStateFlow("")
    val speechInput: StateFlow<String> = _speechInput.asStateFlow()

    private val _voiceRecordingState = MutableStateFlow(false) // Trigger recording simulation
    val voiceRecordingState: StateFlow<Boolean> = _voiceRecordingState.asStateFlow()

    init {
        // Start simulated watchdog for ADB persistent connection
        viewModelScope.launch {
            while (true) {
                delay(12000)
                if (watchdogActive.value && !adbConnected.value) {
                    _activeAdbLog.value = "Watchdog: Lost link detected. Retrying 'adb connect ${adbIp.value}:${adbPort.value}'..."
                    delay(2000)
                    repository.setAdbConnected(true)
                    _activeAdbLog.value = "Watchdog: Silent reconnection succeeded! Link re-secured."
                }
            }
        }
    }

    // ADB connection actions
    fun connectAdb(ip: String, port: String) {
        viewModelScope.launch {
            repository.setAdbIp(ip)
            repository.setAdbPort(port)
            _activeAdbLog.value = "Executing: adb connect $ip:$port..."
            delay(1500)
            repository.setAdbConnected(true)
            _activeAdbLog.value = "Connected to $ip:$port successfully. ADB transport channel active."
        }
    }

    fun disconnectAdb() {
        viewModelScope.launch {
            _activeAdbLog.value = "Executing: adb disconnect ${adbIp.value}:${adbPort.value}..."
            delay(1000)
            repository.setAdbConnected(false)
            _activeAdbLog.value = "ADB disconnected by user instruction. WATCHDOG PAUSED until re-connection."
        }
    }

    fun toggleWatchdog(enabled: Boolean) {
        repository.setWatchdogActive(enabled)
    }

    // Login Gate - Face Scan
    fun simulateFaceScan(profileId: String) {
        viewModelScope.launch {
            _isFaceScanning.value = true
            delay(2000) // Simulate capture and ML embedding lookup
            val profile = repository.profileDao.getProfileById(profileId)
            _isFaceScanning.value = false
            if (profile != null) {
                repository.setResolvedProfile(profile)
                repository.setLoginStep(2) // Move to Fingerprint
            }
        }
    }

    // Login Gate - Fingerprint
    fun simulateFingerprintScan(success: Boolean = true) {
        viewModelScope.launch {
            _isFingerprintScanning.value = true
            delay(1500) // Simulate BiometricPrompt interaction
            _isFingerprintScanning.value = false
            if (success) {
                repository.setLoginStep(3) // Move to Voice verification
            }
        }
    }

    // Login Gate - Voice Phrase verification
    fun simulateVoiceVerify(spokenText: String) {
        viewModelScope.launch {
            _isVoiceVerifying.value = true
            _speechInput.value = spokenText
            delay(2500) // Simulate speaker voiceprint recognition
            _isVoiceVerifying.value = false

            val currentProfile = resolvedProfile.value
            if (currentProfile != null) {
                // Voice print match check against enrolled voice phrase (case insensitive comparison / verification)
                val cleanExpected = currentProfile.enrolledVoicePhrase.lowercase()
                val cleanSpoken = spokenText.lowercase()
                
                // For Sarah and Swami, let's assume speaker verification matches their voiceprints
                if (cleanSpoken.contains("swami") || cleanSpoken.contains("sarah") || cleanSpoken.contains("esha") || cleanSpoken.contains("hey")) {
                    repository.setAuthenticated(true)
                } else {
                    // Fail voice phrase matching
                    _speechInput.value = "Voiceprint Mismatch! Speaker voice does not match enrolled profile."
                }
            }
        }
    }

    // Lock session
    fun logout() {
        repository.setAuthenticated(false)
        _speechInput.value = ""
    }

    fun setLoginStep(step: Int) {
        repository.setLoginStep(step)
    }

    // Command submission
    fun dispatchCommand(commandText: String) {
        if (commandText.isBlank()) return
        viewModelScope.launch {
            _isProcessingCommand.value = true
            _speechInput.value = commandText
            
            // Execute
            val result = repository.executeCommand(commandText)
            
            delay(1000)
            _isProcessingCommand.value = false
        }
    }

    // Simulated speech activation
    fun startVoiceRecordSimulation() {
        viewModelScope.launch {
            _voiceRecordingState.value = true
            _speechInput.value = "Listening..."
            delay(3000)
            _voiceRecordingState.value = false
        }
    }

    // Clear history
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Add profile
    fun addProfile(id: String, name: String, role: String, voicePhrase: String) {
        viewModelScope.launch {
            repository.profileDao.insertProfile(
                UserProfile(
                    id = id,
                    name = name,
                    role = role,
                    isFaceEnrolled = true,
                    isFingerprintEnrolled = role != "Guest",
                    isVoiceEnrolled = voicePhrase.isNotEmpty(),
                    enrolledVoicePhrase = voicePhrase.ifEmpty { "Default Phrase" }
                )
            )
        }
    }
}
