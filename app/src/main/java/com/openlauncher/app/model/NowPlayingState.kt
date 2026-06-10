package com.openlauncher.app.model

import android.graphics.Bitmap
import android.media.session.MediaController

data class NowPlayingState(
    val title: String,
    val artist: String,
    val albumArt: Bitmap?,
    // Full-resolution artwork URI when the source app provides one —
    // preferred over the (often downscaled) metadata bitmap
    val artUri: String? = null,
    val isPlaying: Boolean,
    val controller: MediaController?
)
