package com.example.music

object MusicQueueManager {
    private val queue = mutableListOf<Song>()
    private var currentSong: Song? = null
    private var currentIndex = -1

    fun add(song: Song) {
        queue.add(song)
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
}

