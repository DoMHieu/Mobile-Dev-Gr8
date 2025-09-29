package com.example.music

import android.content.Context
import android.widget.Toast

object MusicQueueManager {
    private val queue = mutableListOf<Song>()
    private var currentSong: Song? = null
    private var currentIndex = -1

    fun add(song: Song, context: Context) {
        queue.add(song)
        Toast.makeText(context, "Added to the queue", Toast.LENGTH_LONG).show()
    }

    fun getQueue(): List<Song> = queue

    fun getCurrent(): Song? = currentSong

    fun setCurrentSong(song: Song) {
        currentSong = song
        currentIndex = queue.indexOf(song)
    }

    fun playNext(): Song? {
        return if (currentIndex != -1 && currentIndex + 1 < queue.size) {
            currentIndex++
            currentSong = queue[currentIndex]
            currentSong
        } else null
    }

    fun playPrevious(): Song? {
        return if (currentIndex > 0) {
            currentIndex--
            currentSong = queue[currentIndex]
            currentSong
        } else null
    }

    fun remove(song: Song) {
        queue.remove(song)
        if(song == currentSong) {
            currentSong = null
            currentIndex = -1
        }
    }
}

