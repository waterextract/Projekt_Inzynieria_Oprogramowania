package com.juliaijustyna.apka

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LobbyActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference // Referencja do węzła z nazwami użytkowników

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        // Pobierz identyfikator pokoju przekazany z poprzedniej aktywności
        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Nieprawidłowy identyfikator pokoju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicjalizuj referencję do pokoju w bazie danych Firebase
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)

        // Inicjalizuj referencję do węzła z nazwami użytkowników
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        // Znajdź widoki w layoucie
        val roomIdTextView: TextView = findViewById(R.id.roomIdValueTextView)
        val playersListTextView: TextView = findViewById(R.id.playersListTextView)

        // Ustaw wyświetlanie ID pokoju
        roomIdTextView.text = roomId

        // Dodaj nasłuchiwanie na zmiany w liście użytkowników
        roomRef.child("players").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<String>()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: ""
                    // Pobierz nazwę użytkownika na podstawie UID
                    usersRef.child(userId).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userDataSnapshot: DataSnapshot) {
                            val username = userDataSnapshot.value?.toString() ?: ""
                            userList.add(username)
                            // Aktualizuj listę użytkowników w TextView
                            playersListTextView.text = userList.joinToString(", ")
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@LobbyActivity, "Błąd pobierania danych użytkownika: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LobbyActivity, "Błąd pobierania danych: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Obsługa przycisku opuszczania pokoju
        val leaveButton: Button = findViewById(R.id.leaveButton)
        leaveButton.setOnClickListener {
            leaveRoom()
        }
    }

    private fun leaveRoom() {
        // Usuń bieżącego użytkownika z listy uczestników pokoju
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val playerId = currentUser.uid
            roomRef.child("players").child(playerId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Pomyślnie opuszczono pokój", Toast.LENGTH_SHORT).show()
                    // Przejdź do poprzedniej aktywności lub ekranu głównego
                    onBackPressed()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd podczas opuszczania pokoju: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
