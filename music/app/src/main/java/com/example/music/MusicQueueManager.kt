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
        val removedIndex = queue.indexOf(song)
        if (removedIndex == -1) return

        queue.removeAt(removedIndex)

        if (song == currentSong) {
            if (queue.isNotEmpty()) {
                // Ưu tiên bài đứng ở cùng vị trí (chính là bài "tiếp theo" sau khi remove)
                currentIndex = minOf(removedIndex, queue.size - 1)
                currentSong = queue[currentIndex]
            } else {
                currentSong = null
                currentIndex = -1
            }
        } else {
            // Nếu xoá bài đứng trước currentIndex, dịch trái index hiện tại
            if (removedIndex < currentIndex) {
                currentIndex--
            }
        }
    }
    fun getPlayableSong(song: Song, callback: (Song?) -> Unit) {
        val now = System.currentTimeMillis()
        val expired = (now - song.lastFetchTime) > 10 * 60 * 1000 // 10 phút

        if (song.url.isEmpty() || expired) {
            DeezerApiHelper.refreshPreview(song) { refreshed ->
                callback(refreshed)
            }
        } else {
            callback(song)
        }
    }


}

