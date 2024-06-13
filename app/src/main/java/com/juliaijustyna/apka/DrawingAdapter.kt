package com.juliaijustyna.apka

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class DrawingAdapter(private val drawings: List<Drawing>, private val voteCallback: (String) -> Unit) : RecyclerView.Adapter<DrawingAdapter.DrawingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawing, parent, false)
        return DrawingViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrawingViewHolder, position: Int) {
        val drawing = drawings[position]
        holder.drawingImageView.post {
            Picasso.get()
                .load(drawing.url)
                .resize(holder.drawingImageView.width, holder.drawingImageView.height)
                .centerCrop()
                .into(holder.drawingImageView)
        }
        holder.voteButton.setOnClickListener {
            voteCallback(drawing.id)
        }
    }

    override fun getItemCount() = drawings.size

    class DrawingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val drawingImageView: ImageView = itemView.findViewById(R.id.drawingImageView)
        val voteButton: Button = itemView.findViewById(R.id.voteButton)
    }
}
