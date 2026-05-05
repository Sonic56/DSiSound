package com.dsi.sound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var selectedIndex = -1

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.songTitle)
        val artist: TextView = itemView.findViewById(R.id.songArtist)
        val duration: TextView = itemView.findViewById(R.id.songDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = if (song.artist.isNotEmpty() && song.artist != "<unknown>")
            song.artist else "Unknown Artist"
        holder.duration.text = song.durationFormatted
        holder.itemView.isSelected = position == selectedIndex
        holder.itemView.setOnClickListener {
            val prev = selectedIndex
            selectedIndex = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedIndex)
            onSongClick(song, selectedIndex)
        }
    }

    override fun getItemCount() = songs.size

    fun setSelected(index: Int) {
        val prev = selectedIndex
        selectedIndex = index
        notifyItemChanged(prev)
        notifyItemChanged(selectedIndex)
    }
}
