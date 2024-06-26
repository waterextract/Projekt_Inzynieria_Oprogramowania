package com.juliaijustyna.apka

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso



class PlayersAdapter(private val players: List<String>) :
    RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.player_item, parent, false)
        return PlayerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val userId = players[position]
        // Pobierz nazwę użytkownika na podstawie UID
        val usersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.child(userId).child("name").get().addOnSuccessListener { dataSnapshot ->
            val username = dataSnapshot.value?.toString() ?: ""
            holder.usernameTextView.text = username
        }
        // Pobierz avatar użytkownika na podstawie UID
        // Tutaj można dodać logikę pobierania avatara i ustawiania go w ImageView
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_images").child("$userId.jpg")

        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            // Pobierz adres URL obrazu
            val imageUrl = downloadUri.toString()
            // Załaduj obraz za pomocą Picasso
            Picasso.get().load(imageUrl).into(holder.avatarImageView)
        }.addOnFailureListener { exception ->
            // Obsługa błędu podczas pobierania adresu URL obrazu
        }
    }

    override fun getItemCount(): Int {
        return players.size
    }
}

