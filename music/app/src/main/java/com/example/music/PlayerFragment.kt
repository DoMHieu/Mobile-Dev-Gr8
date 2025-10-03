package com.example.music

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.SeekBar
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
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit
import androidx.core.view.isGone

class PlayerFragment : Fragment() {

    private lateinit var slider: SeekBar
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
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val position = intent?.getLongExtra("position", 0L) ?: 0L
            val duration = intent?.getLongExtra("duration", 0L) ?: 0L
            val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
            val isRepeating = intent?.getBooleanExtra("isRepeating", false) ?: false
            val title = intent?.getStringExtra("title") ?: ""
            val artist = intent?.getStringExtra("artist") ?: ""
            val coverUrl = intent?.getStringExtra("cover") ?: ""
            val coverUrlXL = intent?.getStringExtra("cover_xl") ?: ""

            queueAdapter.notifyDataSetChanged()

            // Toolbar
            val toolbar = view?.findViewById<MaterialToolbar>(R.id.Toolbar)
            toolbar?.title = title
            toolbar?.subtitle = artist

            // Ảnh bìa
            if (coverUrlXL.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(coverUrlXL)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.image_24px)
                    .error(R.drawable.image_24px)
                    .into(coverImage)
            } else {
                coverImage.setImageResource(R.drawable.image_24px)
            }

            // Slider
            if (!isUserSeeking && duration > 0) {
                if (slider.max != duration.toInt()) {
                    slider.max = duration.toInt()
                }
                slider.progress = position.toInt().coerceAtMost(duration.toInt())
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
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let {
                    sendMusicCommand("SEEK_TO", it.progress.toLong())
                }
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    textCurrentTime.text = formatTime(progress.toLong())
                }
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
                    cover = it.cover,
                    coverXL = it.coverXL
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
                    cover = it.cover,
                    coverXL = it.coverXL,
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
                cover = song.cover,
                coverXL = song.coverXL
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
                    val position = viewHolder.bindingAdapterPosition
                    val song = MusicQueueManager.getQueue()[position]
                    val isCurrent = (song == MusicQueueManager.getCurrent())
                    MusicQueueManager.remove(song)
                    queueAdapter.notifyItemRemoved(position)

                    if (isCurrent) {//change song if delete currentsong
                        val next = MusicQueueManager.getCurrent()
                        if (next != null) {
                            MusicService.play(
                                next.url,
                                requireContext(),
                                title = next.title,
                                artist = next.artist,
                                cover = next.cover,
                                coverXL = next.coverXL,
                            )
                        } else { //else, continue to run, this code only work to update queue
                            MusicService.next(requireContext())
                        }
                    }
                }

            })
        itemTouchHelper.attachToRecyclerView(rvQueue)

        //Queue Button
        val btnQueue = view.findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.playlist_play)
        btnQueue.setOnClickListener {
            if (rvQueue.isGone) {
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
