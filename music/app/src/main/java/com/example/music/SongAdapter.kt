package com.example.music

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(
    private val items: List<Song>,
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.songTitle)
        val artist: TextView = itemView.findViewById(R.id.songArtist)
        val cover: ImageView = itemView.findViewById(R.id.songCover)       // ảnh bìa
        val playingIcon: ImageView = itemView.findViewById(R.id.playingIcon) // icon đang phát
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = items[position]

        // Gán dữ liệu
        holder.title.text = song.title
        holder.artist.text = song.artist
        Glide.with(holder.itemView.context)
            .load(song.cover)
            .placeholder(R.drawable.image_24px)
            .into(holder.cover)

        // Highlight nếu là bài hát đang phát
        val current = MusicQueueManager.getCurrent()
        if (current != null && current.url == song.url) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.black)
            )
            holder.playingIcon.visibility = View.VISIBLE
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.playingIcon.visibility = View.GONE
        }

        // Xử lý click
        holder.itemView.setOnClickListener { onClick(song) }
    }

    override fun getItemCount(): Int = items.size
}
