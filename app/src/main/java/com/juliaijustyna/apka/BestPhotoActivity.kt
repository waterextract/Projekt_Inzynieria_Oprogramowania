package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference

class BestPhotoActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var storageRef: StorageReference
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private val photosList = mutableListOf<Photo>()
    private var voted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_best_photo)

        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Nieprawidłowy identyfikator pokoju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)
        storageRef = FirebaseStorage.getInstance().getReference("room_images/$roomId")

        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        photosRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PhotoAdapter(photosList, this::onPhotoVoted)
        photosRecyclerView.adapter = adapter

        // Pobierz wszystkie zdjęcia z Firebase Storage
        storageRef.listAll().addOnSuccessListener { listResult ->
            photosList.clear()
            for (item in listResult.items) {
                item.downloadUrl.addOnSuccessListener { uri ->
                    photosList.add(Photo(item.name, uri.toString()))
                    adapter.notifyDataSetChanged()
                }.addOnFailureListener { exception ->
                    Toast.makeText(this@BestPhotoActivity, "Błąd pobierania zdjęcia: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this@BestPhotoActivity, "Błąd listowania zdjęć: ${exception.message}", Toast.LENGTH_SHORT).show()
        }

        // Sprawdź, czy użytkownik już głosował
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            roomRef.child("votes").child(it.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        voted = true
                        Toast.makeText(this@BestPhotoActivity, "Już oddałeś swój głos.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BestPhotoActivity, "Błąd sprawdzania głosu: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Pobierz wyniki głosowania
        roomRef.child("votes").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val votesCount = mutableMapOf<String, Int>()
                for (voteSnapshot in snapshot.children) {
                    val photoId = voteSnapshot.getValue(String::class.java) ?: ""
                    votesCount[photoId] = votesCount.getOrDefault(photoId, 0) + 1
                }

                // Pokaż wyniki głosowania
                val results = votesCount.entries.sortedByDescending { it.value }
                val resultsText = results.joinToString("\n") { "${it.key}: ${it.value} votes" }
                findViewById<TextView>(R.id.votingResultsTextView).text = resultsText
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BestPhotoActivity, "Błąd pobierania wyników głosowania: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun onPhotoVoted(photoId: String) {
        if (voted) {
            Toast.makeText(this, "Już oddałeś swój głos.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            roomRef.child("votes").child(it.uid).setValue(photoId)
                .addOnSuccessListener {
                    Toast.makeText(this, "Głos został oddany", Toast.LENGTH_SHORT).show()
                    voted = true
                    // Aktualizacja wyników głosowania po oddaniu głosu
                    updateVotingResults()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Błąd oddawania głosu: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateVotingResults() {
        roomRef.child("votes").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val votesCount = mutableMapOf<String, Int>()
                for (voteSnapshot in snapshot.children) {
                    val photoId = voteSnapshot.getValue(String::class.java) ?: ""
                    votesCount[photoId] = votesCount.getOrDefault(photoId, 0) + 1
                }

                // Pokaż wyniki głosowania
                val results = votesCount.entries.sortedByDescending { it.value }
                val resultsText = results.joinToString("\n") { "${it.key}: ${it.value} votes" }
                findViewById<TextView>(R.id.votingResultsTextView).text = resultsText
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BestPhotoActivity, "Błąd pobierania wyników głosowania: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        // Przenieś logikę opuszczania pokoju tutaj
        // Usuń bieżącego użytkownika z listy uczestników pokoju
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val playerId = currentUser.uid

            // Usuń gracza z listy graczy pokoju
            roomRef.child("players").child(playerId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Pomyślnie opuszczono pokój", Toast.LENGTH_SHORT)
                        .show()

                    // Dodatkowo, sprawdź, czy liczba graczy w pokoju wynosi zero
                    roomRef.child("players")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) { // Jeśli liczba graczy wynosi zero
                                    // Ustaw stan pokoju na "empty"
                                    roomRef.child("state").setValue("empty")
                                        .addOnSuccessListener {
                                            // Usuń pokój z bazy danych
                                            roomRef.removeValue()
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this@BestPhotoActivity,
                                                        "Pokój został usunięty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Przenieś gracza na stronę główną
                                                    val intent = Intent(
                                                        this@BestPhotoActivity,
                                                        HomeFragment::class.java
                                                    )
                                                    startActivity(intent)
                                                    finish() // Zakończ bieżącą aktywność, aby nie można było wrócić do pokoju za pomocą przycisku "wstecz"
                                                }
                                                .addOnFailureListener {
                                                    // Obsługa błędu usuwania pokoju
                                                }
                                        }
                                        .addOnFailureListener {
                                            // Obsługa błędu ustawiania stanu pokoju
                                        }
                                } else {
                                    // Przenieś gracza na stronę główną
                                    val intent =
                                        Intent(this@BestPhotoActivity, HomeFragment::class.java)
                                    startActivity(intent)
                                    finish() // Zakończ bieżącą aktywność, aby nie można było wrócić do pokoju za pomocą przycisku "wstecz"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@BestPhotoActivity,
                                    "Błąd pobierania danych: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Błąd podczas opuszczania pokoju: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

    }
}


data class Photo(val id: String, val url: String)
