package com.example.music

import android.annotation.SuppressLint
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
    private val randomKeywords = listOf("love","summer","dance","rock","deco*27","PinocchioP","acoustic","chill","happy","Hatsune Miku")
    private fun getRandomKeyword(): String {
        return randomKeywords.random()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput = view.findViewById(R.id.searchInput)
        recyclerView = view.findViewById(R.id.recyclerView)

        adapter = SongAdapter(songs) { song ->
            //Add in queue
            MusicQueueManager.add(song, requireContext())
            //Play the song if the queue empty
            if (MusicQueueManager.getQueue().size == 1) {
                MusicQueueManager.setCurrentSong(song)
                MusicService.play(
                    song.url,
                    requireContext(),
                    title = song.title,
                    artist = song.artist,
                    cover = song.cover,
                    coverXL = song.coverXL,
                )
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        val randomKey = getRandomKeyword()
        searchSongs(randomKey)

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim()
                if (keyword.isNotEmpty()) {
                    searchSongs(keyword)
                }; else {val randomKey = getRandomKeyword()
                    searchSongs(randomKey)}
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
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
                            id = it.id,
                            title = it.title,
                            url = it.preview ?: "",
                            artist = it.artist.name,
                            cover = it.album.cover ?: "",
                            coverXL = it.album.cover_xl ?: "",
                            lastFetchTime = System.currentTimeMillis()
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
}
