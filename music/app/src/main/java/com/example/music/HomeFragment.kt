package com.example.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val song8 = view.findViewById<ImageView>(R.id.song_8)

        val songs = listOf(
            R.id.song_1 to R.raw.song1,
            R.id.song_2 to R.raw.song2,
            R.id.song_3 to R.raw.song3,
            R.id.song_4 to R.raw.song4,
            R.id.song_5 to R.raw.song5,
            R.id.song_6 to R.raw.song6,
            R.id.song_7 to R.raw.song7,
            R.id.song_8 to R.raw.song8,
            R.id.song_9 to R.raw.song9,
            R.id.song_10 to R.raw.song10
        )
        return view
    }
}
