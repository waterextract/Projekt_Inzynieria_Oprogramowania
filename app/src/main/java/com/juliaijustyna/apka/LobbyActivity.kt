package com.juliaijustyna.apka

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LobbyActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference // Referencja do węzła z nazwami użytkowników
    private lateinit var playersRecyclerView: RecyclerView

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
        playersRecyclerView = findViewById(R.id.playersRecyclerView)

        // Ustaw wyświetlanie ID pokoju
        roomIdTextView.text = roomId

        // Ustawienie layout managera dla RecyclerView
        playersRecyclerView.layoutManager = LinearLayoutManager(this)

        // Dodaj nasłuchiwanie na zmiany w liście użytkowników
        roomRef.child("players").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<String>()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: ""
                    userList.add(userId)
                }
                // Utwórz i ustaw adapter dla RecyclerView
                val adapter = PlayersAdapter(userList)
                playersRecyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@LobbyActivity,
                    "Błąd pobierania danych: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Obsługa przycisku opuszczania pokoju
        val leaveButton: Button = findViewById(R.id.leaveButton)
        leaveButton.setOnClickListener {
            leaveRoom()
        }
    }

    private fun leaveRoom() {
        // Dodaj kod obsługujący opuszczanie pokoju
    }
}
