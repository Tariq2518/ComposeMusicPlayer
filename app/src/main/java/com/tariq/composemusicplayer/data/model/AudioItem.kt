package com.tariq.composemusicplayer.data.model

import android.graphics.Bitmap
import android.net.Uri

data class AudioItem(
    val id:Long,
    val uri: Uri,
    val displayName:String,
    val artist: String,
    val duration: Int,
    val title: String,
    val data: String,
    val artWork: Bitmap?,

    )
