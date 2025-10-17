package com.example.music

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DeezerApiHelper {
    fun refreshPreview(song: Song, callback: (Song?) -> Unit) { //refresh 30s preview url (10 mins expired)
        RetrofitClient.api.getTrack(song.id).enqueue(object : Callback<Track> {
            override fun onResponse(call: Call<Track>, response: Response<Track>) {
                if (response.isSuccessful) { //Check if response is possible
                    val track = response.body() //get real data response from server
                    if (track != null) {
                        song.url = track.preview
                        song.lastFetchTime = System.currentTimeMillis() //last time get fetch data (preview url) = system time
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
