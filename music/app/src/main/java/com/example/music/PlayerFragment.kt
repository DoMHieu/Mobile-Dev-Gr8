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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
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
import java.util.concurrent.TimeUnit
import androidx.core.view.isGone
import android.widget.Toast
import com.bumptech.glide.request.RequestOptions

class PlayerFragment : Fragment() {
    private lateinit var slider: SeekBar
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var playPauseButton: ImageView
    private lateinit var repeatButton: ImageView
    private lateinit var coverImage: ImageView
    private lateinit var rvQueue: RecyclerView
    private lateinit var queueAdapter: SongAdapter
    private var isUserSeeking = false

    //Receiver message from Broadcast (Intent)
    private val musicReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            //Get Song data information
            val title = intent?.getStringExtra("title") ?: ""
            val artist = intent?.getStringExtra("artist") ?: ""
            queueAdapter.notifyDataSetChanged()
            val toolbar = view?.findViewById<MaterialToolbar>(R.id.Toolbar)   // Toolbar editor
            toolbar?.title = title
            toolbar?.subtitle = artist
            //Music CoverXL Glide
            val coverUrlXL = intent?.getStringExtra("cover_xl") ?: ""
            if (coverUrlXL.isNotBlank()) {
                Glide.with(requireContext())
                    .load(coverUrlXL)
                    .apply(
                        RequestOptions.bitmapTransform(
                            MultiTransformation(CenterCrop(), RoundedCorners(24))
                        )
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .placeholder(R.drawable.image_24px)
                            .error(R.drawable.image_24px)
                    )
                    .into(coverImage)
            } else {
                coverImage.setImageResource(R.drawable.image_24px)
            }

            // slider, have been replaced with seekbar, still keep using slider as variable
            val position = intent?.getLongExtra("position", 0L) ?: 0L
            val duration = intent?.getLongExtra("duration", 0L) ?: 0L
            if (!isUserSeeking && duration > 0) {
                if (slider.max != duration.toInt()) {
                    slider.max = duration.toInt()
                }
                slider.progress = position.toInt().coerceAtMost(duration.toInt())
            }
            textCurrentTime.text = formatTime(position)
            textTotalTime.text = if (duration > 0) formatTime(duration) else "--:--"

            //Play, Loop button
            val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
            playPauseButton.setImageResource(
                if (isPlaying) R.drawable.pause_24px else R.drawable.play
            )
            playPauseButton.tag = if (isPlaying) "pause" else "play"
            val isRepeating = intent?.getBooleanExtra("isRepeating", false) ?: false
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
        playPauseButton.setOnClickListener { sendMusicCommand("TOGGLE_PLAY") } //send message to intent
        repeatButton.setOnClickListener { sendMusicCommand("TOGGLE_REPEAT") }

        // SeekeBar change listener
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

        //Next, Previous
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
            MusicQueueManager.getPlayableSong(song) { playable ->
                if (playable != null) {
                    MusicQueueManager.setCurrentSong(playable)
                    MusicService.play(
                        playable.url,
                        requireContext(),
                        title = playable.title,
                        artist = playable.artist,
                        cover = playable.cover,
                        coverXL = playable.coverXL
                    )
                    queueAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Invalid song, try delete from queue and retry!", Toast.LENGTH_SHORT).show()
                }
            }
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
        btnQueue.setOnLongClickListener {
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                action = "CLEAR_QUEUE"
            }
            requireContext().startForegroundService(intent)
            Toast.makeText(requireContext(), "Queue deleted", Toast.LENGTH_SHORT).show()
            true
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
