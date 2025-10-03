package com.example.music

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DeezerApiHelper {
    fun refreshPreview(song: Song, callback: (Song?) -> Unit) {
        RetrofitClient.api.getTrack(song.id).enqueue(object : Callback<Track> {
            override fun onResponse(call: Call<Track>, response: Response<Track>) {
                if (response.isSuccessful) {
                    val track = response.body()
                    if (track != null) {
                        song.url = track.preview
                        song.lastFetchTime = System.currentTimeMillis()
                        callback(song)
                    } else {
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<Track>, t: Throwable) {
                callback(null)
            }
        })
    }
}
