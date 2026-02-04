package com.itech.kilamix.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itech.kilamix.R
import com.itech.kilamix.model.Video

class VideoAdapter(
    private var videos: List<Video>,
    private val onLike: (Video) -> Unit,
    private val onShare: (Video) -> Unit,
    private val onPlay: (Video) -> Unit,
    private val onDownload: (Video) -> Unit,
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val title: TextView = view.findViewById(R.id.txtTitle)
        val creator: TextView = view.findViewById(R.id.txtCreator)
        val viewsCount: TextView = view.findViewById(R.id.txtViews)
        val likesText: TextView = view.findViewById(R.id.txtLikes)
        val likeCount: TextView = view.findViewById(R.id.txtLikeCount)
        val duration: TextView = view.findViewById(R.id.txtDuration)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnShare: ImageButton = view.findViewById(R.id.btnShare)
        val btnDownload: ImageButton = view.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]

        holder.title.text = video.title
        holder.creator.text = video.user?.name ?: "Unknown Creator"
        holder.viewsCount.text = formatViews(video.views_count)
        holder.likesText.text = "${formatLikes(video.likes_count)} likes"
        holder.likeCount.text = formatLikes(video.likes_count)

        // Duration
        if (video.duration > 0) {
            holder.duration.visibility = View.VISIBLE
            holder.duration.text = formatDuration(video.duration)
        } else {
            holder.duration.visibility = View.GONE
        }

        // Load thumbnail using Glide
        Glide.with(holder.itemView.context)
            .load(video.thumbnail_url)
            .placeholder(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(holder.thumbnail)

        // Like button state with proper icon tint
        updateLikeButtonState(holder.btnLike, video.is_liked)

        // Handle Play (Clicking the whole item)
        holder.itemView.setOnClickListener {
            onPlay(video)
        }

        // Handle Buttons
        holder.btnLike.setOnClickListener {
            onLike(video)
        }
        holder.btnShare.setOnClickListener {
            onShare(video)
        }
        holder.btnDownload.setOnClickListener {
            onDownload(video)
        }
    }

    private fun updateLikeButtonState(btnLike: ImageButton, isLiked: Boolean) {
        if (isLiked) {
            // Liked state - red heart color
            btnLike.setColorFilter(
                ContextCompat.getColor(btnLike.context, R.color.holo_red_dark)
            )
            btnLike.setImageResource(R.drawable.ic_like_filled)
        } else {
            // Unliked state - gray color
            btnLike.setColorFilter(
                ContextCompat.getColor(btnLike.context, R.color.darker_gray)
            )
            btnLike.setImageResource(R.drawable.ic_like_outline)
        }
    }

    fun updateVideos(newVideos: List<Video>) {
        videos = newVideos
        notifyDataSetChanged()
    }

    fun updateVideoLike(videoId: Int, liked: Boolean, likesCount: Int) {
        val position = videos.indexOfFirst { it.id == videoId }
        if (position != -1) {
            val updatedVideo = videos[position].copy(is_liked = liked, likes_count = likesCount)
            videos = videos.toMutableList().apply {
                this[position] = updatedVideo
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = videos.size

    private fun formatViews(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM views", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK views", count / 1_000.0)
            else -> "$count views"
        }
    }

    private fun formatLikes(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
}

