package com.juliaijustyna.apka

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class PhotoAdapter(private val photos: List<Photo>, private val voteCallback: (String) -> Unit) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        holder.photoImageView.post {
            Picasso.get()
                .load(photo.url)
                .resize(holder.photoImageView.width, holder.photoImageView.height)
                .centerCrop()
                .into(holder.photoImageView)
        }
        holder.voteButton.setOnClickListener {
            voteCallback(photo.id)
        }
    }

    override fun getItemCount() = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val voteButton: Button = itemView.findViewById(R.id.voteButton)
    }
}
