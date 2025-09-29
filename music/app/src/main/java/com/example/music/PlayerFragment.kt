package com.example.music

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import java.util.concurrent.TimeUnit

class PlayerFragment : Fragment() {

    private lateinit var slider: Slider
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var playPauseButton: FloatingActionButton
    private lateinit var repeatButton: ImageView
    private lateinit var coverImage: ImageView
    private lateinit var rvQueue: RecyclerView
    private lateinit var queueAdapter: SongAdapter
    private var isUserSeeking = false

    // Nhận broadcast từ MusicService
    private val musicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val position = intent?.getLongExtra("position", 0L) ?: 0L
            val duration = intent?.getLongExtra("duration", 0L) ?: 0L
            val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
            val isRepeating = intent?.getBooleanExtra("isRepeating", false) ?: false
            val title = intent?.getStringExtra("title") ?: ""
            val artist = intent?.getStringExtra("artist") ?: ""
            val coverUrl = intent?.getStringExtra("cover") ?: ""
            queueAdapter.notifyDataSetChanged()

            // Toolbar
            val toolbar = view?.findViewById<MaterialToolbar>(R.id.Toolbar)
            toolbar?.title = title
            toolbar?.subtitle = artist

            // Ảnh bìa
            if (!coverUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.image_24px)
                    .error(R.drawable.image_24px)
                    .into(coverImage)
            } else {
                coverImage.setImageResource(R.drawable.image_24px)
            }

            // Slider
            if (!isUserSeeking && duration > 0) {
                if (slider.valueTo != duration.toFloat()) {
                    slider.valueTo = duration.toFloat()
                }
                slider.value = position.toFloat().coerceAtMost(duration.toFloat())
            }

            // Thời gian
            textCurrentTime.text = formatTime(position)
            textTotalTime.text = if (duration > 0) formatTime(duration) else "--:--"

            // Nút play/pause
            playPauseButton.setImageResource(
                if (isPlaying) R.drawable.pause_24px else R.drawable.play
            )
            playPauseButton.tag = if (isPlaying) "pause" else "play"

            // Nút repeat
            repeatButton.setImageResource(
                if (isRepeating) R.drawable.repeat_one_24px else R.drawable.repeat
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_player, container, false)

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slider = view.findViewById(R.id.progressSlider)
        textCurrentTime = view.findViewById(R.id.songCurrentProgress)
        textTotalTime = view.findViewById(R.id.songTotalTime)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        repeatButton = view.findViewById(R.id.repeatButton)
        coverImage = view.findViewById(R.id.imageView)
        rvQueue = view.findViewById(R.id.rvQueue)

        // Nút play/pause và repeat
        playPauseButton.setOnClickListener { sendMusicCommand("TOGGLE_PLAY") }
        repeatButton.setOnClickListener { sendMusicCommand("TOGGLE_REPEAT") }

        // Slider
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(slider: Slider) {
                isUserSeeking = false
                sendMusicCommand("SEEK_TO", slider.value.toLong())
            }
        })

        // Nút next/previous
        val nextButton = view.findViewById<ImageView>(R.id.nextButton)
        val previousButton = view.findViewById<ImageView>(R.id.previousButton)

        nextButton.setOnClickListener {
            val next = MusicQueueManager.playNext()
            next?.let {
                MusicService.play(
                    it.url,
                    requireContext(),
                    title = it.title,
                    artist = it.artist,
                    cover = it.cover
                )
                queueAdapter.notifyDataSetChanged()
            }
        }

        previousButton.setOnClickListener {
            val prev = MusicQueueManager.playPrevious()
            prev?.let {
                MusicService.play(
                    it.url,
                    requireContext(),
                    title = it.title,
                    artist = it.artist,
                    cover = it.cover
                )
                queueAdapter.notifyDataSetChanged()
            }
        }

        // Queue
        rvQueue.layoutManager = LinearLayoutManager(requireContext())
        queueAdapter = SongAdapter(MusicQueueManager.getQueue()) { song ->
            MusicQueueManager.setCurrentSong(song)
            MusicService.play(
                song.url,
                requireContext(),
                title = song.title,
                artist = song.artist,
                cover = song.cover
            )
            queueAdapter.notifyDataSetChanged()
        }
        rvQueue.adapter = queueAdapter
        queueAdapter.notifyDataSetChanged()

        // Swipe to delete
        val itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val song = MusicQueueManager.getQueue()[position]
                    MusicQueueManager.remove(song)
                    queueAdapter.notifyItemRemoved(position)
                }
            })
        itemTouchHelper.attachToRecyclerView(rvQueue)

        // Nút toggle queue
        val btnQueue = view.findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.playlist_play)
        btnQueue.setOnClickListener {
            if (rvQueue.visibility == View.GONE) {
                rvQueue.visibility = View.VISIBLE
                coverImage.visibility = View.GONE
                queueAdapter.notifyDataSetChanged()
            } else {
                rvQueue.visibility = View.GONE
                coverImage.visibility = View.VISIBLE
            }
        }
    }

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

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(musicReceiver)
    }

    private fun sendMusicCommand(action: String, seekTo: Long? = null) {
        val intent = Intent(requireContext(), MusicService::class.java).apply {
            this.action = action
            seekTo?.let { putExtra("SEEK_TO", it) }
        }
        requireContext().startForegroundService(intent)
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
