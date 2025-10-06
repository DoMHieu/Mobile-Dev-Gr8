package com.example.music

data class Song(
    val id: Long,
    val title: String,
    var url: String,
    val artist: String,
    val cover: String,
    val coverXL: String,
    var lastFetchTime: Long = 0
)


