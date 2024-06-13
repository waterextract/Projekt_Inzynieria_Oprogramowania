package com.juliaijustyna.apka

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class PaintActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var saveDrawing: Button
    private lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_paint)

        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Nieprawidłowy identyfikator pokoju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicjalizuj referencję do pokoju w bazie danych Firebase
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)

        saveDrawing = findViewById(R.id.saveDrawing)
        drawingView = findViewById(R.id.drawingView)

        saveDrawing.setOnClickListener {
            saveDrawingToDatabase(drawingView.getBitmap())
        }

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
                        5 -> startActivity(Intent(this@PaintActivity, BestDrawingActivity::class.java).apply { putExtra("roomId", roomId) })
                        // Dodaj więcej przypadków dla innych aktywności
                    }
                    // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu pobierania danych
                Toast.makeText(
                    this@PaintActivity,
                    "Błąd pobierania stanu pokoju: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun saveDrawingToDatabase(imageBitmap: Bitmap) {
        val storageRef = FirebaseStorage.getInstance().getReference("room_drawings/$roomId")

        // Konwertuj obraz na tablicę bajtów
        val imageByteArray = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, imageByteArray)
        val imageData = imageByteArray.toByteArray()

        // Utwórz nazwę pliku
        val fileName = "drawing_${System.currentTimeMillis()}.png"
        val imageRef = storageRef.child(fileName)

        // Prześlij dane obrazu do Firebase Storage
        imageRef.putBytes(imageData)
            .addOnSuccessListener {
                // Pobierz liczbę dodanych rysunków z bazy danych
                roomRef.child("drawingCount")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var drawingCount = snapshot.getValue(Int::class.java) ?: 0
                            drawingCount++

                            // Zapisz liczbę dodanych rysunków w bazie danych
                            roomRef.child("drawingCount").setValue(drawingCount)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Powiadomienie użytkownika o pomyślnym zapisaniu rysunku
                                        Toast.makeText(
                                            this@PaintActivity,
                                            "Pomyślnie zapisano rysunek w bazie danych",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Pobierz liczbę graczy z bazy danych
                                        roomRef.child("playerCount")
                                            .addListenerForSingleValueEvent(object :
                                                ValueEventListener {
                                                override fun onDataChange(playersSnapshot: DataSnapshot) {
                                                    val numberOfPlayers = playersSnapshot.getValue(Int::class.java) ?: 0
                                                    if (drawingCount == numberOfPlayers) {
                                                        roomRef.child("state")
                                                            .setValue("playing5")
                                                            .addOnSuccessListener {
                                                                // Upewnij się, że obecna aktywność zostanie zakończona
                                                                finish()
                                                            }
                                                            .addOnFailureListener { exception ->
                                                                // Obsługa błędu zmiany stanu pokoju
                                                                Toast.makeText(
                                                                    this@PaintActivity,
                                                                    "Błąd zmiany stanu pokoju: ${exception.message}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    // Obsługa błędu podczas odczytu liczby graczy
                                                    Toast.makeText(
                                                        this@PaintActivity,
                                                        "Błąd odczytu liczby graczy: ${error.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            })
                                    } else {
                                        // Obsługa błędu podczas zapisywania liczby dodanych rysunków
                                        Toast.makeText(
                                            this@PaintActivity,
                                            "Błąd zapisu liczby dodanych rysunków w bazie danych: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Obsługa błędu podczas odczytu liczby dodanych rysunków
                            Toast.makeText(
                                this@PaintActivity,
                                "Błąd odczytu liczby dodanych rysunków z bazy danych: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
            .addOnFailureListener { exception ->
                // Obsługa błędu podczas przesyłania obrazu do Firebase Storage
                Toast.makeText(
                    this@PaintActivity,
                    "Błąd przesyłania obrazu do Firebase Storage: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                                                        this@PaintActivity,
                                                        "Pokój został usunięty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Przenieś gracza na stronę główną
                                                    val intent = Intent(
                                                        this@PaintActivity,
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
                                        Intent(this@PaintActivity, HomeFragment::class.java)
                                    startActivity(intent)
                                    finish() // Zakończ bieżącą aktywność, aby nie można było wrócić do pokoju za pomocą przycisku "wstecz"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@PaintActivity,
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
