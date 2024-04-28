package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.random.Random

class LobbyActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference // Referencja do węzła z nazwami użytkowników
    private lateinit var playersRecyclerView: RecyclerView
    private lateinit var hostId: String

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

        roomRef.child("hostId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                hostId = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu pobierania identyfikatora gospodarza
            }
        })


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

        roomRef.child("state").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Pobierz wartość stanu pokoju
                val state = snapshot.getValue(String::class.java)

                // Sprawdź, czy stan pokoju to "playing"
                if (state == "playing") {
                    // Rozpocznij aktywność w zależności od wylosowanej liczby
                    startActv()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu pobierania danych
                Toast.makeText(
                    this@LobbyActivity,
                    "Błąd pobierania stanu pokoju: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Obsługa przycisku opuszczania pokoju
        val leaveButton: Button = findViewById(R.id.leaveButton)
        leaveButton.setOnClickListener {
            leaveRoom()
        }

        // pzrycisk Start
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            startActv()
        }
    }

    private fun leaveRoom() {
        // Usuń bieżącego użytkownika z listy uczestników pokoju
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val playerId = currentUser.uid

            // Usuń gracza z listy graczy pokoju
            roomRef.child("players").child(playerId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Pomyślnie opuszczono pokój", Toast.LENGTH_SHORT).show()

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
                                                        this@LobbyActivity,
                                                        "Pokój został usunięty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Przenieś gracza na stronę główną
                                                    val intent = Intent(
                                                        this@LobbyActivity,
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
                                        Intent(this@LobbyActivity, HomeFragment::class.java)
                                    startActivity(intent)
                                    finish() // Zakończ bieżącą aktywność, aby nie można było wrócić do pokoju za pomocą przycisku "wstecz"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@LobbyActivity,
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


    private fun startActv() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.uid == hostId) {
            Toast.makeText(this@LobbyActivity, "Losowanie aktywności!", Toast.LENGTH_SHORT).show()

            // Losowanie liczby od 1 do 3 (możesz dostosować zakres do ilości dostępnych aktywności)
            val randomNumber = Random.nextInt(1, 4)

            // W zależności od wylosowanej liczby, uruchamiamy odpowiednią aktywność
            when (randomNumber) {
                1 -> startActivity(Intent(this, QuestionActivity::class.java))
                2 -> startActivity(Intent(this, PhotoActivity::class.java))
                3 -> startActivity(Intent(this, PaintActivity::class.java))
                // Dodaj więcej przypadków dla innych aktywności
            }

            // Zmiana stanu pokoju na "playing"
            roomRef.child("state").setValue("playing")
                .addOnFailureListener {
                    // Obsługa błędu zmiany stanu pokoju
                    Toast.makeText(
                        this@LobbyActivity,
                        "Błąd zmiany stanu pokoju: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
            finish()
        } else {
            Toast.makeText(
                this@LobbyActivity,
                "Tylko gospodarz może rozpocząć grę",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}



