package com.openlauncher.app.model

import android.graphics.Bitmap
import android.media.session.MediaController

data class NowPlayingState(
    val title: String,
    val artist: String,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val controller: MediaController?
)
