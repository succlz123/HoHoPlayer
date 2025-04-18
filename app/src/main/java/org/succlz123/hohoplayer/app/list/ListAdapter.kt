package org.succlz123.hohoplayer.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.succlz123.hohoplayer.app.support.VideoItem
import org.succlz123.hohoplayer.ui.list.ListPlayer
import org.succlz123.hohoplayer.ui.list.ListPlayerAdapter
import org.succlz123.hohoplayer.ui.list.ListPlayerViewHolder

class ListAdapter(val listPlayer: ListPlayer, val items: List<VideoItem>) : RecyclerView.Adapter<VideoItemHolder>(), ListPlayerAdapter {

    companion object {
        const val TAG = "ListAdapter"
    }

    var onListListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemHolder {
        return VideoItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false))
    }

    override fun onBindViewHolder(holder: VideoItemHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.title.context)
                .load(item.cover)
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.image)
        holder.videoUrl = item.path
        holder.title.text = item.displayName
        holder.title.setOnClickListener {
            onListListener?.invoke()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onViewRecycled(holder: VideoItemHolder) {
        super.onViewRecycled(holder)
        recycleListPlayViewHolder(listPlayer, holder)
    }
}

class VideoItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ListPlayerViewHolder {
    var playerContainer: FrameLayout = itemView.findViewById(R.id.playerContainer)
    var image: ImageView = itemView.findViewById(R.id.image)
    var title: TextView = itemView.findViewById(R.id.title)

    override var videoUrl: String? = null

    override fun getPlayerContainer(): ViewGroup {
        return playerContainer
    }

    override fun getCoverImage(): ImageView? {
        return image
    }

    override fun getViewHolder(): RecyclerView.ViewHolder {
        return this
    }
}
