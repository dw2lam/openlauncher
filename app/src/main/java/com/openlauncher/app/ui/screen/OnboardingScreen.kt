package com.openlauncher.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.openlauncher.app.data.AppSettings

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    accent: Color,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by rememberSaveable { mutableStateOf(0) }
    var locationGranted by remember { mutableStateOf(false) }
    var mediaGranted by remember { mutableStateOf(false) }

    val checkPermissions = {
        locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        mediaGranted = enabledListeners != null && enabledListeners.contains(context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Trigger initial check
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationGranted = granted
        if (granted) {
            currentStep = 2 // Auto-advance to next step
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070707))
    ) {
        // Aesthetic glowing background orb
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.15f), Color.Transparent),
                        radius = 600f
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Left branding pane ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(Color(0xFF0F0F0F))
                    .padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "OPEN LAUNCHER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Designed for the dashboard",
                        color = Color(0xFF666666),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Step indicator list
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepItem(0, "Introduction", currentStep)
                    StepItem(1, "Location Services", currentStep)
                    StepItem(2, "Media Integration", currentStep)
                    StepItem(3, "Ready to Go", currentStep)
                }

                Text(
                    text = "v0.0.5",
                    color = Color(0xFF333333),
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }

            // Vertical separator line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1E1E1E))
            )

            // ── Right content wizard ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(48.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Active Step Content
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally { it / 5 } togetherWith
                            fadeOut() + slideOutHorizontally { -it / 5 }
                        },
                        label = "step_transition"
                    ) { step ->
                        when (step) {
                            0 -> IntroStep(accent)
                            1 -> LocationStep(accent, locationGranted, onGrant = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            })
                            2 -> MediaStep(accent, mediaGranted, onGrant = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                            })
                            3 -> FinalStep(accent, onSetDefault = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                }
                            })
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // UNIFIED WIZARD FOOTER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (left aligned)
                    if (currentStep > 0) {
                        TextButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("BACK", color = Color(0xFF888888), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    // Next / Finish button (right aligned)
                    val isPrimary = when (currentStep) {
                        0 -> true
                        1 -> locationGranted
                        2 -> mediaGranted
                        3 -> true
                        else -> true
                    }

                    val nextButtonLabel = when (currentStep) {
                        0 -> "GET STARTED"
                        1 -> if (locationGranted) "CONTINUE" else "SKIP FOR NOW"
                        2 -> if (mediaGranted) "CONTINUE" else "SKIP FOR NOW"
                        3 -> "FINISH SETUP"
                        else -> "CONTINUE"
                    }

                    val nextButtonIcon = if (currentStep == 3) Icons.Default.Check else Icons.Default.ArrowForward

                    if (isPrimary) {
                        Button(
                            onClick = {
                                if (currentStep < 3) {
                                    currentStep++
                                } else {
                                    onComplete()
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(nextButtonLabel, color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(nextButtonIcon, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                currentStep++
                            },
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(nextButtonLabel, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(nextButtonIcon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(stepIndex: Int, title: String, currentStep: Int) {
    val active = stepIndex == currentStep
    val completed = stepIndex < currentStep
    val tint = when {
        active -> MaterialTheme.colorScheme.primary
        completed -> Color(0xFF44AA44)
        else -> Color(0xFF333333)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(tint)
            )
        }
        Text(
            text = title,
            fontSize = 11.sp,
            color = if (active) Color.White else Color(0xFF666666),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun IntroStep(accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "WELCOME TO OPEN LAUNCHER",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 2.sp,
            fontSize = 20.sp
        )
        Text(
            text = "A clean, modern landscape dashboard designed to be the ultimate companion for your car's screen.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BulletItem(Icons.Default.CloudOff, "100% Offline-Based", "No reliance on a mobile signal or network connection to function. Speedometer, compass telemetry, and altimeter operate entirely offline.")
            BulletItem(Icons.Default.Palette, "Highly Customizable Dashboard", "Tailor color accents, background gradients, typography fonts, system units, and drag-and-drop to rearrange your tiles.")
            BulletItem(Icons.Default.VolumeUp, "Soundboard & Media Shortcuts", "Trigger custom soundboard sound effects, manage CarPlay & Android Auto shortcuts, and control active media players.")
        }
    }
}

@Composable
private fun LocationStep(accent: Color, isGranted: Boolean, onGrant: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "TELEMETRY & WEATHER",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 2.sp,
            fontSize = 20.sp
        )
        Text(
            text = "To compute your real-time speed, compass bearing, altitude telemetry, and update local weather conditions, Open Launcher requires high-precision GPS services.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (isGranted) Color(0xFF0F1E10) else Color(0xFF1E1010))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF44AA44) else Color(0xFFDD5555),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (isGranted) "Permission Granted" else "Permission Required",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isGranted) "GPS telemetry is active and ready." else "Telemetry is currently disabled.",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!isGranted) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onGrant,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("GRANT ACCESS", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MediaStep(accent: Color, isGranted: Boolean, onGrant: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "MEDIA INTEGRATION",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 2.sp,
            fontSize = 20.sp
        )
        Text(
            text = "To capture live album art, track info, progress bars, and provide playback control from your dashboard cards, Open Launcher listens to active media notifications.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (isGranted) Color(0xFF0F1E10) else Color(0xFF1E1010))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF44AA44) else Color(0xFFDD5555),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (isGranted) "Notification Access Granted" else "Notification Access Required",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isGranted) "Music player widget is connected." else "Now Playing dashboard will remain inactive.",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!isGranted) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onGrant,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.VolumeUp, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ENABLE MEDIA LISTENER", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FinalStep(accent: Color, onSetDefault: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "READY FOR THE ROAD!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 2.sp,
            fontSize = 20.sp
        )
        Text(
            text = "You are all set up and ready to go. You can set Open Launcher as your default home app so it launches automatically whenever you start your vehicle.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSetDefault,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("SET AS DEFAULT", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BulletItem(icon: ImageVector, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(desc, color = Color(0xFF888888), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
