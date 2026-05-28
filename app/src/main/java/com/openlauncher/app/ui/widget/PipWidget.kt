package com.openlauncher.app.ui.widget

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PipWidget(
    packageName: String,
    accent: Color,
    isDayMode: Boolean,
    isEditing: Boolean,
    onLaunch: () -> Unit,
    onAssign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileBg   = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF0B0B0B)
    val dimColor = if (isDayMode) Color(0xFF888888) else Color(0xFF555555)
    val context  = LocalContext.current
    val rootView = LocalView.current

    if (packageName.isEmpty()) {
        Column(
            modifier            = modifier.background(tileBg)
                .then(if (!isEditing) Modifier.clickable(onClick = onAssign) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Add, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                "ASSIGN APP",
                color         = dimColor,
                fontSize      = 8.sp,
                letterSpacing = 1.5.sp,
                textAlign     = TextAlign.Center
            )
        }
        return
    }

    var screenRect by remember { mutableStateOf<Rect?>(null) }

    val pm    = context.packageManager
    val icon  = remember(packageName) {
        runCatching { pm.getApplicationIcon(packageName).toBitmap(96, 96) }.getOrNull()
    }
    val label = remember(packageName) {
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    key(packageName) {
        EmbeddedAppView(
            packageName = packageName,
            modifier = modifier
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .background(tileBg)
                    .onGloballyPositioned { coords ->
                        val rootPos = IntArray(2)
                        rootView.getLocationOnScreen(rootPos)
                        val winPos = coords.positionInWindow()
                        screenRect = Rect(
                            rootPos[0] + winPos.x.toInt(),
                            rootPos[1] + winPos.y.toInt(),
                            rootPos[0] + winPos.x.toInt() + coords.size.width,
                            rootPos[1] + winPos.y.toInt() + coords.size.height
                        )
                    }
                    .then(if (!isEditing) Modifier.clickable {
                        val intent = pm.getLaunchIntentForPackage(packageName)
                            ?: pm.queryIntentActivities(
                                Intent(Intent.ACTION_MAIN).setPackage(packageName), 0
                            ).firstOrNull()?.activityInfo?.let { ai ->
                                Intent(Intent.ACTION_MAIN).apply {
                                    setClassName(ai.packageName, ai.name)
                                }
                            }
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            val rect = screenRect
                            if (rect != null) {
                                val opts = ActivityOptions.makeBasic()
                                runCatching {
                                    val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", java.lang.Integer.TYPE)
                                    method.invoke(opts, 5) // WINDOWING_MODE_FREEFORM
                                }
                                opts.launchBounds = rect
                                context.startActivity(intent, opts.toBundle())
                            } else {
                                context.startActivity(intent)
                            }
                        }
                    } else Modifier)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (icon != null) {
                        androidx.compose.foundation.Image(
                            painter            = BitmapPainter(icon.asImageBitmap()),
                            contentDescription = label,
                            modifier           = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, null, tint = accent, modifier = Modifier.size(48.dp))
                    }
                    Text(
                        text          = label.uppercase(),
                        color         = dimColor,
                        fontSize      = 8.sp,
                        letterSpacing = 1.5.sp,
                        textAlign     = TextAlign.Center,
                        maxLines      = 1
                    )
                    Text(
                        text          = "TAP TO OPEN",
                        color         = accent.copy(alpha = 0.6f),
                        fontSize      = 7.sp,
                        letterSpacing = 1.sp,
                        textAlign     = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EmbeddedAppView(
    packageName: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }

    var useFallback by remember { mutableStateOf(false) }

    if (useFallback) {
        fallback()
    } else {
        AndroidView(
            factory = { ctx ->
                try {
                    val activityViewClass = Class.forName("android.app.ActivityView")
                    val constructor = activityViewClass.getConstructor(android.content.Context::class.java)
                    val activityView = constructor.newInstance(ctx) as android.view.ViewGroup

                    val surfaceView = (0 until activityView.childCount)
                        .map { activityView.getChildAt(it) }
                        .filterIsInstance<android.view.SurfaceView>()
                        .firstOrNull()

                    val launchApp = {
                        runCatching {
                            val intent = pm.getLaunchIntentForPackage(packageName)
                                ?: pm.queryIntentActivities(
                                    Intent(Intent.ACTION_MAIN).setPackage(packageName), 0
                                ).firstOrNull()?.activityInfo?.let { ai ->
                                    Intent(Intent.ACTION_MAIN).apply {
                                        setClassName(ai.packageName, ai.name)
                                    }
                                }
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                val method = activityViewClass.getMethod("startActivity", Intent::class.java)
                                method.invoke(activityView, intent)
                            }
                        }
                    }

                    if (surfaceView != null) {
                        surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                launchApp()
                            }
                            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                runCatching {
                                    val method = activityViewClass.getMethod("release")
                                    method.invoke(activityView)
                                }
                            }
                        })
                        if (surfaceView.holder.surface?.isValid == true) {
                            launchApp()
                        }
                    } else {
                        // If no SurfaceView child, trigger fallback/post
                        activityView.post {
                            launchApp()
                        }
                    }

                    activityView
                } catch (e: Exception) {
                    useFallback = true
                    android.view.View(ctx)
                }
            },
            modifier = modifier.fillMaxSize(),
            update = { _ -> }
        )
    }
}
