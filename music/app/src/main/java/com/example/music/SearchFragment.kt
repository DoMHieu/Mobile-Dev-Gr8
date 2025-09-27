package com.example.music

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val songs = mutableListOf<Song>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchInput = view.findViewById(R.id.searchInput)
        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = SongAdapter(songs) { song ->
            playSong(song)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        //Require enter to search
        searchInput.setOnEditorActionListener { _, _, _ ->
            val keyword = searchInput.text.toString()
            if (keyword.isNotEmpty()) {
                searchSongs(keyword)
            }
            true
        }
    }
    private fun searchSongs(keyword: String) {
        RetrofitClient.api.searchTrack(keyword).enqueue(object : Callback<DeezerResponse> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(
                call: Call<DeezerResponse>,
                response: Response<DeezerResponse>
            ) {
                if (response.isSuccessful) {
                    val tracks = response.body()?.data ?: emptyList()
                    songs.clear()
                    songs.addAll(tracks.map {
                        Song(
                            title = it.title,
                            url = it.preview,
                            artist = it.artist.name,
                            cover = it.album.cover
                        )
                    })
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<DeezerResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
    private fun playSong(song: Song) {
        val intent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra("ACTION", "PLAY_URL")
            putExtra("URL", song.url)
            putExtra("TITLE", song.title)
            putExtra("ARTIST", song.artist)
            putExtra("COVER", song.cover)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

}
