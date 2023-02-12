package com.tariq.composemusicplayer.data

import com.tariq.composemusicplayer.data.model.AudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MusicRepository @Inject constructor(private val contentResolverHelper: ContentResolverHelper) {
    suspend fun getAudioData():List<AudioItem> = withContext(Dispatchers.IO){
        contentResolverHelper.getAudioData()
    }
}