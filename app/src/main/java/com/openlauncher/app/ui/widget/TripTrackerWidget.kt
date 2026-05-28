package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlinx.coroutines.delay

@Composable
fun TripTrackerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Master design system colors aligned with Vitals, Compass, Clock, and Altimeter
    val displayColor = if (isDayMode) Color(0xFF111111) else Color.White
    val dimDisplayColor = if (isDayMode) Color(0xFF111111).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    
    val lcdBg = Color.Transparent
    val lcdBorder = if (isDayMode) Color(0xFFCCCCCC) else Color.White.copy(alpha = 0.12f)
    
    val labelColor = if (isDayMode) Color(0xFF888888) else Color.White.copy(alpha = 0.30f)
    
    // Safety accents mapped elegantly to the dynamic accent color
    val activeAccent = accent
    val teRed = Color(0xFFFF2D55) // Still useful for Reset/Stopped indicator
    val teGrey = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2E3238)

    var isRunning by rememberSaveable { mutableStateOf(false) }
    var driveTimeSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var idleTimeSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var totalSpeedSum by rememberSaveable { mutableDoubleStateOf(0.0) }
    var movingSecondsCount by rememberSaveable { mutableLongStateOf(0L) }

    var activeMode by rememberSaveable { mutableStateOf("TRIP") } // "TRIP" or "0-100"
    
    // Accel Test state
    var accelState by rememberSaveable { mutableStateOf("READY") } // "READY", "RUNNING", "COMPLETE"
    var accelStartTime by rememberSaveable { mutableLongStateOf(0L) }
    var accelEndTime by rememberSaveable { mutableLongStateOf(0L) }
    var accelTimeDisplay by remember { mutableStateOf("0.00s") }
    var bestAccelTime by rememberSaveable { mutableStateOf<Float?>(null) }
    
    var simSpeed by remember { mutableFloatStateOf(0f) }
    var isSimulating by remember { mutableStateOf(false) }

    val currentSpeedMps = location?.speedMps ?: 0f
    val speedDisplay = if (isSimulating) simSpeed else (if (isMetric) currentSpeedMps * 3.6f else currentSpeedMps * 2.23694f)
    val targetSpeed = if (isMetric) 100f else 60f
    val targetSpeedUnit = if (isMetric) "KM/H" else "MPH"

    // High precision stopwatch update loop
    LaunchedEffect(accelState, accelStartTime) {
        if (accelState == "RUNNING") {
            while (accelState == "RUNNING") {
                val elapsed = System.currentTimeMillis() - accelStartTime
                accelTimeDisplay = "%.2fs".format(elapsed / 1000f)
                delay(30)
            }
        } else if (accelState == "COMPLETE") {
            accelTimeDisplay = "%.2fs".format((accelEndTime - accelStartTime) / 1000f)
        } else {
            accelTimeDisplay = "0.00s"
        }
    }

    // GPS real-time speed run tracking
    LaunchedEffect(location, activeMode) {
        if (activeMode == "0-100" && !isSimulating) {
            val speed = location?.speedMps ?: 0f
            val speedDisplayVal = if (isMetric) speed * 3.6f else speed * 2.23694f
            
            if (accelState == "READY") {
                if (speedDisplayVal > 0.8f) { // start timer when vehicle moves above 0.8 km/h or mph
                    accelStartTime = System.currentTimeMillis()
                    accelState = "RUNNING"
                }
            } else if (accelState == "RUNNING") {
                if (speedDisplayVal >= targetSpeed) {
                    accelEndTime = System.currentTimeMillis()
                    accelState = "COMPLETE"
                    val finalTime = (accelEndTime - accelStartTime) / 1000f
                    bestAccelTime = if (bestAccelTime == null) finalTime else minOf(bestAccelTime!!, finalTime)
                }
            }
        }
    }

    // Playful local test simulation loop (triggers when tapping speed layout while READY)
    LaunchedEffect(isSimulating) {
        if (isSimulating) {
            accelStartTime = System.currentTimeMillis()
            accelState = "RUNNING"
            val simTarget = if (isMetric) 100f else 60f
            simSpeed = 0f
            while (simSpeed < simTarget + 5f && isSimulating && accelState == "RUNNING") {
                delay(30)
                val elapsed = (System.currentTimeMillis() - accelStartTime) / 1000f
                simSpeed = elapsed * elapsed * 2.8f + elapsed * 8f
                if (simSpeed >= simTarget) {
                    accelEndTime = System.currentTimeMillis()
                    accelState = "COMPLETE"
                    val finalTime = (accelEndTime - accelStartTime) / 1000f
                    bestAccelTime = if (bestAccelTime == null) finalTime else minOf(bestAccelTime!!, finalTime)
                    break
                }
            }
            isSimulating = false
        } else {
            simSpeed = 0f
        }
    }

    // Trip update loop
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            val currentSpeed = location?.speedMps ?: 0f
            if (currentSpeed > 0.5f) {
                driveTimeSeconds++
                totalSpeedSum += currentSpeed
                movingSecondsCount++
            } else {
                idleTimeSeconds++
            }
        }
    }

    // Calculations
    val averageSpeedMps = if (movingSecondsCount > 0) totalSpeedSum / movingSecondsCount else 0.0
    val avgSpeedDisplay = if (isMetric) averageSpeedMps * 3.6 else averageSpeedMps * 2.23694
    val speedUnit = if (isMetric) "KM/H" else "MPH"

    val totalDistanceMeters = totalSpeedSum
    val distanceDisplay = if (isMetric) totalDistanceMeters / 1000.0 else totalDistanceMeters / 1609.34
    val distUnit = if (isMetric) "KM" else "MI"

    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    // Flat, borderless Column that lets the launcher's card boundary frame the content
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. DYNAMIC MONOCHROME FLAT LCD PANEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(lcdBg)
                .border(1.dp, lcdBorder, RoundedCornerShape(6.dp))
                .drawBehind {
                    val dotColor = displayColor.copy(alpha = 0.02f)
                    val dotSize = 1.dp.toPx()
                    val gap = 5.dp.toPx()
                    var x = 3.dp.toPx()
                    while (x < size.width) {
                        var y = 3.dp.toPx()
                        while (y < size.height) {
                            drawCircle(
                                color = dotColor,
                                radius = dotSize / 2,
                                center = Offset(x, y)
                            )
                            y += gap
                        }
                        x += gap
                    }
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (activeMode == "TRIP") {
                // Panel Column 1: Distance Readout
                Column(
                    modifier = Modifier.weight(0.38f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DISTANCE // DIST",
                        color = labelColor,
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomStart) {
                            Text(
                                text = "88.88",
                                color = dimDisplayColor,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%05.2f".format(distanceDisplay),
                                color = displayColor,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = distUnit,
                            color = displayColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    
                    // Hired/Time-Off Indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "[RUNNING]",
                            color = if (isRunning) activeAccent else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[STOPPED]",
                            color = if (!isRunning && (driveTimeSeconds > 0 || idleTimeSeconds > 0)) teRed else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Panel Column 2: Drive & Idle Timers
                Column(
                    modifier = Modifier
                        .weight(0.30f)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DRIVE [TIME]", color = labelColor, fontSize = 6.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Box {
                            Text("88:88:88", color = dimDisplayColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(formatTime(driveTimeSeconds), color = displayColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("IDLE [TIME]", color = labelColor, fontSize = 6.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Box {
                            Text("88:88:88", color = dimDisplayColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(formatTime(idleTimeSeconds), color = displayColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Panel Column 3: Average Speed
                Column(
                    modifier = Modifier.weight(0.32f),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "AVG SPEED // SPD",
                            color = labelColor,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Text(
                                    text = "888.8",
                                    color = dimDisplayColor,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%05.1f".format(avgSpeedDisplay),
                                    color = displayColor,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = speedUnit,
                                color = displayColor,
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("SYS STAT", color = labelColor, fontSize = 6.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isRunning) "A" else "I",
                            color = if (isRunning) activeAccent else displayColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // ACCEL RUN DISPLAY (0-100 KM/H or 0-60 MPH Speed run)
                Column(
                    modifier = Modifier.weight(0.48f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isMetric) "ACCEL TEST // 0-100" else "ACCEL TEST // 0-60",
                        color = labelColor,
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomStart) {
                            Text(
                                text = "88.88",
                                color = dimDisplayColor,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = accelTimeDisplay.removeSuffix("s"),
                                color = displayColor,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "SEC",
                            color = displayColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    
                    // Acceleration Status indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "[READY]",
                            color = if (accelState == "READY") activeAccent else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[RUNNING]",
                            color = if (accelState == "RUNNING" || isSimulating) Color(0xFFE6A23C) else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[COMPLETE]",
                            color = if (accelState == "COMPLETE") teRed else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .clickable(enabled = accelState == "READY") {
                            isSimulating = true
                        },
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SPEED // TARGET %d".format(targetSpeed.toInt()),
                            color = labelColor,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Text(
                                    text = "888.8",
                                    color = dimDisplayColor,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%05.1f".format(speedDisplay),
                                    color = displayColor,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = targetSpeedUnit,
                                color = displayColor,
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (accelState == "READY" && !isSimulating) "TAP SPEED TO TEST" else "BEST RECORD",
                            color = if (accelState == "READY" && !isSimulating) activeAccent.copy(alpha = 0.7f) else labelColor,
                            fontSize = 5.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (bestAccelTime != null) "%.2fs".format(bestAccelTime) else "--.--s",
                            color = if (bestAccelTime != null) activeAccent else displayColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 2. FLAT MINIMALIST TACTILE BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: OPR / RUN styled as a flat dynamic circular cap
            val oprActive = if (activeMode == "0-100") (accelState == "RUNNING" || isSimulating) else isRunning
            TeTactileButton(
                label = "OPR",
                keyColor = activeAccent,
                active = oprActive,
                onClick = {
                    if (activeMode == "0-100") {
                        isSimulating = false
                        accelState = "READY"
                        accelStartTime = 0L
                        accelEndTime = 0L
                    } else {
                        isRunning = !isRunning
                    }
                },
                isDayMode = isDayMode
            )

            // Button 2: RST / RESET
            val canReset = if (activeMode == "0-100") {
                accelState == "COMPLETE" || bestAccelTime != null
            } else {
                !isRunning && (driveTimeSeconds > 0 || idleTimeSeconds > 0)
            }
            TeTactileButton(
                label = "RST",
                keyColor = teRed,
                active = false,
                enabled = canReset,
                onClick = {
                    if (activeMode == "0-100") {
                        isSimulating = false
                        accelState = "READY"
                        accelStartTime = 0L
                        accelEndTime = 0L
                        bestAccelTime = null
                    } else {
                        driveTimeSeconds = 0L
                        idleTimeSeconds = 0L
                        totalSpeedSum = 0.0
                        movingSecondsCount = 0L
                    }
                },
                isDayMode = isDayMode
            )

            // Button 3: EXTRAS (Toggles between TRIP info and 0-100 Accel Run)
            TeTactileButton(
                label = "EXT",
                keyColor = activeAccent,
                active = activeMode == "0-100",
                enabled = true,
                onClick = {
                    activeMode = if (activeMode == "TRIP") "0-100" else "TRIP"
                },
                isDayMode = isDayMode
            )

            // Button 4: SET
            TeTactileButton(
                label = "SET",
                keyColor = teGrey,
                active = false,
                enabled = false,
                onClick = {},
                isDayMode = isDayMode
            )
        }
    }
}

@Composable
private fun TeTactileButton(
    label: String,
    keyColor: Color,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    isDayMode: Boolean
) {
    val printedLabelColor = if (isDayMode) Color(0xFF666666) else Color.White.copy(alpha = 0.35f)
    
    val buttonBg = if (!enabled) {
        Color.Transparent
    } else if (active) {
        keyColor
    } else {
        if (isDayMode) Color(0xFFE5E7EB) else Color(0xFF1D2024)
    }
    
    val buttonBorder = if (isDayMode) Color(0xFFD1D5DB) else Color(0xFF2E3238)
    val dotColor = if (active) {
        if (isDayMode) Color.White else Color.Black
    } else {
        if (enabled) keyColor else keyColor.copy(alpha = 0.2f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Monospace printed label above key
        Text(
            text = label,
            color = printedLabelColor,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // Flat, elegant minimalist circular keycap
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(1.dp, buttonBorder, CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Failsafe flat center indicator
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

