package com.example.music

data class Song(
    val id: Long,              // track id từ Deezer
    val title: String,
    var url: String,           // preview link (có thể thay đổi khi refresh)
    val artist: String,
    val cover: String,
    val coverXL: String,
    var lastFetchTime: Long = 0
)


