package com.openlauncher.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.ui.theme.LocalDayMode

@Composable
fun AppLibraryScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    isPickerMode: Boolean,
    pickerSlot: Int?,
    isCarPlayPickerMode: Boolean,
    carPlayPickerLabel: String = "CHOOSE CARPLAY APP",
    accent: Color,
    onAppClick: (AppInfo) -> Unit,
    onPickerSelect: (Int, AppInfo) -> Unit,
    onCarPlaySelect: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDayMode     = LocalDayMode.current
    val screenBg      = MaterialTheme.colorScheme.background
    val headerColor   = MaterialTheme.colorScheme.onBackground
    val placeholderC  = if (isDayMode) Color(0xFF999999) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val dividerColor  = if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    val emptyColor    = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    val fieldTextC    = MaterialTheme.colorScheme.onBackground
    val fieldBorderU  = if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

    val anyPickerMode = isPickerMode || isCarPlayPickerMode
    var query by remember { mutableStateOf("") }

    val filtered by remember(apps, query) {
        derivedStateOf {
            apps.filter { it.appName.contains(query, ignoreCase = true) }
        }
    }
/*
    val filtered by remember(apps, query) {
        derivedStateOf {
            apps.filter { !it.isSystemApp }
                .filter { it.appName.contains(query, ignoreCase = true) }
        }
    } */

    Column(modifier = modifier.fillMaxSize().background(screenBg)) {
        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text          = when {
                    isCarPlayPickerMode -> carPlayPickerLabel
                    anyPickerMode       -> "CHOOSE APP"
                    else                -> "APPS"
                },
                style         = MaterialTheme.typography.titleLarge,
                color         = if (anyPickerMode) accent else headerColor,
                letterSpacing = 3.sp,
                fontSize      = 14.sp
            )

            Spacer(Modifier.weight(1f))
            var searchFocused by remember { mutableStateOf(false) }
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .width(200.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, if (searchFocused) accent else fieldBorderU, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Search, null, tint = placeholderC, modifier = Modifier.size(14.dp))
                    BasicTextField(
                        value         = query,
                        onValueChange = { query = it },
                        singleLine    = true,
                        textStyle     = TextStyle(color = fieldTextC, fontSize = 13.sp),
                        cursorBrush   = SolidColor(accent),
                        modifier      = Modifier
                            .weight(1f)
                            .onFocusChanged { searchFocused = it.isFocused },
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) Text("Search…", color = placeholderC, fontSize = 13.sp)
                                inner()
                            }
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = dividerColor)

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            return@Column
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No apps found", color = emptyColor, letterSpacing = 1.sp, fontSize = 12.sp)
            }
            return@Column
        }

        // ── App grid ────────────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns               = GridCells.Fixed(6),
            contentPadding        = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.packageName }) { app ->
                AppTile(
                    app     = app,
                    accent  = accent,
                    onClick = {
                        when {
                            isCarPlayPickerMode            -> onCarPlaySelect(app)
                            isPickerMode && pickerSlot != null -> onPickerSelect(pickerSlot, app)
                            else                           -> onAppClick(app)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppTile(
    app: AppInfo,
    accent: Color,
    onClick: () -> Unit
) {
    val isDayMode  = LocalDayMode.current
    val tileBg     = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF0B0B0B)
    val tileBorder = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1A1A1A)
    val tileShape  = MaterialTheme.shapes.medium
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(tileShape)
            .background(tileBg)
            .border(1.dp, tileBorder, tileShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        val bmp = remember(app.packageName) {
            try { app.icon.toBitmap(80, 80) } catch (_: Exception) { null }
        }
        if (bmp != null) {
            androidx.compose.foundation.Image(
                painter            = BitmapPainter(bmp.asImageBitmap()),
                contentDescription = app.appName,
                modifier           = Modifier.size(52.dp)
            )
        } else {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Text(app.appName.take(1).uppercase(), color = accent, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text          = app.appName.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            maxLines      = 1,
            overflow      = TextOverflow.Ellipsis,
            textAlign     = TextAlign.Center,
            letterSpacing = 1.sp,
            fontSize      = 10.sp
        )
    }
}
