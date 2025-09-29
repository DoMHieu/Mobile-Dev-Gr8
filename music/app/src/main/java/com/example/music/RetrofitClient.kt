package com.example.music

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//RetrofitClient for SEND and GET data from API, in this case is deezer api
object RetrofitClient {
    private const val BASE_URL = "https://deezerdevs-deezer.p.rapidapi.com/"
    val api: DeezerApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApi::class.java)
    }
}
