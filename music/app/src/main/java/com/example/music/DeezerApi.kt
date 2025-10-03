package com.example.music

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Path

interface DeezerApi {
    @Headers(
        "x-rapidapi-key: 7d312199aamsh3cecd298aa3d7a5p101f9ejsnadc976a2d3aa",
        "X-RapidAPI-Host: deezerdevs-deezer.p.rapidapi.com"
    )
    @GET("track/{id}")
    fun getTrack(@Path("id") id: Long): Call<Track>

    @Headers(
        "x-rapidapi-key: 7d312199aamsh3cecd298aa3d7a5p101f9ejsnadc976a2d3aa",
        "X-RapidAPI-Host: deezerdevs-deezer.p.rapidapi.com"
    )
    @GET("search")
    fun searchTrack(@Query("q") query: String): Call<DeezerResponse>
}
