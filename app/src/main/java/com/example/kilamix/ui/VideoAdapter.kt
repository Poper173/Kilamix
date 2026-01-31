package com.itech.kilamix.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itech.kilamix.R
import com.itech.kilamix.model.Video

class VideoAdapter(
    private val videos: List<Video>,
    private val onLike: (Video) -> Unit,
    private val onShare: (Video) -> Unit,
    private val onDownload: (Video) -> Unit,
    private val onPlay: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val title: TextView = view.findViewById(R.id.txtTitle)
        val creator: TextView = view.findViewById(R.id.txtCreator)
        val btnLike: Button = view.findViewById(R.id.btnLike)
        val btnShare: Button = view.findViewById(R.id.btnShare)
        val btnDownload: Button = view.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]

        holder.title.text = video.title
        holder.creator.text = video.user.name

        Glide.with(holder.itemView.context)
            .load(video.thumbnail_url?.replace("localhost", "10.0.2.2"))
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            onPlay(video)
        }

        holder.btnLike.setOnClickListener { onLike(video) }
        holder.btnShare.setOnClickListener { onShare(video) }
        holder.btnDownload.setOnClickListener { onDownload(video) }
    }

    override fun getItemCount(): Int = videos.size
}
