package com.openlauncher.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DefaultShortcutIcon
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.model.NavDestination
import kotlin.math.roundToInt

private val ICON_INACTIVE = Color(0xFF3A3A3A)
private val ICON_SIZE     = 22.dp
private val SLOT_HEIGHT   = 52.dp

@Composable
fun Sidebar(
    currentDest: NavDestination,
    settings: AppSettings,
    installedIconFor: (String) -> Drawable?,
    onNavigate: (NavDestination) -> Unit,
    onShortcutClick: (Int) -> Unit,
    onShortcutLongPress: (Int) -> Unit,
    onShortcutRemove: (Int) -> Unit,
    onShortcutSetIcon: (Int, DefaultShortcutIcon?) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(settings.accentColor)
    val density = LocalDensity.current
    val slotHeightPx = with(density) { SLOT_HEIGHT.toPx() }

    var actionSheetSlot by remember { mutableStateOf<Int?>(null) }
    var iconPickerSlot  by remember { mutableStateOf<Int?>(null) }

    // Drag-to-reorder state
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx  by remember { mutableFloatStateOf(0f) }

    fun dragTargetIndex(): Int = if (draggingIndex < 0) -1 else
        (draggingIndex + (dragOffsetPx / slotHeightPx).roundToInt())
            .coerceIn(0, settings.shortcuts.size - 1)

    fun slotTranslationY(index: Int): Float {
        if (draggingIndex < 0 || index == draggingIndex) return 0f
        val from = draggingIndex
        val to   = dragTargetIndex()
        return when {
            from < to && index in (from + 1)..to -> -slotHeightPx
            from > to && index in to until from  ->  slotHeightPx
            else -> 0f
        }
    }

    Column(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(Color(0xFF080808)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 6.dp, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            settings.shortcuts.forEachIndexed { index, shortcut ->
                val isDragging = (index == draggingIndex)
                val translationY = if (isDragging) dragOffsetPx else slotTranslationY(index)

                ShortcutSlot(
                    shortcut         = shortcut,
                    accent           = accent,
                    resolvedIcon     = if (shortcut.packageName.isNotEmpty())
                                           installedIconFor(shortcut.packageName) else null,
                    isDragging       = isDragging,
                    dragTranslationY = translationY,
                    onClick          = { onShortcutClick(index) },
                    onLongPress      = {
                        if (shortcut.packageName.isNotEmpty()) {
                            actionSheetSlot = index
                        } else {
                            onShortcutLongPress(index)
                        }
                    },
                    onDragStart = {
                        draggingIndex = index
                        dragOffsetPx  = 0f
                    },
                    onDragDelta = { dy -> dragOffsetPx += dy },
                    onDragEnd   = {
                        val from = draggingIndex
                        val to   = dragTargetIndex()
                        if (from >= 0 && to != from) onReorder(from, to)
                        draggingIndex = -1
                        dragOffsetPx  = 0f
                    }
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1A1A1A))

        NavButton(
            icon     = Icons.Default.Apps,
            label    = "Apps",
            isActive = currentDest == NavDestination.APP_LIBRARY,
            accent   = accent,
            onClick  = { onNavigate(NavDestination.APP_LIBRARY) }
        )
        NavButton(
            icon     = Icons.Default.Settings,
            label    = "Settings",
            isActive = currentDest == NavDestination.SETTINGS,
            accent   = accent,
            onClick  = { onNavigate(NavDestination.SETTINGS) }
        )
        NavButton(
            icon     = Icons.Default.Home,
            label    = "Home",
            isActive = currentDest == NavDestination.HOME,
            accent   = accent,
            onClick  = { onNavigate(NavDestination.HOME) }
        )

        Spacer(Modifier.height(4.dp))
    }

    // ── Action sheet dialog ──────────────────────────────────────────────────
    actionSheetSlot?.let { slot ->
        ShortcutActionDialog(
            accent      = accent,
            onChangeApp = {
                actionSheetSlot = null
                onShortcutLongPress(slot)
            },
            onCustomizeIcon = {
                actionSheetSlot = null
                iconPickerSlot = slot
            },
            onRemove = {
                actionSheetSlot = null
                onShortcutRemove(slot)
            },
            onDismiss = { actionSheetSlot = null }
        )
    }

    // ── Icon picker dialog ───────────────────────────────────────────────────
    iconPickerSlot?.let { slot ->
        IconPickerDialog(
            accent          = accent,
            hasNativeIcon   = settings.shortcuts.getOrNull(slot)?.packageName?.isNotEmpty() == true,
            currentOverride = settings.shortcuts.getOrNull(slot)?.customIconOverride,
            onPick  = { icon ->
                iconPickerSlot = null
                onShortcutSetIcon(slot, icon)
            },
            onDismiss = { iconPickerSlot = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutSlot(
    shortcut: ShortcutConfig,
    accent: Color,
    resolvedIcon: Drawable?,
    isDragging: Boolean,
    dragTranslationY: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val currentOnClick      by rememberUpdatedState(onClick)
    val currentOnLongPress  by rememberUpdatedState(onLongPress)
    val currentOnDragStart  by rememberUpdatedState(onDragStart)
    val currentOnDragDelta  by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd    by rememberUpdatedState(onDragEnd)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(SLOT_HEIGHT)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragTranslationY
                alpha        = if (isDragging) 0.55f else 1f
            }
            // Tap handler — short taps launch the app
            .combinedClickable(
                onClick     = { currentOnClick() },
                onLongClick = { /* intentionally empty — long press handled by drag detector below */ }
            )
            // Long-press + optional drag handler
            // onDragStart fires immediately when the long-press threshold fires (even before
            // the user moves). We track whether the movement was significant to decide
            // whether this is a reorder drag or a no-move long press (action sheet).
            .pointerInput(Unit) {
                var longPressTriggered  = false
                var hasSignificantDrag  = false
                var totalDragY          = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        longPressTriggered = true
                        hasSignificantDrag = false
                        totalDragY         = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                        if (!hasSignificantDrag && kotlin.math.abs(totalDragY) > viewConfiguration.touchSlop) {
                            hasSignificantDrag = true
                            currentOnDragStart()
                        }
                        if (hasSignificantDrag) currentOnDragDelta(dragAmount.y)
                    },
                    onDragEnd = {
                        if (longPressTriggered && !hasSignificantDrag) currentOnLongPress()
                        if (hasSignificantDrag) currentOnDragEnd()
                        longPressTriggered = false
                        hasSignificantDrag = false
                        totalDragY         = 0f
                    },
                    onDragCancel = {
                        if (hasSignificantDrag) currentOnDragEnd()
                        longPressTriggered = false
                        hasSignificantDrag = false
                        totalDragY         = 0f
                    }
                )
            }
    ) {
        val override = shortcut.customIconOverride
        when {
            override != null && override != DefaultShortcutIcon.NONE -> {
                Icon(
                    imageVector        = override.toIcon(),
                    contentDescription = shortcut.label,
                    tint               = ICON_INACTIVE,
                    modifier           = Modifier.size(ICON_SIZE)
                )
            }
            resolvedIcon != null -> {
                val bmp = resolvedIcon.toBitmap(44, 44)
                Icon(
                    painter            = BitmapPainter(bmp.asImageBitmap()),
                    contentDescription = shortcut.label,
                    tint               = Color.Unspecified,
                    modifier           = Modifier.size(26.dp)
                )
            }
            shortcut.isDefault -> {
                Icon(
                    imageVector        = shortcut.defaultIcon.toIcon(),
                    contentDescription = shortcut.label,
                    tint               = ICON_INACTIVE,
                    modifier           = Modifier.size(ICON_SIZE)
                )
            }
            else -> {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add shortcut",
                    tint               = Color(0xFF252525),
                    modifier           = Modifier.size(ICON_SIZE)
                )
            }
        }
    }
}

@Composable
private fun ShortcutActionDialog(
    accent: Color,
    onChangeApp: () -> Unit,
    onCustomizeIcon: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp)
                .width(180.dp)
        ) {
            ActionRow("CHANGE APP",     Icons.Default.SwapHoriz, accent, onChangeApp)
            HorizontalDivider(color = Color(0xFF1A1A1A))
            ActionRow("CUSTOMIZE ICON", Icons.Default.Palette,   accent, onCustomizeIcon)
            HorizontalDivider(color = Color(0xFF1A1A1A))
            ActionRow("REMOVE",         Icons.Default.Delete,     Color(0xFF993333), onRemove)
        }
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun IconPickerDialog(
    accent: Color,
    hasNativeIcon: Boolean,
    currentOverride: DefaultShortcutIcon?,
    onPick: (DefaultShortcutIcon?) -> Unit,
    onDismiss: () -> Unit
) {
    val vectorOptions = DefaultShortcutIcon.entries.filter { it != DefaultShortcutIcon.NONE }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Text(
                "CHOOSE ICON",
                color         = Color(0xFF888888),
                fontSize      = 9.sp,
                letterSpacing = 2.sp,
                modifier      = Modifier.padding(bottom = 10.dp)
            )

            if (hasNativeIcon) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (currentOverride == null) accent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onPick(null) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Apps, null, tint = if (currentOverride == null) accent else Color(0xFF666666), modifier = Modifier.size(18.dp))
                    Text(
                        "NATIVE APP ICON",
                        color         = if (currentOverride == null) accent else Color(0xFF888888),
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
                HorizontalDivider(color = Color(0xFF1A1A1A), modifier = Modifier.padding(vertical = 6.dp))
            }

            LazyVerticalGrid(
                columns               = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement   = Arrangement.spacedBy(5.dp),
                modifier              = Modifier.heightIn(max = 300.dp)
            ) {
                items(vectorOptions) { iconOption ->
                    val isSelected = currentOverride == iconOption
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.18f) else Color(0xFF1A1A1A))
                            .clickable { onPick(iconOption) }
                    ) {
                        Icon(
                            imageVector        = iconOption.toIcon(),
                            contentDescription = iconOption.name,
                            tint               = if (isSelected) accent else Color(0xFF888888),
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(SLOT_HEIGHT)
            .background(if (isActive) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (isActive) Color.White else ICON_INACTIVE,
            modifier           = Modifier.size(ICON_SIZE)
        )
    }
}

fun DefaultShortcutIcon.toIcon(): ImageVector = when (this) {
    // Navigation & vehicle
    DefaultShortcutIcon.RADIO       -> Icons.Default.Radio
    DefaultShortcutIcon.CAMERA      -> Icons.Default.CameraAlt
    DefaultShortcutIcon.PHONE       -> Icons.Default.Phone
    DefaultShortcutIcon.MAP         -> Icons.Default.Map
    DefaultShortcutIcon.NAVIGATION  -> Icons.Default.Navigation
    DefaultShortcutIcon.CAR         -> Icons.Default.DirectionsCar
    DefaultShortcutIcon.GAS_STATION -> Icons.Default.LocalGasStation
    DefaultShortcutIcon.DASHBOARD   -> Icons.Default.Speed
    // Audio & media
    DefaultShortcutIcon.MUSIC       -> Icons.Default.MusicNote
    DefaultShortcutIcon.SPEAKER     -> Icons.Default.Speaker
    DefaultShortcutIcon.HEADSET     -> Icons.Default.Headset
    DefaultShortcutIcon.EQUALIZER   -> Icons.Default.Equalizer
    DefaultShortcutIcon.VOLUME_UP   -> Icons.Default.VolumeUp
    // Connectivity
    DefaultShortcutIcon.BLUETOOTH   -> Icons.Default.Bluetooth
    DefaultShortcutIcon.WIFI        -> Icons.Default.Wifi
    // Lighting & climate
    DefaultShortcutIcon.LIGHTBULB   -> Icons.Default.Lightbulb
    DefaultShortcutIcon.BRIGHTNESS  -> Icons.Default.BrightnessHigh
    DefaultShortcutIcon.AC          -> Icons.Default.AcUnit
    DefaultShortcutIcon.THERMOSTAT  -> Icons.Default.Thermostat
    // General utility
    DefaultShortcutIcon.TV          -> Icons.Default.Tv
    DefaultShortcutIcon.VIDEOCAM    -> Icons.Default.Videocam
    DefaultShortcutIcon.STAR        -> Icons.Default.Star
    DefaultShortcutIcon.MESSAGE     -> Icons.Default.Message
    DefaultShortcutIcon.TIMER       -> Icons.Default.Timer
    DefaultShortcutIcon.LOCK        -> Icons.Default.Lock
    DefaultShortcutIcon.SETTINGS    -> Icons.Default.Settings
    DefaultShortcutIcon.FAVORITE    -> Icons.Default.Favorite
    // Web / location
    DefaultShortcutIcon.GLOBE       -> Icons.Default.Language
    DefaultShortcutIcon.NONE        -> Icons.Default.Apps
}
