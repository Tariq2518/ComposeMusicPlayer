package com.tariq.composemusicplayer.media.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.tariq.composemusicplayer.media.constans.Constants
import com.tariq.composemusicplayer.media.exoplayer.MediaPlayerNotificationManager
import com.tariq.composemusicplayer.media.exoplayer.MediaSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: CacheDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var mediaSource: MediaSource

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var notificationManager: MediaPlayerNotificationManager
    private var currentPlayingMedia: MediaMetadataCompat? = null
    private var isPlayerInitialized: Boolean = false
    var isForegroundService: Boolean = false

    companion object {
        private const val TAG = "MediaPlayerService"
        var currentDuration: Long = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val sessionActivityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    sessionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            }


        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(sessionActivityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
        notificationManager = MediaPlayerNotificationManager(
            this,
            mediaSession.sessionToken,
            PlayerNotificationListener()
        )
        notificationManager.showNotification(exoPlayer)
        serviceScope.launch {
            mediaSource.load()
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(MusicPlaybackPreparer())
            setQueueNavigator(QueueNavigator(mediaSession))
            setPlayer(exoPlayer)
        }

    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(Constants.MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            Constants.MEDIA_ROOT_ID -> {
                val resultSent = mediaSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(mediaSource.asMediaItem())
                    } else {
                        result.sendResult(null)
                    }
                }
                Log.i("CLick", "onLoadChildren: $resultSent")
                if (!resultSent) {
                    result.detach()
                }

            }
            else -> Unit
        }
    }

    override fun onCustomAction(
        action: String,
        extras: Bundle?,
        result: Result<Bundle>
    ) {
        super.onCustomAction(action, extras, result)

        when (action) {
            Constants.START_MEDIA_PLAY_ACTION -> {
                notificationManager.showNotification(exoPlayer)
                Log.i("CLick", "onCustomAction: $action -- ${exoPlayer.isPlaying}")
            }
            Constants.REFRESH_MEDIA_PLAY_ACTION -> {
                mediaSource.refresh()
                notifyChildrenChanged(Constants.MEDIA_ROOT_ID)
            }
            else -> Unit
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.release()
    }

    inner class QueueNavigator(
        mediaSessionCompat: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(
            player: Player,
            windowIndex: Int
        ): MediaDescriptionCompat {
            if (windowIndex < mediaSource.audioMediaMetadata.size) {
                return mediaSource.audioMediaMetadata[windowIndex].description
            }
            return MediaDescriptionCompat.Builder().build()
        }

    }

    inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            Log.i("CLick", "onNotificationPosted: ${exoPlayer.isPlaying}")
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(
                        applicationContext,
                        this@MediaPlayerService.javaClass
                    )
                )
                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

    }

    inner class MusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {
        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            return false
        }

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        override fun onPrepare(playWhenReady: Boolean) = Unit

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            Log.i("CLick", "onPrepareFromMediaId: $mediaId, -- ${mediaSource}")
            mediaSource.whenReady {
                Log.i("CLick", "onPrepareFromMediaId: $it")
                val itemToPlay = mediaSource.audioMediaMetadata.find {
                    it.description.mediaId == mediaId
                }

                currentPlayingMedia = itemToPlay

                createPlayer(
                    mediaSource.audioMediaMetadata,
                    itemToPlay,
                    playWhenReady
                )
            }


        }

        override fun onPrepareFromSearch(
            query: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) = Unit

        override fun onPrepareFromUri(
            uri: Uri,
            playWhenReady: Boolean,
            extras: Bundle?
        ) = Unit

        private fun createPlayer(
            mediaMetadata: List<MediaMetadataCompat>,
            itemToPlay: MediaMetadataCompat?,
            playWhenReady: Boolean
        ) {
            Log.i("CLick", "createPlayer: $playWhenReady --- ${exoPlayer}")
            val indexToPlay =
                if (currentPlayingMedia == null) 0 else mediaMetadata.indexOf(itemToPlay)

            exoPlayer.addListener(ExoPlayerEventListener())
            exoPlayer.setMediaSource(mediaSource.asMediaSource(dataSourceFactory))
            exoPlayer.prepare()
            exoPlayer.seekTo(indexToPlay, 0)
            exoPlayer.playWhenReady = playWhenReady

        }

    }

    inner class ExoPlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.i("CLick", "onPlaybackStateChanged: $playbackState")
            when (playbackState) {
                Player.STATE_BUFFERING, Player.STATE_READY -> {
                    notificationManager.showNotification(exoPlayer)
                }
                else -> {
                    notificationManager.hideNotification()
                }

            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            currentDuration = player.duration
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.i("CLick", "onPlayerError: $error")
        }
    }
}