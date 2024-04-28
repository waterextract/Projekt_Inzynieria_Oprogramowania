package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class MakeRoom : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_quest)

        // Znajdź przycisk i pola tekstowe w widoku
        val addButton: Button = findViewById(R.id.add_button)
        val descEditText: EditText = findViewById(R.id.Desc)
        val nameEditText: EditText = findViewById(R.id.name)

        // Ustaw nasłuchiwacz zdarzeń dla przycisku "START"
        addButton.setOnClickListener {
            // Pobierz dane z pól tekstowych
            val roomName = nameEditText.text.toString().trim()
            val roomDesc = descEditText.text.toString().trim()

            // Sprawdź, czy pola nie są puste
            if (roomName.isEmpty() || roomDesc.isEmpty()) {
                Toast.makeText(this, "Proszę wypełnić wszystkie pola", Toast.LENGTH_SHORT).show()
            } else {
                // Utwórz pokój w bazie danych Firebase
                createRoom(roomName, roomDesc)
            }
        }
    }

    private fun createRoom(roomName: String, roomDesc: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Generuj unikalne id pokoju
            val roomId = generateRoomId()

            val playerId = currentUser.uid

            val roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)
            roomRef.child("players").child(playerId).setValue(true)
            roomRef.child("hostId").setValue(currentUser.uid)
            roomRef.child("state").setValue("waiting")
                .addOnSuccessListener {
                    Toast.makeText(this, "Pokój został utworzony pomyślnie", Toast.LENGTH_SHORT).show()
                    // Przejdź do kolejnej aktywności (lobby gry) po pomyślnym utworzeniu pokoju
                    goToLobbyActivity(roomId) // Przekazanie roomId do kolejnej aktywności
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd podczas tworzenia pokoju: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun goToLobbyActivity(roomId: String) {
        val intent = Intent(this, LobbyActivity::class.java)
        intent.putExtra("roomId", roomId) // Dodaj roomId do intencji
        startActivity(intent)
        finish() // Zakończ bieżącą aktywność, aby nie można jej było cofnąć
    }

    private fun generateRoomId(): String {
        // Generuj unikalne id z zakresu od 0 do 9999
        val roomId = Random.nextInt(0, 9999)
        return roomId.toString().padStart(4, '0') // Zwróć id jako 4-cyfrowy ciąg znaków, jeśli jest krótszy, uzupełnij zerami z przodu
    }
}
