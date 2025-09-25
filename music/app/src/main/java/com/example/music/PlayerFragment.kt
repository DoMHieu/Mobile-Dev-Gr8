package com.example.music

import android.media.MediaPlayer
import android.app.Service
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import java.util.concurrent.TimeUnit

class PlayerFragment : Fragment() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var slider: Slider
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var playPauseButton: ImageView
    private lateinit var repeatButton: ImageView

    private val handler = Handler(Looper.getMainLooper())

    private val updateSlider: Runnable = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                val pos = mediaPlayer.currentPosition.toFloat()
                slider.value = pos.coerceIn(slider.valueFrom, slider.valueTo)
                textCurrentTime.text = formatTime(mediaPlayer.currentPosition)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(  //To create view
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { //for function
        super.onViewCreated(view, savedInstanceState)

        slider = view.findViewById(R.id.progressSlider)
        textCurrentTime = view.findViewById(R.id.songCurrentProgress)
        textTotalTime = view.findViewById(R.id.songTotalTime)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        repeatButton = view.findViewById(R.id.repeatButton)
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.monitoring)
        slider.valueFrom = 0f //f: float, because slider using float type instead of milisecond
        slider.valueTo = mediaPlayer.duration.toFloat()
        slider.stepSize = 1f
        textTotalTime.text = formatTime(mediaPlayer.duration)

        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.monitoring)
            slider.value = 0f
            slider.valueTo = mediaPlayer.duration.toFloat()
            textCurrentTime.text = "0:00"
            textTotalTime.text = formatTime(mediaPlayer.duration)
            playPauseButton.setImageResource(R.drawable.play)
            handler.removeCallbacks(updateSlider)
        }


        playPauseButton.setOnClickListener {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                handler.post(updateSlider)
                playPauseButton.setImageResource(R.drawable.pause_24px)
            } else {
                mediaPlayer.pause()
                playPauseButton.setImageResource(R.drawable.play)
            }
        }

        repeatButton.setOnClickListener {
            if(!mediaPlayer.isLooping) {
                mediaPlayer.isLooping = true
                repeatButton.setImageResource(R.drawable.repeat_one_24px)
            } else {
                mediaPlayer.isLooping = false
                repeatButton.setImageResource(R.drawable.repeat)
            }
        }

        slider.addOnChangeListener { _, value, fromUser -> //to control the slider
            if (fromUser) {
                mediaPlayer.seekTo(value.toInt())
                textCurrentTime.text = formatTime(value.toInt())
            }
        }


    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSlider)
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
