package com.example.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class MusicService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private lateinit var exoPlayer: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private val CHANNEL_ID = "music_channel_id"
    private val NOTIFICATION_ID = 1
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentCover: String = ""
    private var coverXL: String = ""
    private lateinit var mediaSession: MediaSessionCompat

    companion object { //static data in kotlin
        fun play(        // Start playing a specific song
            url: String,
            context: Context,
            title: String = "",
            artist: String = "",
            cover: String = "",
            coverXL: String = ""
        ) {     //add data to intent
            val intent = Intent(context, MusicService::class.java).apply {
                action = "PLAY_URL"
                putExtra("URL", url)
                putExtra("TITLE", title)
                putExtra("ARTIST", artist)
                putExtra("COVER", cover)
                putExtra("COVER_XL", coverXL)
            }
            context.startForegroundService(intent)
        }

        // Play the next song in the queue
        fun next(context: Context) {
            context.startForegroundService(Intent(context, MusicService::class.java).apply { action = "NEXT" })
        }
    }

    private val updateRunnable = object : Runnable { //create Runnable data
        override fun run() {
            if (::exoPlayer.isInitialized) {
                sendProgressBroadcast()
            }
            handler.postDelayed(this, 1000)
        }
    }

    // Initialize the service, player, and media session
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() //Notification is obligated for using Service
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
            setCallback(mediaSessionCallback)
        }
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) handleSongEnded()
                updateNotification()
                updatePlaybackState()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
                updatePlaybackState()
            }
        })
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(updateRunnable)
    }

    // Handle incoming service commands (play, pause, seek, etc.)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                "TOGGLE_PLAY" -> {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    updateNotification()
                    updatePlaybackState()
                    sendProgressBroadcast()
                }

                "SEEK_TO" -> {
                    val position = intent.getLongExtra("SEEK_TO", 0L)
                    exoPlayer.seekTo(position)
                    updatePlaybackState()
                    sendProgressBroadcast()
                }

                "TOGGLE_REPEAT" -> {
                    exoPlayer.repeatMode =
                        if (exoPlayer.repeatMode == ExoPlayer.REPEAT_MODE_ONE)
                            ExoPlayer.REPEAT_MODE_OFF
                        else
                            ExoPlayer.REPEAT_MODE_ONE
                    sendProgressBroadcast()
                }

                "PLAY_URL" -> {
                    val url = intent.getStringExtra("URL") ?: return START_NOT_STICKY
                    currentTitle = intent.getStringExtra("TITLE") ?: ""
                    currentArtist = intent.getStringExtra("ARTIST") ?: ""
                    currentCover = intent.getStringExtra("COVER") ?: ""
                    coverXL = intent.getStringExtra("COVER_XL") ?: ""
                    exoPlayer.setMediaItem(MediaItem.fromUri(url.toUri()))
                    exoPlayer.prepare()
                    exoPlayer.play()

                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            !handler.hasCallbacks(updateRunnable)
                        } else {
                            true
                        }
                    ) {
                        handler.post(updateRunnable)
                    }

                    updateNotification()
                    updatePlaybackState()
                    sendProgressBroadcast()
                }

                "NEXT" -> {
                    val next = MusicQueueManager.playNext()
                    next?.let { playSong(it) }
                }

                "PREVIOUS" -> {
                    val prev = MusicQueueManager.playPrevious()
                    prev?.let { playSong(it) }
                }

                "CLEAR_QUEUE" -> {
                    MusicQueueManager.removeQueue {
                        exoPlayer.stop()
                        sendBroadcast(Intent("QUEUE_CLEARED"))
                        updateNotification()
                        updatePlaybackState()
                    }
                }

                "STOP" -> stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    // Release all resources when the service is destroyed
    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        if (::exoPlayer.isInitialized) exoPlayer.release()
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        super.onDestroy()
    }

    // Stop service when removed from recent apps
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    // Handle media control actions from the system notification
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            exoPlayer.play()
            updateNotification()
            updatePlaybackState()
            sendProgressBroadcast()
        }

        override fun onPause() {
            exoPlayer.pause()
            updateNotification()
            updatePlaybackState()
            sendProgressBroadcast()
        }

        override fun onSkipToNext() {
            val next = MusicQueueManager.playNext()
            next?.let { playSong(it) }
        }

        override fun onSkipToPrevious() {
            val prev = MusicQueueManager.playPrevious()
            prev?.let { playSong(it) }
        }

        override fun onSeekTo(pos: Long) {
            updatePlaybackState()
            sendProgressBroadcast()
            exoPlayer.seekTo(pos)
        }
    }

    // Handle end of the current song and play the next one
    private fun handleSongEnded() {
        val next = MusicQueueManager.playNext()
        if (next != null) {
            playSong(next)
        } else {
            exoPlayer.seekTo(0)
            exoPlayer.pause()
            updateNotification()
            updatePlaybackState()
            sendProgressBroadcast()
        }
    }

    // Play a new song and update UI components
    private fun playSong(song: Song) {
        MusicQueueManager.getPlayableSong(song) { refreshed ->
            refreshed?.let {
                MusicQueueManager.setCurrentSong(it)
                currentTitle = it.title
                currentArtist = it.artist
                currentCover = it.cover
                coverXL = it.coverXL
                exoPlayer.setMediaItem(MediaItem.fromUri(it.url.toUri()))
                exoPlayer.prepare()
                exoPlayer.play()
                updateNotification()
                updatePlaybackState()
                sendProgressBroadcast()
            }
        }
    }

    // Send playback progress to UI components
    private fun sendProgressBroadcast() {
        if (::exoPlayer.isInitialized) {
            val duration = if (exoPlayer.duration == C.TIME_UNSET) 0L else exoPlayer.duration
            val position = exoPlayer.currentPosition
            val isPlaying = exoPlayer.isPlaying
            val isRepeating = exoPlayer.repeatMode == ExoPlayer.REPEAT_MODE_ONE
            val intent = Intent("MUSIC_PROGRESS_UPDATE").apply {
                putExtra("position", position)
                putExtra("duration", duration)
                putExtra("isPlaying", isPlaying)
                putExtra("isRepeating", isRepeating)
                putExtra("title", currentTitle)
                putExtra("artist", currentArtist)
                putExtra("cover", currentCover)
                putExtra("cover_xl", coverXL)
            }
            sendBroadcast(intent)
        }
    }

    // Create a notification channel for the service
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for music playback"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    // Build PendingIntent for notification actions
    private fun getActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Build the main media notification
    private fun buildNotification(): Notification {
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle.ifEmpty { "Did you really choose a song?" })
            .setContentText(currentArtist.ifEmpty { "Does it hard?" })
            .setSmallIcon(R.drawable.music_note_24px)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.skip_previous_24px,
                    "Previous",
                    getActionIntent("PREVIOUS")
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (exoPlayer.isPlaying) R.drawable.pause_24px else R.drawable.play,
                    if (exoPlayer.isPlaying) "Pause" else "Play",
                    getActionIntent("TOGGLE_PLAY")
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.skip_next_24px,
                    "Next",
                    getActionIntent("NEXT")
                )
            )
            .build()
    }

    // Update existing media notification
    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    // Update media session playback state and metadata
    private fun updatePlaybackState() {
        if (!::exoPlayer.isInitialized) return
        val position = exoPlayer.currentPosition
        val duration = if (exoPlayer.duration == C.TIME_UNSET) 0L else exoPlayer.duration
        val playbackSpeed = if (exoPlayer.isPlaying) 1f else 0f
        val state = if (exoPlayer.isPlaying)
            PlaybackStateCompat.STATE_PLAYING
        else
            PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, playbackSpeed)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setBufferedPosition(exoPlayer.bufferedPosition)
            .build()

        mediaSession.setPlaybackState(playbackState)
//MediaMetadataCompat = backward compatible
        val metadataBuilder = android.support.v4.media.MediaMetadataCompat.Builder()
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, duration)

        if (coverXL.isNotBlank()) {
            Glide.with(this)
                .asBitmap()
                .load(coverXL)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        metadataBuilder.putBitmap(
                            android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                            resource
                        )
                        mediaSession.setMetadata(metadataBuilder.build())
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } else {
            mediaSession.setMetadata(metadataBuilder.build())
        }
    }
}