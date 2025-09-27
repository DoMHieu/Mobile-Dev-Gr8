package com.example.music

data class DeezerResponse(
    val data: List<Track>
)
data class Album(
    val cover: String
)
data class Artist(
    val name: String
)

data class Track(
    val title: String,
    val preview: String,
    val artist: Artist,
    val album: Album
)


