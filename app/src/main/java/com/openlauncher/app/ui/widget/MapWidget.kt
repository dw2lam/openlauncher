package com.openlauncher.app.ui.widget

import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlauncher.app.data.MapProvider
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.viewmodel.LauncherViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import com.openlauncher.app.data.NominatimApi
import com.openlauncher.app.data.NominatimPlace
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.TextUnit

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


/**
 * Un composable de soporte para animar un dígito individual con un efecto de "rodillo"
 * o revelación vertical con desvanecimiento.
 *
 * @param targetDigit El dígito objetivo (0-9).
 * @param color El color del texto.
 * @param fontSize El tamaño de la fuente.
 * @param fontWeight El grosor de la fuente.
 * @param letterSpacing El espaciado entre letras.
 * @param fontFamily La familia de fuentes.
 * @param moveUp Si es verdadero, el dígito viejo sale hacia arriba y el nuevo entra desde abajo.
 * Si es falso, sale hacia abajo y entra desde arriba.
 * @param modifier Modificador opcional.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RollingDigit(
    targetDigit: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit,
    moveUp: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = targetDigit,
        transitionSpec = {
            // ruede rápido y no se acumulen animaciones colisionadas.
            if (moveUp) {
                (slideInVertically(animationSpec = tween(300)) { height -> height } + fadeIn()) togetherWith
                (slideOutVertically(animationSpec = tween(300)) { height -> -height } + fadeOut())
            } else {
                (slideInVertically(animationSpec = tween(300)) { height -> -height } + fadeIn()) togetherWith
                (slideOutVertically(animationSpec = tween(300)) { height -> height } + fadeOut())
            }
        },
        modifier = modifier,
        label = "RollingDigitAnimation"
    ) { digit ->
        Text(
            text = digit,
             color = color,
             fontSize = fontSize,
             fontWeight = fontWeight,
             letterSpacing = letterSpacing,
        )
    }
}

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
    val rawSpeedMps = location?.speedMps ?: 0f
    val filteredSpeedMps = if (rawSpeedMps < 0.5f) 0f else rawSpeedMps
    val targetSpeed = (filteredSpeedMps * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)

    // Esto obliga al sistema a rellenar el vacío entre los calculos de la velocidad,
    // dándole tiempo al usuario de ver la transición numérica.
    val animatedSpeed by animateFloatAsState(
        targetValue = targetSpeed,
        animationSpec = tween(
            durationMillis = 900,
            easing = LinearOutSlowInEasing
        ),
        label = "SpeedAnimation"
    )

    // El resto de los cálculos numéricos usando el valor animado y fluido
    val speed = animatedSpeed.toInt()
    val hundredsText = if (speed >= 100) ((speed / 100) % 10).toString() else ""
    val tensText = if (speed >= 10) ((speed / 10) % 10).toString() else ""
    val onesText = (speed % 10).toString()

    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.07f
    val tickAlphaMaj = if (isDayMode) 0.50f else 0.28f
    val tickAlphaMin = if (isDayMode) 0.25f else 0.13f

    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subAlpha      = if (isDayMode) 0.55f else 0.32f
    val tickBaseColor = if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground

    Box(
        modifier          = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (digitalOnly) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier            = Modifier.fillMaxSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RollingDigit(
                        targetDigit = hundredsText,
                        color = contentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                                 moveUp = false
                    )
                    RollingDigit(
                        targetDigit = tensText,
                        color = contentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                                 moveUp = true
                    )
                    RollingDigit(
                        targetDigit = onesText,
                        color = contentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                                 moveUp = false
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text          = unitLabel,
                     color         = contentColor.copy(alpha = subAlpha * 1.5f),
                     fontSize      = 10.sp,
                     fontWeight    = FontWeight.Bold,
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
                val progressSweep = (animatedSpeed / maxSpeed).coerceIn(0f, 1f) * sweepTotal

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RollingDigit(
                        targetDigit = hundredsText,
                        color = contentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                                 moveUp = false
                    )
                    RollingDigit(
                        targetDigit = tensText,
                        color = contentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                                 moveUp = true
                    )
                    RollingDigit(
                        targetDigit = onesText,
                        color = contentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                                 moveUp = false
                    )
                }
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
    bearing: Float,
    mapProvider: MapProvider,
    mapType: com.openlauncher.app.data.MapType = com.openlauncher.app.data.MapType.ROADMAP,
    showTraffic: Boolean = false,
    accent: Color,
    isDayMode: Boolean = false,
    editMode: Boolean = false,
    onToggleProvider: () -> Unit,
    onToggleTraffic: () -> Unit = {},
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isFirstLoad by remember { mutableStateOf(true) }
    var autoFollow by remember { mutableStateOf(true) }
    val launcherViewModel: LauncherViewModel = viewModel()
    val settings by launcherViewModel.settings.collectAsState()
    val isMetric = settings.unitSystem.name == "METRIC"

    // --- Valhalla Navigation state collection ---
    val activeRoutePoints by launcherViewModel.activeRoutePoints.collectAsState()
    val isNavigating by launcherViewModel.isNavigating.collectAsState()
    val routeSummary by launcherViewModel.routeSummary.collectAsState()
    val destinationName by launcherViewModel.destinationName.collectAsState()
    val currentManeuver by launcherViewModel.currentManeuver.collectAsState()
    val distanceToManeuver by launcherViewModel.distanceToManeuver.collectAsState()
    val routeBearing by launcherViewModel.routeBearing.collectAsState()

    // --- Search Query state ---
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<NominatimPlace>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // --- Auxiliar para verificar estado de la conexión ---
    fun isConnected(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Monitoreamos reactivamente si hay conexión disponible
    var isConnected by remember { mutableStateOf(isConnected(context)) }

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

    // Route Polyline
    val routePolyline = remember(accent) {
        Polyline().apply {
            outlinePaint.color = accent.toArgb()
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        }
    }

    LaunchedEffect(activeRoutePoints) {
        mapView.overlays.remove(routePolyline)
        if (!activeRoutePoints.isNullOrEmpty()) {
            routePolyline.setPoints(activeRoutePoints)
            mapView.overlays.add(routePolyline)
            
            // Adjust bounds to show preview
            routeSummary?.let { summary ->
                val boundingBox = org.osmdroid.util.BoundingBox(
                    summary.maxLat, summary.maxLon, summary.minLat, summary.minLon
                )
                mapView.zoomToBoundingBox(boundingBox, true, 80)
            }
        }
        mapView.invalidate()
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
                if (p != null && location != null) {
                    launcherViewModel.calculateRoute(
                        startLat = location.latitude,
                        startLon = location.longitude,
                        destLat = p.latitude,
                        destLon = p.longitude,
                        name = "Punto en el mapa"
                    )
                } else {
                    onLongClick()
                }
                return true
            }
        }
        val eventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(receiver)
        mapView.overlays.add(eventsOverlay)
        onDispose { mapView.overlays.remove(eventsOverlay) }
    }

    // Indicador estilo Google Maps (con flecha al centro)
    val marker = remember(accent) {
        Marker(mapView).apply {
            val size = (32 * context.resources.displayMetrics.density).toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val paintDisc = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
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
            rotation = 0f
        }
    }

    // Función de zoom dinámico
    fun getZoomByAccuracy(accuracyInMeters: Float?): Double {
        if (accuracyInMeters == null || accuracyInMeters <= 0) return 15.0
            return when {
                accuracyInMeters < 15f -> 17.0
                accuracyInMeters < 50f -> 16.5
                accuracyInMeters < 150f -> 15.5
                else -> 15.0
            }
    }

    // Guardamos de manera persistente el último rumbo válido para evitar saltos locos al detenerse
    var lastValidBearing by remember { mutableStateOf(0f) }

    LaunchedEffect(location, bearing, autoFollow){
        isConnected = isConnected(context)

        // Cambia dinámicamente el comportamiento de osmdroid
        if (isConnected) {
            mapView.setUseDataConnection(true) // Permite descargar libremente desde Internet
        } else {
            mapView.setUseDataConnection(false) // Fuerza el modo 100% Offline (bloquea peticiones HTTP de osmdroid)
        }

        location?.let { loc ->
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            marker.position = geoPoint

            if (!mapView.overlays.contains(marker)) {
                mapView.overlays.add(marker)
            }

            val currentBearing = bearing

            // Siempre guardar el último bearing recibido
            lastValidBearing = currentBearing

            val targetBearing: Float = when {
                routeBearing != null && isNavigating -> routeBearing!!
                else -> currentBearing
            }

            if (autoFollow) {
                mapView.mapOrientation = -targetBearing.toFloat()
                android.util.Log.d(
                    "MAP_ROTATION",
                    "orientation=${mapView.mapOrientation}"
                )
                marker.rotation = 0f
                mapView.controller.animateTo(geoPoint)
            } else {
                // Modo exploración manual (Mapa estático hacia el Norte)
                mapView.mapOrientation = -targetBearing
                marker.rotation = targetBearing
            }

            if (isFirstLoad) {
                mapView.controller.setZoom(17.5)
                mapView.controller.setCenter(geoPoint)
                isFirstLoad = false
            }

            mapView.invalidate()
        }
    }

    // Proveedores de mapas (Google / OSM)
    LaunchedEffect(mapProvider, mapType, showTraffic) {
        if (mapProvider == MapProvider.GOOGLE) {
            val layerCode = when (mapType) {
                com.openlauncher.app.data.MapType.ROADMAP -> "m"
                com.openlauncher.app.data.MapType.SATELLITE -> "s"
                com.openlauncher.app.data.MapType.HYBRID -> "y"
                com.openlauncher.app.data.MapType.TERRAIN -> "p"
            }
            val layer = if (showTraffic) "$layerCode,traffic" else layerCode
            val googleTiles = object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                "GoogleRoads", 0, 20, 256, "",
                arrayOf(
                    "https://mt0.google.com/vt/lyrs=$layer",
                    "https://mt1.google.com/vt/lyrs=$layer",
                    "https://mt2.google.com/vt/lyrs=$layer",
                    "https://mt3.google.com/vt/lyrs=$layer"
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

    // Modo noche/día (Estilo Android Auto Dark)
    LaunchedEffect(isDayMode) {
        if (isDayMode) {
            mapView.overlayManager.tilesOverlay.setColorFilter(null)
        } else {
            // Matriz optimizada para imitar el look de Android Auto / Google Maps Night
            // 1. Invertimos ligeramente los colores (-0.8f)
            // 2. Aplicamos un tinte azulado profundo (Offset 40 en Rojo, 50 en Verde, 70 en Azul)
            // 3. Ajustamos el brillo para que no sea excesivamente oscuro pero sí confortable
            val matrix = floatArrayOf(
                -0.8f, 0f, 0f, 0f, 220f,
                0f, -0.8f, 0f, 0f, 225f,
                0f, 0f, -0.8f, 0f, 245f,
                0f, 0f, 0f, 1.0f, 0f
            )
            mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
        }
        mapView.invalidate()
    }

    Box(modifier = modifier.clip(RoundedCornerShape(4.dp))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // UI Overlay: Cambiar de proveedor e indicador de estado de red
        Box(
            modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onToggleProvider() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                // Agregamos un indicador visual "(OFFLINE)" en el texto por si estás en ruta sin conexión
                Text(
                    text = (if (mapProvider == MapProvider.GOOGLE) "GOOGLE" else "OSM") +
                    (if (mapProvider == MapProvider.GOOGLE) " (${mapType.name})" else "") +
                    (if (showTraffic && mapProvider == MapProvider.GOOGLE) " + TRAFFIC" else "") +
                    (if (!isConnected) " (OFFLINE)" else ""),
                     color = if (isConnected) Color.White else Color.Yellow,
                     fontSize = 10.sp,
                     style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Botón de Tráfico (Solo visible si es Google)
        if (mapProvider == MapProvider.GOOGLE) {
            Row(
                modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 50.dp, start = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Togle Tráfico
                Box(
                    modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (showTraffic) accent.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f))
                    .clickable { onToggleTraffic() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TRAFFIC",
                         color = Color.White,
                         fontSize = 10.sp,
                         fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                // Ciclo de Tipo de Mapa
                Box(
                    modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable {
                        val next = when (mapType) {
                            com.openlauncher.app.data.MapType.ROADMAP -> com.openlauncher.app.data.MapType.SATELLITE
                            com.openlauncher.app.data.MapType.SATELLITE -> com.openlauncher.app.data.MapType.HYBRID
                            com.openlauncher.app.data.MapType.HYBRID -> com.openlauncher.app.data.MapType.TERRAIN
                            com.openlauncher.app.data.MapType.TERRAIN -> com.openlauncher.app.data.MapType.ROADMAP
                        }
                        launcherViewModel.setMapType(next)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "VIEW",
                         color = Color.White,
                         fontSize = 10.sp,
                         fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        // UI Overlay: Botones de Zoom manual
        Column(
            modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp),
               verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
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
                     modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
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
                     modifier = Modifier.size(18.dp)
                )
            }
        }

        // VELOCÍMETRO
        SpeedometerWidgetMaps(
            location = location,
            isMetric = isMetric,
            accent = accent,
            isDayMode = isDayMode,
            modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp)
            .size(110.dp)
            .offset(x = (-14).dp, y = 36.dp)
        )

        // UI Overlay: Botón de Recentrar / GPS
        IconButton(
            onClick = {
                autoFollow = true
                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(geoPoint)
                    mapView.controller.setZoom(17.5)
                    if (it.bearing != null) {
                        mapView.mapOrientation = -it.bearing!!
                    }
                }
            },
            modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.GpsFixed, contentDescription = "Center on location", tint = Color.White, modifier = Modifier.size(16.dp))
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
