package com.openlauncher.app.ui.widget

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openlauncher.app.data.MapProvider
import com.openlauncher.app.util.LocationData
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlauncher.app.viewmodel.LauncherViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun SpeedometerWidgetMaps(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val maxSpeed     = if (isMetric) 200f else 124f
    val speedDisplay = ((location?.speedMps ?: 0f) * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)
    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.07f
    val tickAlphaMaj = if (isDayMode) 0.50f else 0.28f
    val tickAlphaMin = if (isDayMode) 0.25f else 0.13f

    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subAlpha     = if (isDayMode) 0.55f else 0.32f
    val tickBaseColor = if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground

    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (digitalOnly) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier            = Modifier.fillMaxSize()
            ) {
                Text(
                    text          = "%.0f".format(speedDisplay),
                     color         = contentColor,
                     fontSize      = 54.sp,
                     fontWeight    = androidx.compose.ui.text.font.FontWeight.SemiBold,
                     letterSpacing = (-1.5).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text          = unitLabel,
                     color         = contentColor.copy(alpha = subAlpha * 1.5f),
                     fontSize      = 10.sp,
                     fontWeight    = androidx.compose.ui.text.font.FontWeight.Bold,
                     letterSpacing = 2.sp
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx    = size.width  / 2f
                val cy    = size.height / 2f
                val arcR  = minOf(size.width, size.height) * 0.37f
                val trackW = arcR * 0.13f
                val startAngle    = 150f
                val sweepTotal    = 240f
                val progressSweep = (speedDisplay / maxSpeed).coerceIn(0f, 1f) * sweepTotal

                val tl   = Offset(cx - arcR, cy - arcR)
                val sz   = Size(arcR * 2f, arcR * 2f)

                drawArc(
                    color      = contentColor.copy(alpha = trackAlpha),
                        startAngle = startAngle,
                        sweepAngle = sweepTotal,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(width = trackW, cap = StrokeCap.Round)
                )

                if (progressSweep > 0.5f) {
                    drawArc(
                        color      = accent,
                        startAngle = startAngle,
                        sweepAngle = progressSweep,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(width = trackW, cap = StrokeCap.Round)
                    )
                }

                for (i in 0..10) {
                    val angle   = startAngle + i * (sweepTotal / 10f)
                    val rad     = Math.toRadians(angle.toDouble())
                    val isMajor = i % 2 == 0
                    val outerR  = arcR - trackW / 2f - 3.dp.toPx()
                    val innerR  = outerR - if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                    drawLine(
                        color       = tickBaseColor.copy(alpha = if (isMajor) tickAlphaMaj else tickAlphaMin),
                             start       = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                             end         = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
                             strokeWidth = if (isMajor) 1.5.dp.toPx() else 0.8.dp.toPx()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.offset(y = (-4).dp)
            ) {
                Text(
                    text          = "%.0f".format(speedDisplay),
                     color         = contentColor,
                     fontSize      = 34.sp,
                     letterSpacing = (-1).sp
                )
                Text(
                    text          = unitLabel,
                     color         = contentColor.copy(alpha = subAlpha),
                     fontSize      = 8.sp,
                     letterSpacing = 2.sp
                )
            }
        }
    }
}


@Composable
fun MapWidget(
    location: LocationData?,
    mapProvider: MapProvider,
    accent: Color,
    isDayMode: Boolean = false,
    editMode: Boolean = false,
    onToggleProvider: () -> Unit,
              onLongClick: () -> Unit,
              modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFirstLoad by remember { mutableStateOf(true) }
    var autoFollow by remember { mutableStateOf(true) }
    val launcherViewModel: LauncherViewModel = viewModel()
    val settings by launcherViewModel.settings.collectAsState()
    val isMetric = settings.unitSystem.name == "METRIC"

    // --- 1. CONFIGURACIÓN DE CACHÉ OFFLINE EXTENDIDO ---
    LaunchedEffect(Unit) {
        val osmConfig = org.osmdroid.config.Configuration.getInstance()
        osmConfig.userAgentValue = context.packageName

        val cacheDir = File(context.cacheDir, "osmdroid_tiles")
        if (!cacheDir.exists()) cacheDir.mkdirs()
            osmConfig.osmdroidTileCache = cacheDir

            osmConfig.tileFileSystemCacheMaxBytes = 900L * 1024 * 1024
            osmConfig.tileFileSystemCacheTrimBytes = 700L * 1024 * 1024
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.0)
        }
    }

    // Listener para desactivar el auto-seguimiento si el usuario arrastra el mapa manualmente
    DisposableEffect(mapView) {
        val listener = object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                if (event?.source?.isAnimating == false) {
                    autoFollow = false
                }
                return true
            }
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    // Overlay de eventos de presión larga
    DisposableEffect(mapView) {
        val receiver = object : org.osmdroid.events.MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                onLongClick()
                return true
            }
        }
        val eventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(receiver)
        mapView.overlays.add(eventsOverlay)
        onDispose { mapView.overlays.remove(eventsOverlay) }
    }

    // Indicador estilo Google Maps (Borde blanco + Centro Accent)
    val marker = remember(accent) {
        Marker(mapView).apply {
            val size = (32 * context.resources.displayMetrics.density).toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val paintDisc = Paint().apply {
                isAntiAlias = true
                color = Color.White.toArgb()
                setShadowLayer(6f, 0f, 3f, android.graphics.Color.argb(80, 0, 0, 0))
            }
            val paintArrow = Paint().apply {
                isAntiAlias = true
                color = accent.toArgb()
            }
            val paintShadow = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(50, 0, 0, 0)
                maskFilter = android.graphics.BlurMaskFilter(2f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }

            canvas.drawCircle(size / 2f, size / 2f, size / 2.4f, paintDisc)

            val path = android.graphics.Path().apply {
                moveTo(size / 2f, size / 3.5f)
                lineTo(size / 1.35f, size / 1.5f)
                lineTo(size / 2f, size / 1.7f)
                lineTo(size / 3.7f, size / 1.5f)
                close()
            }

            val shadowPath = android.graphics.Path(path).apply { offset(1f, 2f) }
            canvas.drawPath(shadowPath, paintShadow)
            canvas.drawPath(path, paintArrow)

            icon = BitmapDrawable(context.resources, bitmap)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
    }

    // --- 2. FUNCIÓN DE ZOOM DINÁMICO ---
    fun getZoomByAccuracy(accuracyInMeters: Float?): Double {
        if (accuracyInMeters == null || accuracyInMeters <= 0) return 15.0
            return when {
                accuracyInMeters < 15f -> 17.5
                accuracyInMeters < 50f -> 16.5
                accuracyInMeters < 150f -> 16.0
                else -> 15.0
            }
    }

    // Actualizar posición del marcador y aplicar auto-centrado con Zoom dinámico
    LaunchedEffect(location, autoFollow) {
        location?.let { loc ->
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            marker.position = geoPoint

            // SOLUCIÓN: Agregado ?: 0f para manejar de forma segura el Float? opcional
            marker.rotation = loc.bearing ?: 0f

            if (!mapView.overlays.contains(marker)) {
                mapView.overlays.add(marker)
            }

            if (isFirstLoad || autoFollow) {
                val targetZoom = getZoomByAccuracy(loc.accuracy)
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(targetZoom)
                isFirstLoad = false
            }
            mapView.invalidate()
        }
    }

    // Proveedores de mapas (Google / OSM)
    LaunchedEffect(mapProvider) {
        if (mapProvider == MapProvider.GOOGLE) {
            val googleTiles = object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                "GoogleRoads", 0, 20, 256, "",
                arrayOf(
                    "https://mt0.google.com/vt/lyrs=m",
                    "https://mt1.google.com/vt/lyrs=m",
                    "https://mt2.google.com/vt/lyrs=m",
                    "https://mt3.google.com/vt/lyrs=m"
                )
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                    val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                    val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                    return getBaseUrl() + "&x=" + x + "&y=" + y + "&z=" + zoom
                }
            }
            mapView.setTileSource(googleTiles)
        } else {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
        }
    }

    // Modo noche/día
    LaunchedEffect(isDayMode) {
        if (isDayMode) {
            mapView.overlayManager.tilesOverlay.setColorFilter(null)
        } else {
            val matrix = floatArrayOf(
                -0.6f,  0f,    0f,    0f, 200f,
                0f,   -0.6f,  0f,    0f, 200f,
                0f,    0f,   -0.6f,  0f, 200f,
                0f,    0f,    0f,    1.0f, 0f
            )
            mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
        }
        mapView.invalidate()
    }

    Box(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // UI Overlay: Cambiar de proveedor
        Box(
            modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onToggleProvider() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(
                    text = if (mapProvider == MapProvider.GOOGLE) "GOOGLE" else "OSM",
                     color = Color.White,
                     fontSize = 10.sp,
                     style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // UI Overlay: Botones de Zoom manual
        // UI Overlay: Botones de Zoom manual
        Column(
            modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp),
               verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Botón de Zoom In
            Box(
                modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable {
                    autoFollow = false
                    mapView.controller.zoomIn()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                     contentDescription = "Zoom In",
                     tint = Color.White,
                     modifier = Modifier.size(16.dp)
                )
            }

            // Botón de Zoom Out
            Box(
                modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable {
                    autoFollow = false
                    mapView.controller.zoomOut()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                     contentDescription = "Zoom Out",
                     tint = Color.White,
                     modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- VELOCÍMETRO UBICADO ABAJO A LA IZQUIERDA ---
        SpeedometerWidgetMaps(
            location = location,
            isMetric = isMetric,
            accent = accent,
            isDayMode = isDayMode,
            modifier = Modifier
            .align(Alignment.BottomStart) // Lo posiciona abajo a la izquierda
            .padding(10.dp)               // Margen para que no toque los bordes
            .size(110.dp)                 // Define un tamaño para el Canvas circular
            .offset(x = (-14).dp, y = 36.dp)
        )

        // UI Overlay: Botón de Recentrar / GPS
        IconButton(
            onClick = {
                autoFollow = true
                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    val targetZoom = getZoomByAccuracy(it.accuracy)
                    mapView.controller.animateTo(geoPoint)
                    mapView.controller.setZoom(targetZoom)
                }
            },
            modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                 contentDescription = "Center on location",
                 tint = Color.White,
                 modifier = Modifier.size(16.dp)
            )
        }

        if (editMode) {
            Box(
                modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .pointerInput(Unit) {}
            )
        }
    }
}
