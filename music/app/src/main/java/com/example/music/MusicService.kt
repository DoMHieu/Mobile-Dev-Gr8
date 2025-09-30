package com.example.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private val CHANNEL_ID = "music_channel_id"
    private val NOTIFICATION_ID = 1
    private val handler = Handler(Looper.getMainLooper())

    //Current song info
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentCover: String = ""
    private var coverXL: String = ""

    companion object {
        fun play(url: String, context: Context, title: String = "", artist: String = "", cover: String = "", coverXL: String = "") {
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

        fun next(context: Context) {
            context.startService(Intent(context, MusicService::class.java).apply { action = "NEXT" })
        }
    }

    // Runnable update progress
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (::exoPlayer.isInitialized && exoPlayer.isPlaying) {
                sendProgressBroadcast()
            }
            handler.postDelayed(this, 1000) //for handler update 1 times in 1 second, no limit = most CPU use but smoothest
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    handleSongEnded()
                }
            }
        })

        startForeground(NOTIFICATION_ID, buildNotification("Waiting to play..."))
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOGGLE_PLAY" -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    updateNotification("Paused")
                } else {
                    exoPlayer.play()
                    updateNotification("Playing")
                }
                sendProgressBroadcast()
            }

            "SEEK_TO" -> {
                val position = intent.getLongExtra("SEEK_TO", 0L)
                exoPlayer.seekTo(position)
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
                updateNotification("Playing: $currentTitle")
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
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    // --- Helper methods ---
    private fun handleSongEnded() {
        val next = MusicQueueManager.playNext()
        if (next != null) {
            playSong(next)
        } else {
            exoPlayer.seekTo(0)
            exoPlayer.pause()
            updateNotification("Queue ended")
            sendProgressBroadcast()
        }
    }

    private fun playSong(song: Song) {
        MusicQueueManager.setCurrentSong(song)
        currentTitle = song.title
        currentArtist = song.artist
        currentCover = song.cover
        coverXL = song.coverXL
        exoPlayer.setMediaItem(MediaItem.fromUri(song.url.toUri()))
        exoPlayer.prepare()
        exoPlayer.play()
        updateNotification("Playing: ${song.title}")
        sendProgressBroadcast()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for music playback"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.music_note_24px)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

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
}
