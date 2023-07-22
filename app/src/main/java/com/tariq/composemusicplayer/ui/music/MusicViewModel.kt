package com.tariq.composemusicplayer.ui.music

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tariq.composemusicplayer.data.MusicRepository
import com.tariq.composemusicplayer.data.model.AudioItem
import com.tariq.composemusicplayer.media.constans.Constants
import com.tariq.composemusicplayer.media.exoplayer.ExoPlayerServiceConnection
import com.tariq.composemusicplayer.media.exoplayer.currentPosition
import com.tariq.composemusicplayer.media.exoplayer.isPlaying
import com.tariq.composemusicplayer.media.service.MediaPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    serviceConnection: ExoPlayerServiceConnection
) : ViewModel() {
    var musicList = mutableStateListOf<AudioItem>()
    val currentPlayingMusic = serviceConnection.currentPlayingMusic
    private val isConnected = serviceConnection.isConnected

    lateinit var rootMediaId: String
    var currentPlaybackPosition by mutableStateOf(0L)
    private var updatePosition = true
    private val playbackState = serviceConnection.playbackState
    val isMusicPlaying: Boolean
        get() = playbackState.value?.isPlaying == true

    private val subscriptionCallback = object :
        MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
        }

    }
    private val serviceConnection = serviceConnection.also {
        updatePlayback()
    }
    var currentMusicProgress = mutableStateOf(0f)
    private val currentDuration: Long
        get() = MediaPlayerService.currentDuration

    init {
        viewModelScope.launch {
            musicList += getAndFormatMusicData()
            isConnected.collect {
                if (it) {
                    rootMediaId = serviceConnection.rootMediaId
                    serviceConnection.playbackState.value?.apply {
                        currentPlaybackPosition = position
                    }
                    serviceConnection.subscribe(rootMediaId, subscriptionCallback)
                }
            }
        }
    }

    private suspend fun getAndFormatMusicData(): List<AudioItem> {
        return repository.getAudioData().map {
            val displayName = it.displayName.substringBefore(".")
            val artist = if (it.artist.contains("<unknown>")) "Unknow Artist" else it.artist
            it.copy(
                displayName = displayName,
                artist = artist
            )
        }
    }

    fun playMusic(currentMusic: AudioItem) {
        serviceConnection.playMusic(musicList)
        Log.i("CLick", "playMusic1: ${currentMusic.id == currentPlayingMusic.value?.id}, isMusicPlaying= $isMusicPlaying")
        if (currentMusic.id == currentPlayingMusic.value?.id) {
            if (isMusicPlaying) {
                serviceConnection.transportControl.pause()
            } else {
                serviceConnection.transportControl.play()
            }
        } else {
            Log.i("CLick", "playMusic: ${currentMusic.id}")
            serviceConnection.transportControl.playFromMediaId(currentMusic.id.toString(), null)
        }
    }

    fun stopPlayback() {
        serviceConnection.transportControl.stop()
    }

    fun fasForward() {
        serviceConnection.fastForward()
    }

    fun rewind() {
        serviceConnection.rewind()
    }

    fun skipToNext() {
        serviceConnection.skipToNext()
    }

    fun seekTo(seek: Float) {
        serviceConnection.transportControl.seekTo(
            (currentDuration * seek / 100f).toLong()
        )
    }

    private fun updatePlayback() {
        viewModelScope.launch {
            val position = playbackState.value?.currentPosition ?: 0
            if (currentPlaybackPosition != position) {
                currentPlaybackPosition = position
            }

            if (currentDuration > 0) {
                currentMusicProgress.value = (
                        currentPlaybackPosition.toFloat()
                                / currentDuration.toFloat() * 100f
                        )
            }

            delay(Constants.PLAYBACK_UPDATE_INTERVAL)

            if (updatePosition) {
                updatePlayback()
            }


        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unSubscribe(
            Constants.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {})
        updatePosition = false
    }
}