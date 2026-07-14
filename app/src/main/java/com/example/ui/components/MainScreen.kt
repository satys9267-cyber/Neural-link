package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CommandHistory
import com.example.data.UserProfile
import com.example.ui.EshaViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EshaViewModel) {
    val loginStep by viewModel.loginStep.collectAsStateWithLifecycle()
    val isUserAuthenticated by viewModel.isUserAuthenticated.collectAsStateWithLifecycle()
    val resolvedProfile by viewModel.resolvedProfile.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isUserAuthenticated) CyanAccent else CyberRed)
                        )
                        Text(
                            text = "ESHA // CONTROL SUITE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = TextSilver
                        )
                    }
                },
                actions = {
                    if (isUserAuthenticated) {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Lock System",
                                tint = CyberRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground,
                    titleContentColor = TextSilver
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateDarkBackground)
        ) {
            AnimatedContent(
                targetState = isUserAuthenticated,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "auth_transition"
            ) { authenticated ->
                if (authenticated) {
                    DashboardView(viewModel)
                } else {
                    LoginGateView(viewModel, loginStep, resolvedProfile, allProfiles)
                }
            }
        }
    }
}

@Composable
fun LoginGateView(
    viewModel: EshaViewModel,
    step: Int,
    resolvedProfile: UserProfile?,
    allProfiles: List<UserProfile>
) {
    var selectedProfileId by remember { mutableStateOf("admin") }
    val isFaceScanning by viewModel.isFaceScanning.collectAsStateWithLifecycle()
    val isFingerprintScanning by viewModel.isFingerprintScanning.collectAsStateWithLifecycle()
    val isVoiceVerifying by viewModel.isVoiceVerifying.collectAsStateWithLifecycle()
    val speechInput by viewModel.speechInput.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Futuristic Ring and Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LAYERED BIOMETRIC LOGIN GATE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CyanAccent,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Sequential 4-Factor Authentication active. Authenticate your identity profile to establish secure persistent ADB link.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Sequential Steps Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthStepIndicator(step = 1, activeStep = step, label = "FACE")
            Divider(modifier = Modifier.width(30.dp), color = if (step > 1) CyanAccent else BorderSlate)
            AuthStepIndicator(step = 2, activeStep = step, label = "FINGER")
            Divider(modifier = Modifier.width(30.dp), color = if (step > 2) CyanAccent else BorderSlate)
            AuthStepIndicator(step = 3, activeStep = step, label = "VOICE")
        }

        // Current Active Step Content
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            },
            label = "step_content_transition"
        ) { currentStep ->
            when (currentStep) {
                1 -> {
                    // Face Scan
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "STEP 1: CAMERA SCAN & ROLE RESOLUTION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSilver,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        // Camera Scan Simulation Box
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TintedSlateSurface)
                                .border(2.dp, if (isFaceScanning) CyanAccent else BorderSlate, RoundedCornerShape(12.dp))
                                .drawBehind {
                                    // Custom target grid
                                    drawRect(
                                        color = BorderSlate.copy(alpha = 0.3f),
                                        size = size
                                    )
                                    // Bounding Box Simulation
                                    if (isFaceScanning) {
                                        drawRoundRect(
                                            color = CyanAccent,
                                            topLeft = Offset(40.dp.toPx(), 40.dp.toPx()),
                                            size = Size(120.dp.toPx(), 120.dp.toPx()),
                                            cornerRadius = CornerRadius(10.dp.toPx()),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        // Scanline
                                        val scanlineY = (System.currentTimeMillis() % 2000 / 2000f) * size.height
                                        drawLine(
                                            color = CyanAccent,
                                            start = Offset(0f, scanlineY),
                                            end = Offset(size.width, scanlineY),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFaceScanning) {
                                Text(
                                    text = "SCANNING FACE...",
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Face,
                                    contentDescription = "Face ID Scan",
                                    tint = TextMuted,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        // Simulation selector: Choose which face ESHA detects
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Simulate Detected Profile:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextSilver
                                )
                                allProfiles.forEach { profile ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedProfileId = profile.id }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedProfileId == profile.id,
                                            onClick = { selectedProfileId = profile.id },
                                            colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                                        )
                                        Column {
                                            Text(
                                                text = profile.name,
                                                color = TextSilver,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Role: ${profile.role}",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.simulateFaceScan(selectedProfileId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("face_scan_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFaceScanning) {
                                CircularProgressIndicator(color = SlateDarkBackground, modifier = Modifier.size(24.dp))
                            } else {
                                Text("EXECUTE CAMERA FACE SCAN", color = SlateDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                2 -> {
                    // Fingerprint
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "STEP 2: BIOMETRIC FINGERPRINT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSilver,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Text(
                            text = "Identity Resolved: ${resolvedProfile?.name} [${resolvedProfile?.role}]. Please verify fingerprint biometric credentials.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )

                        // Animated Fingerprint Scan Area
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(TintedSlateSurface)
                                .border(2.dp, if (isFingerprintScanning) CyanAccent else BorderSlate, CircleShape)
                                .clickable { viewModel.simulateFingerprintScan(true) }
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = "Fingerprint Sensor",
                                tint = if (isFingerprintScanning) CyanAccent else ElectricBlue,
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(if (isFingerprintScanning) scale else 1f)
                            )
                        }

                        Text(
                            text = "Tap the biometric scanner above to authorize.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.simulateFingerprintScan(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("fingerprint_success"),
                                border = BorderStroke(1.dp, CyanAccent)
                            ) {
                                Text("SIMULATE TOUCH", color = CyanAccent)
                            }
                            OutlinedButton(
                                onClick = { viewModel.setLoginStep(1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, BorderSlate)
                            ) {
                                Text("RESTART FLOW", color = TextSilver)
                            }
                        }
                    }
                }
                3 -> {
                    // Voice Phrase verification
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "STEP 3: SPEAKER SPEAKER-VERIFICATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSilver,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Text(
                            text = "Speak the required passphrase out loud. ESHA will analyze voiceprint harmonics to authenticate session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, CyanAccent)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "REQUIRED PASSPHRASE:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = AmberAccent),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = resolvedProfile?.enrolledVoicePhrase ?: "Hey ESHA, authenticate",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextSilver,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Simulated Speaker voice wave (moving sin lines!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TintedSlateSurface)
                                .drawBehind {
                                    if (isVoiceVerifying) {
                                        val waveCount = 5
                                        val time = System.currentTimeMillis() / 200f
                                        for (w in 0 until waveCount) {
                                            val path = androidx.compose.ui.graphics.Path()
                                            path.moveTo(0f, size.height / 2f)
                                            val amplitude = (20.dp.toPx() / (w + 1))
                                            for (x in 0..size.width.toInt() step 5) {
                                                val y = (size.height / 2f) + sin((x * 0.02f) + time + (w * 0.5f)) * amplitude
                                                path.lineTo(x.toFloat(), y)
                                            }
                                            drawPath(
                                                path = path,
                                                color = CyanAccent.copy(alpha = 1f / (w + 1)),
                                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                    } else {
                                        // Silent baseline
                                        drawLine(
                                            color = BorderSlate,
                                            start = Offset(0f, size.height / 2f),
                                            end = Offset(size.width, size.height / 2f),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVoiceVerifying) {
                                Text(
                                    text = "ANALYZING VOICEPRINT HARMONICS...",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = if (speechInput.isNotEmpty()) speechInput else "Mic Ready",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val phrase = resolvedProfile?.enrolledVoicePhrase ?: "Hey ESHA, connect"
                                    viewModel.simulateVoiceVerify(phrase)
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("voice_match_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Text("SAY PASSPHRASE", color = SlateDarkBackground, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.simulateVoiceVerify("Wrong passphrase speaker")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed)
                            ) {
                                Text("FAIL MIC", color = TextSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthStepIndicator(step: Int, activeStep: Int, label: String) {
    val isCompleted = activeStep > step
    val isActive = activeStep == step
    val color = when {
        isCompleted -> CyanAccent
        isActive -> AmberAccent
        else -> BorderSlate
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isCompleted) CyanAccent else TintedSlateSurface)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed",
                    tint = SlateDarkBackground,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = step.toString(),
                    color = if (isActive) AmberAccent else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun DashboardView(viewModel: EshaViewModel) {
    val adbIp by viewModel.adbIp.collectAsStateWithLifecycle()
    val adbPort by viewModel.adbPort.collectAsStateWithLifecycle()
    val adbConnected by viewModel.adbConnected.collectAsStateWithLifecycle()
    val watchdogActive by viewModel.watchdogActive.collectAsStateWithLifecycle()
    val resolvedProfile by viewModel.resolvedProfile.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val activeAdbLog by viewModel.activeAdbLog.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Control Console, 1: Laptop CLI, 2: Security & ADB
    val tabs = listOf("CONSOLE", "LAPTOP CLI", "SYSTEM GATE")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        // Quick Security & Link Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SECURE SESSION: ${resolvedProfile?.name?.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Badge(
                            containerColor = when (resolvedProfile?.role) {
                                "Admin" -> CyberRed
                                "Family Member" -> AmberAccent
                                else -> BorderSlate
                            }
                        ) {
                            Text(
                                text = resolvedProfile?.role ?: "Guest",
                                color = TextSilver,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Text(
                            text = "Ceiling: ${if (resolvedProfile?.role == "Admin") "Unlimited" else "Restricted"}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // ADB Bridge indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ADB TCP BRIDGE",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontFamily = FontFamily.Monospace)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (adbConnected) CyanAccent else CyberRed)
                        )
                        Text(
                            text = if (adbConnected) "PERSISTENT LINK ACTIVE" else "LINK CLOSED",
                            color = if (adbConnected) CyanAccent else CyberRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SlateDarkBackground,
            contentColor = CyanAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanAccent
                )
            },
            divider = { Divider(color = BorderSlate) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(48.dp),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                )
            }
        }

        // Active Tab Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "tab_view_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ControlConsoleTab(viewModel, allHistory)
                    1 -> LaptopCliTab(allHistory)
                    2 -> SystemSecurityTab(
                        viewModel,
                        adbIp,
                        adbPort,
                        adbConnected,
                        watchdogActive,
                        activeAdbLog
                    )
                }
            }
        }
    }
}

@Composable
fun ControlConsoleTab(viewModel: EshaViewModel, history: List<CommandHistory>) {
    var commandInput by remember { mutableStateOf("") }
    val isProcessingCommand by viewModel.isProcessingCommand.collectAsStateWithLifecycle()
    val speechInput by viewModel.speechInput.collectAsStateWithLifecycle()
    val voiceRecordingState by viewModel.voiceRecordingState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Holographic Brain Visualizer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianSurface)
                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // ESHA neural pulsing core drawing
            val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
            val pulseAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Circular Glowing AI core
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .rotate(pulseAngle)
                        .drawBehind {
                            val brush = Brush.radialGradient(
                                colors = listOf(CyanAccent, ElectricBlue, Color.Transparent),
                                center = center,
                                radius = size.width / 1.5f
                            )
                            drawCircle(brush = brush)
                            drawCircle(
                                color = CyanAccent,
                                radius = size.width / 2f,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Core rings
                            drawCircle(
                                color = AmberAccent.copy(alpha = 0.6f),
                                radius = size.width / 3.5f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Active speaking status
                Text(
                    text = if (isProcessingCommand) "ESHA IS THINKING..." else if (voiceRecordingState) "LISTENING MIC..." else "ESHA NEURAL CORE ONLINE",
                    color = if (isProcessingCommand) AmberAccent else if (voiceRecordingState) CyanAccent else TextSilver,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )

                if (speechInput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$speechInput\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanAccent,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Active Pipeline Dispatch Console
        val latestLog = history.firstOrNull()
        if (latestLog != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                colors = CardDefaults.cardColors(containerColor = TintedSlateSurface),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMMAND DISPATCH PIPELINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Badge(
                            containerColor = when (latestLog.executionStatus) {
                                "Success" -> CyanAccent.copy(alpha = 0.2f)
                                "Unauthorized" -> CyberRed.copy(alpha = 0.2f)
                                else -> BorderSlate
                            }
                        ) {
                            Text(
                                text = latestLog.executionStatus.uppercase(),
                                color = if (latestLog.executionStatus == "Success") CyanAccent else CyberRed,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Target: ${latestLog.targetDevice}",
                            color = TextSilver,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Risk: ${latestLog.riskTier}",
                            color = if (latestLog.riskTier == "High") CyberRed else if (latestLog.riskTier == "Medium") AmberAccent else CyanAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "ESHA: ${latestLog.responseText}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSilver)
                    )

                    Divider(color = BorderSlate)

                    Text(
                        text = latestLog.technicalLogs,
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDarkBackground)
                            .padding(8.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TintedSlateSurface)
                    .border(1.dp, BorderSlate, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No command dispatched. Enter a command below or trigger microphone.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Action input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                placeholder = { Text("Command ESHA...", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextSilver,
                    unfocusedTextColor = TextSilver,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSlate,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    if (commandInput.isNotEmpty()) {
                        IconButton(onClick = { commandInput = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                }
            )

            // Micro simulated button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (voiceRecordingState) CyanAccent else ObsidianSurface)
                    .border(1.dp, if (voiceRecordingState) CyanAccent else BorderSlate, RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.startVoiceRecordSimulation()
                        commandInput = "open spotify and play retro cyberpunk music"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Simulate Microphone Input",
                    tint = if (voiceRecordingState) SlateDarkBackground else CyanAccent
                )
            }

            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        viewModel.dispatchCommand(commandInput)
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("submit_command_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Send, contentDescription = "Send", tint = SlateDarkBackground)
            }
        }
    }
}

@Composable
fun LaptopCliTab(history: List<CommandHistory>) {
    val laptopLogs = history.filter { it.targetDevice == "Laptop" || it.targetDevice == "Both" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Computer,
                    contentDescription = "Laptop Status",
                    tint = CyanAccent,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "ESHA DESKTOP SHELL (LINUX)",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = "Authorized terminal session established via static IP bridge. Fully scriptable wmctrl/xdotool actuation.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }

        // Live simulated CLI display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = SlateDarkBackground),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "esha@linux-desktop:~# systemctl status esha-brain.service\n" +
                                "● esha-brain.service - ESHA AI Agent Core Engine\n" +
                                "   Active: active (running) since Mon 2026-07-13 23:12:05 UTC; 1h ago\n" +
                                "   Main PID: 10424 (node)\n" +
                                "   Tasks: 12 (limit: 4915)\n" +
                                "   Memory: 142.0M\n" +
                                "   CGroup: /system.slice/esha-brain.service\n" +
                                "           └─10424 /usr/bin/node /opt/esha/brain.js\n" +
                                "esha@linux-desktop:~# tail -n 20 /var/log/esha.log",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                if (laptopLogs.isEmpty()) {
                    item {
                        Text(
                            text = "[!] No laptop executions found in the current session. Run commands targeting 'laptop' (e.g. \"kill window\", \"open vs code\") in the console tab.",
                            color = AmberAccent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(laptopLogs) { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSlate, RoundedCornerShape(4.dp))
                                .background(ObsidianSurface)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = ">> CLI Execute: \"${log.inputText}\"",
                                color = AmberAccent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.technicalLogs,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemSecurityTab(
    viewModel: EshaViewModel,
    ip: String,
    port: String,
    connected: Boolean,
    watchdog: Boolean,
    log: String
) {
    var editIp by remember { mutableStateOf(ip) }
    var editPort by remember { mutableStateOf(port) }
    var enrollName by remember { mutableStateOf("") }
    var enrollRole by remember { mutableStateOf("Family Member") }
    var enrollPhrase by remember { mutableStateOf("") }

    val roles = listOf("Admin", "Family Member", "Guest")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ADB connection management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PERSISTENT ADB CONNECTION SETTINGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editIp,
                            onValueChange = { editIp = it },
                            modifier = Modifier.weight(2f),
                            label = { Text("Phone Static IP", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextSilver,
                                unfocusedTextColor = TextSilver,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSlate
                            )
                        )
                        OutlinedTextField(
                            value = editPort,
                            onValueChange = { editPort = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Port", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextSilver,
                                unfocusedTextColor = TextSilver,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSlate
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "IP Watchdog Monitor",
                                color = TextSilver,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Silently reconnects if link drops",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = watchdog,
                            onCheckedChange = { viewModel.toggleWatchdog(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.connectAdb(editIp, editPort) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Text("CONNECT", color = SlateDarkBackground, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.disconnectAdb() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed)
                        ) {
                            Text("DISCONNECT", color = TextSilver, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (log.isNotEmpty()) {
                        Text(
                            text = log,
                            color = AmberAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkBackground)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Profile enrollment / New Face Detected simulations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ENROLL NEW IDENTITY PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    OutlinedTextField(
                        value = enrollName,
                        onValueChange = { enrollName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Profile Full Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextSilver,
                            unfocusedTextColor = TextSilver,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSlate
                        )
                    )

                    OutlinedTextField(
                        value = enrollPhrase,
                        onValueChange = { enrollPhrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Voice Authentication Phrase", color = TextMuted) },
                        placeholder = { Text("e.g. Hey ESHA, identify me") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextSilver,
                            unfocusedTextColor = TextSilver,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSlate
                        )
                    )

                    Text(
                        text = "Assign Security Role Group:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roles.forEach { role ->
                            FilterChip(
                                selected = enrollRole == role,
                                onClick = { enrollRole = role },
                                label = { Text(role, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = SlateDarkBackground
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (enrollName.isNotBlank()) {
                                viewModel.addProfile(
                                    id = enrollName.lowercase().replace(" ", "_"),
                                    name = enrollName,
                                    role = enrollRole,
                                    voicePhrase = enrollPhrase
                                )
                                enrollName = ""
                                enrollPhrase = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("ENROLL PROFILE VIA ADMIN SESSION", color = SlateDarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Audit Logs / System actions clear button
        item {
            Button(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BorderSlate)
            ) {
                Text("PURGE SESSION DISPATCH AUDIT LOGS", color = TextSilver)
            }
        }
    }
}
