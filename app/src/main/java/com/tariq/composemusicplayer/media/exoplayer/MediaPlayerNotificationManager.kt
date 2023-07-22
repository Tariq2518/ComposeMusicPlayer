package com.tariq.composemusicplayer.media.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.media.session.MediaController
import android.service.notification.NotificationListenerService
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.tariq.composemusicplayer.R
import com.tariq.composemusicplayer.media.constans.Constants

internal class MediaPlayerNotificationManager(
    context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) {
    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        val builder = PlayerNotificationManager.Builder(
            context,
            Constants.PLAYBACK_NOTIFICATION_ID,
            Constants.PLAYBACK_NOTIFICATION_CHANNEL_ID
        )

        with(builder) {
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setNotificationListener(notificationListener)
            setChannelNameResourceId(R.string.app_name)
            setChannelDescriptionResourceId(R.string.app_name)

        }
        notificationManager = builder.build()

        with(notificationManager) {
            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.music_icon)
            setUseRewindAction(false)
            setUseFastForwardAction(false)
        }
    }

    fun hideNotification(){
        notificationManager.setPlayer(null)
    }

    fun showNotification(player: Player){
        Log.i("CLick", "showNotification: ${player}")
        notificationManager.setPlayer(player)
    }

    inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence =
            controller.metadata.description.title.toString()

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence? =
            controller.metadata.description.subtitle

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            return null
        }

    }
}