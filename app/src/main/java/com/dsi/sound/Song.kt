package com.dsi.sound

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,   // milliseconds
    val uri: Uri
) {
    val durationFormatted: String
        get() {
            val seconds = (duration / 1000) % 60
            val minutes = (duration / 1000) / 60
            return "%d:%02d".format(minutes, seconds)
        }
}
