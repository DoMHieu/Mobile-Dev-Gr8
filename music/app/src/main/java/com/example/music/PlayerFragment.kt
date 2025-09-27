package com.example.music

//Imports necessary package like android, exoplayer, retrofit,...
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import java.util.concurrent.TimeUnit

// ==== Main Class ====
class PlayerFragment : Fragment() {

    // ---------------- UI Components ----------------
    private lateinit var slider: Slider
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var playPauseButton: FloatingActionButton
    private lateinit var repeatButton: ImageView

    // ---------------- State Flags ----------------
    private var isUserSeeking = false

    // ---------------- Broadcast Receiver ----------------
    // Receives updates from MusicService about playback state and progress
    private val musicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val position = intent?.getLongExtra("position", 0L) ?: 0L
            val duration = intent?.getLongExtra("duration", 0L) ?: 0L
            val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
            val isRepeating = intent?.getBooleanExtra("isRepeating", false) ?: false
            val title = intent?.getStringExtra("title") ?: ""
            val artist = intent?.getStringExtra("artist") ?: ""
            val coverUrl = intent?.getStringExtra("cover") ?: ""

            // Update toolbar with song title and artist
            val toolbar = view?.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.Toolbar)
            toolbar?.title = title
            toolbar?.subtitle = artist

            // Load cover image with Glide
            val coverImage = view?.findViewById<ImageView>(R.id.imageView)
            Glide.with(requireContext())
                .load(coverUrl)
                .placeholder(R.drawable.image_24px)
                .into(coverImage!!)

            // Update slider if user is not dragging
            if (!isUserSeeking && duration > 0) {
                if (slider.valueTo != duration.toFloat()) {
                    slider.valueTo = duration.toFloat()
                }
                slider.value = position.toFloat().coerceAtMost(duration.toFloat())
            }

            // Update text times
            textCurrentTime.text = formatTime(position)
            textTotalTime.text = if (duration > 0) formatTime(duration) else "--:--"

            // Update play/pause button icon and tag
            playPauseButton.setImageResource(
                if (isPlaying) R.drawable.pause_24px else R.drawable.play
            )
            playPauseButton.tag = if (isPlaying) "pause" else "play"

            // Update repeat button icon
            repeatButton.setImageResource(
                if (isRepeating) R.drawable.repeat_one_24px else R.drawable.repeat
            )
        }
    }

    // ---------------- Lifecycle ----------------
    // Inflate fragment layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_player, container, false)

    // Initialize UI components and set event listeners
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init views
        slider = view.findViewById(R.id.progressSlider)
        textCurrentTime = view.findViewById(R.id.songCurrentProgress)
        textTotalTime = view.findViewById(R.id.songTotalTime)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        repeatButton = view.findViewById(R.id.repeatButton)

        // Handle play/pause and repeat toggle clicks
        playPauseButton.setOnClickListener { sendMusicCommand("TOGGLE_PLAY") }
        repeatButton.setOnClickListener { sendMusicCommand("TOGGLE_REPEAT") }

        // Handle user dragging the slider
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isUserSeeking = false
                sendMusicCommand("SEEK_TO", slider.value.toLong())
            }
        })
    }

    // Register BroadcastReceiver to receive updates from MusicService
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("MUSIC_PROGRESS_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(musicReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                requireContext(),
                musicReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    // Unregister BroadcastReceiver when fragment stops
    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(musicReceiver)
    }

    // ---------------- Helper Functions ----------------
    // Sends a command (play/pause, seek, repeat toggle) to MusicService
    private fun sendMusicCommand(action: String, seekTo: Long? = null) {
        val intent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra("ACTION", action)
            seekTo?.let { putExtra("SEEK_TO", it) }
        }
        requireContext().startForegroundService(intent)
    }

    // Convert milliseconds into "mm:ss" format
    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
