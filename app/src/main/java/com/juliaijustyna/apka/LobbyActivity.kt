package com.juliaijustyna.apka

import android.content.ClipData
import android.content.ClipboardManager
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

        val copyRoomIdButton: Button = findViewById(R.id.copyRoomIdButton)
        copyRoomIdButton.setOnClickListener {
            copyRoomIdToClipboard()
        }

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
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: ""
                    userList.add(userId)
                }

                // Aktualizacja liczby graczy w pokoju
                updatePlayerCount(userList.size)

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

        // Dodaj nasłuchiwanie zmiany stanu pokoju
        roomRef.child("state").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Pobierz wartość stanu pokoju
                val state = snapshot.getValue(String::class.java)

                // Sprawdź, czy stan pokoju zaczyna się od "playing" (czyli gra została rozpoczęta przez hosta)
                if (state != null && state.startsWith("playing")) {
                    // Pobierz numer aktywności z informacji przekazanej przez hosta
                    val activityNumber = state.substringAfter("playing").toInt()

                    // Uruchom odpowiednią aktywność w zależności od wylosowanego numeru aktywności
                    when (activityNumber) {
                        1 -> startActivity(Intent(this@LobbyActivity, QuestionActivity::class.java).apply { putExtra("roomId", roomId)})
                        2 -> startActivity(Intent(this@LobbyActivity, PhotoActivity::class.java).apply { putExtra("roomId", roomId)})
                        3 -> startActivity(Intent(this@LobbyActivity, PaintActivity::class.java).apply { putExtra("roomId", roomId)})
                        // Dodaj więcej przypadków dla innych aktywności
                    }
                    // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
                    finish()
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

    private fun copyRoomIdToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Room ID", roomId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "ID pokoju skopiowane do schowka", Toast.LENGTH_SHORT).show()
    }


    override fun onBackPressed() {
        // Przenieś logikę opuszczania pokoju tutaj
        leaveRoom()
    }

    // Funkcja do aktualizacji liczby graczy w pokoju
    private fun updatePlayerCount(count: Int) {
        roomRef.child("playerCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                mutableData.value = count
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (databaseError != null) {
                    // Obsługa błędu aktualizacji
                    Toast.makeText(
                        this@LobbyActivity,
                        "Błąd aktualizacji liczby graczy: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
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
            val randomNumber = Random.nextInt(2, 3)

            // Zmiana stanu pokoju na "playing" i przekazanie informacji o wylosowanej aktywności
            roomRef.child("state").setValue("playing$randomNumber")
                .addOnSuccessListener {
                    // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
                    finish()
                }
                .addOnFailureListener { exception ->
                    // Obsługa błędu zmiany stanu pokoju
                    Toast.makeText(
                        this@LobbyActivity,
                        "Błąd zmiany stanu pokoju: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(
                this@LobbyActivity,
                "Tylko gospodarz może rozpocząć grę",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}



