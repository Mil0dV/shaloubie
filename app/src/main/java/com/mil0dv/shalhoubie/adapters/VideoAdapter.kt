import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mil0dv.shalhoubie.databinding.ItemVideoBinding
import com.mil0dv.shalhoubie.models.YouTubeVideo

class VideoAdapter(
    private val onDownloadClick: (YouTubeVideo) -> Unit
) : ListAdapter<YouTubeVideo, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.buttonDownload.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDownloadClick(getItem(position))
                }
            }
        }

        fun bind(video: YouTubeVideo) {
            binding.apply {
                textViewTitle.text = video.snippet.title
                
                // Load thumbnail using Glide with centerCrop
                Glide.with(imageViewThumbnail.context)
                    .load(video.snippet.thumbnails.default.url)
                    .centerCrop()
                    .into(imageViewThumbnail)
            }
        }
    }

    private class VideoDiffCallback : DiffUtil.ItemCallback<YouTubeVideo>() {
        override fun areItemsTheSame(oldItem: YouTubeVideo, newItem: YouTubeVideo): Boolean {
            return oldItem.id.videoId == newItem.id.videoId
        }

        override fun areContentsTheSame(oldItem: YouTubeVideo, newItem: YouTubeVideo): Boolean {
            return oldItem == newItem
        }
    }
}