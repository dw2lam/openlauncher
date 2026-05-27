package com.openlauncher.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.AppInfo

private val TILE_RADIUS  = RoundedCornerShape(4.dp)
private val TILE_BG      = Color(0xFF0B0B0B)
private val TILE_BORDER  = Color(0xFF1A1A1A)

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
    val anyPickerMode = isPickerMode || isCarPlayPickerMode
    var query by remember { mutableStateOf("") }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
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
                color         = if (anyPickerMode) accent else Color.White,
                letterSpacing = 3.sp,
                fontSize      = 14.sp
            )
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search…", color = Color(0xFF444444), fontSize = 13.sp) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color(0xFF444444), modifier = Modifier.size(16.dp)) },
                singleLine    = true,
                shape         = RoundedCornerShape(4.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accent,
                    unfocusedBorderColor = Color(0xFF1E1E1E),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = accent
                ),
                modifier = Modifier.width(200.dp).height(44.dp)
            )
        }

        HorizontalDivider(color = Color(0xFF1A1A1A))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            return@Column
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No apps found", color = Color(0xFF3A3A3A), letterSpacing = 1.sp, fontSize = 12.sp)
            }
            return@Column
        }

        // ── App grid ────────────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns               = GridCells.Fixed(6),
            contentPadding        = PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement   = Arrangement.spacedBy(4.dp),
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(TILE_RADIUS)
            .background(TILE_BG)
            .border(1.dp, TILE_BORDER, TILE_RADIUS)
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        val bmp = remember(app.packageName) {
            try { app.icon.toBitmap(80, 80) } catch (_: Exception) { null }
        }
        if (bmp != null) {
            androidx.compose.foundation.Image(
                painter            = BitmapPainter(bmp.asImageBitmap()),
                contentDescription = app.appName,
                modifier           = Modifier.size(40.dp)
            )
        } else {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text(app.appName.take(1).uppercase(), color = accent, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text          = app.appName.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = Color(0xFF888888),
            maxLines      = 1,
            overflow      = TextOverflow.Ellipsis,
            textAlign     = TextAlign.Center,
            letterSpacing = 1.sp,
            fontSize      = 8.sp
        )
    }
}
