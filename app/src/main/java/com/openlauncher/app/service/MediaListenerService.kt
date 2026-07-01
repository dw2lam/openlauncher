package com.openlauncher.app.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.openlauncher.app.model.NowPlayingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MediaListenerService : NotificationListenerService() {

    private var activeController: MediaController? = null

        private val controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = refreshNowPlaying()
            override fun onMetadataChanged(metadata: MediaMetadata?)   = refreshNowPlaying()
            override fun onSessionDestroyed()                          = refreshNowPlaying()
        }

        override fun onListenerConnected() {
            super.onListenerConnected()
            instance = this
            isConnected.value = true
            refreshNowPlaying()
        }

        override fun onListenerDisconnected() {
            super.onListenerDisconnected()
            instance = null
            isConnected.value = false
            clearController()
            // CAMBIO: No destruyas el estado con null, deja que persista el último cargado
        }

        override fun onNotificationPosted(sbn: StatusBarNotification?)  { refreshNowPlaying() }
        override fun onNotificationRemoved(sbn: StatusBarNotification?) { refreshNowPlaying() }

        override fun onDestroy() {
            instance = null
            clearController()
            // CAMBIO: Al morir el servicio, no limpies _nowPlaying a null si quieres mantener la última canción visible en el Widget offline
            super.onDestroy()
        }

        private fun clearController() {
            activeController?.unregisterCallback(controllerCallback)
            activeController = null
        }

        private fun refreshNowPlaying() {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val sessions: List<MediaController> = try {
                msm.getActiveSessions(ComponentName(this, MediaListenerService::class.java))
            } catch (_: SecurityException) {
                emptyList()
            }

            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()

            if (active == null) {
                clearController()
                // CAMBIO: Si se pausan/cierran todas las apps, conservamos el estado previo en la UI en modo "pausado"
                _nowPlaying.value = _nowPlaying.value?.copy(isPlaying = false)
                return
            }

            if (active.sessionToken != activeController?.sessionToken) {
                clearController()
                activeController = active
                active.registerCallback(
                    controllerCallback,
                    android.os.Handler(android.os.Looper.getMainLooper())
                )
            }

            updateFromController(activeController)
        }

        private fun updateFromController(controller: MediaController?) {
            if (controller == null) return

                val pkg = controller.packageName
                lastMediaPackage = pkg

                val meta = controller.metadata
                val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "Unknown"
                val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: ""

                val art = try {
                    listOfNotNull(
                        meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                                  meta?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                                  meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                    ).maxByOrNull { it.width * it.height }
                } catch (_: Exception) { null }

                val artUri = meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
                val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

                // PERSISTENCIA FÍSICA: Guardamos en SharedPreferences cada vez que detectamos cambios válidos
                if (title != "Unknown") {
                    val prefs = getSharedPreferences("media_cache", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("last_title", title)
                        putString("last_artist", artist)
                        putString("last_package", pkg)
                        putString("last_art_uri", artUri)
                        apply()
                    }
                }

                val prev = _nowPlaying.value
                val sameSong = prev?.title == title && prev.artist == artist
                val sameArt = prev?.artUri == artUri && artUri != null

                if (prev != null &&
                    prev.controller?.sessionToken == controller.sessionToken &&
                    sameSong &&
                    prev.isPlaying == isPlaying &&
                    sameArt
                ) return

                _nowPlaying.value = NowPlayingState(
                    title      = title,
                    artist     = artist,
                    albumArt   = art,
                    artUri     = artUri,
                    isPlaying  = isPlaying,
                    controller = controller
                )
        }

        companion object {
            private val _nowPlaying = MutableStateFlow<NowPlayingState?>(null)
            val nowPlaying: StateFlow<NowPlayingState?> = _nowPlaying
            val isConnected = MutableStateFlow(false)
            var lastMediaPackage: String = ""

            @Volatile private var instance: MediaListenerService? = null
            fun requestRefresh() { instance?.refreshNowPlaying() }

            /**
             * Inicializa el estado global usando el último registro guardado.
             * Llama a esta función en el `onCreate` de tu MainActivity o LauncherApplication.
             */
            fun initializeFromCache(context: Context) {
                val prefs = context.getSharedPreferences("media_cache", Context.MODE_PRIVATE)
                val title = prefs.getString("last_title", "") ?: ""
                val artist = prefs.getString("last_artist", "") ?: ""
                val pkg = prefs.getString("last_package", "") ?: ""
                val artUri = prefs.getString("last_art_uri", null)

                if (title.isNotEmpty()) {
                    lastMediaPackage = pkg
                    _nowPlaying.value = NowPlayingState(
                        title = title,
                        artist = artist,
                        albumArt = null, // Los Bitmaps grandes no se guardan en SharedPreferences
                        artUri = artUri,
                        isPlaying = false,
                        controller = null
                    )
                }
            }
        }
}
