package com.tariq.composemusicplayer.media.exoplayer

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Constraints
import com.tariq.composemusicplayer.data.model.AudioItem
import com.tariq.composemusicplayer.media.constans.Constants
import com.tariq.composemusicplayer.media.service.MediaPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ExoPlayerServiceConnection @Inject constructor(
    @ApplicationContext context: Context
) {

    private val _playBackState: MutableStateFlow<PlaybackStateCompat?> =
        MutableStateFlow(null)
    val playbackState: StateFlow<PlaybackStateCompat?>
        get() = _playBackState

    private val _isConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean>
        get() = _isConnected

    val currentPlayingMusic = mutableStateOf<AudioItem?>(null)

    lateinit var mediaControllerCompat: MediaControllerCompat

    private val mediaBrowserServiceCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MediaPlayerService::class.java),
        mediaBrowserServiceCallback,
        null
    ).apply {
        connect()
    }

    private var audioList = listOf<AudioItem>()

    val rootMediaId: String
        get() = mediaBrowser.root

    val transportControl: MediaControllerCompat.TransportControls
        get() = mediaControllerCompat.transportControls


    fun playMusic(musicList: List<AudioItem>) {
        Log.i("CLick", "playMusic: ")
        audioList = musicList
        mediaBrowser.sendCustomAction(Constants.START_MEDIA_PLAY_ACTION, null, null)
    }

    fun fastForward(seconds: Int = 10){
        playbackState.value?.currentPosition?.let {
            transportControl.seekTo(it + seconds * 1000)
        }
    }

    fun rewind(seconds: Int = 10){
        playbackState.value?.currentPosition?.let {
            transportControl.seekTo(it - seconds * 1000)
        }
    }

    fun skipToNext(){
        transportControl.skipToNext()
    }

    fun subscribe(
        parentId:String,
        callback: MediaBrowserCompat.SubscriptionCallback
    ){
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unSubscribe(
        parentId:String,
        callback: MediaBrowserCompat.SubscriptionCallback
    ){
        mediaBrowser.unsubscribe(parentId, callback)
    }


    fun refreshMediaBrowserChildren(){
        mediaBrowser.sendCustomAction(Constants.REFRESH_MEDIA_PLAY_ACTION, null, null)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            _isConnected.value = true
            mediaControllerCompat = MediaControllerCompat(
                context,
                mediaBrowser.sessionToken,
            ).apply {
                registerCallback(MediaControllerCallback())
            }
        }

        override fun onConnectionSuspended() {
            _isConnected.value = false
        }

        override fun onConnectionFailed() {
            _isConnected.value = false
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            _playBackState.value = state

        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            currentPlayingMusic.value = metadata?.let { data ->
                audioList.find {
                    it.id.toString() == data.description.mediaId
                }

            }

        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserServiceCallback.onConnectionSuspended()
        }
    }
}