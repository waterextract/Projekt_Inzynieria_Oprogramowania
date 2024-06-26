package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class JoinRoom : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_room)

        // Znajdź przycisk i pole tekstowe w widoku
        val joinButton: Button = findViewById(R.id.join_button)
        val roomIdEditText: EditText = findViewById(R.id.room_id)

        // Ustaw nasłuchiwacz zdarzeń dla przycisku "DOŁĄCZ"
        joinButton.setOnClickListener {
            // Pobierz identyfikator pokoju wprowadzony przez użytkownika
            val roomId = roomIdEditText.text.toString().trim()

            // Sprawdź, czy pole nie jest puste
            if (roomId.isEmpty()) {
                Toast.makeText(this, "Proszę wprowadzić identyfikator pokoju", Toast.LENGTH_SHORT).show()
            } else {
                // Dołącz do pokoju
                joinRoom(roomId)
            }
        }
    }

    private fun joinRoom(roomId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val playerId = currentUser.uid

            val roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)

            roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        roomRef.child("players").child(playerId).setValue(true)
                            .addOnSuccessListener {
                                Toast.makeText(this@JoinRoom, "Pomyślnie dołączono do pokoju", Toast.LENGTH_SHORT).show()
                                // Przejdź do kolejnej aktywności (lobby gry) po dołączeniu do pokoju
                                goToLobbyActivity(roomId) // Przekazanie roomId do kolejnej aktywności
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@JoinRoom, "Błąd podczas dołączania do pokoju: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this@JoinRoom, "Podany identyfikator pokoju jest nieprawidłowy", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@JoinRoom, "Błąd: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun goToLobbyActivity(roomId: String) {
        val intent = Intent(this, LobbyActivity::class.java)
        intent.putExtra("roomId", roomId) // Dodaj roomId do intencji
        startActivity(intent)
        finish() // Zakończ bieżącą aktywność, aby nie można jej było cofnąć
    }
}
