package com.juliaijustyna.apka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class QuestionActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)

        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Nieprawidłowy identyfikator pokoju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicjalizuj referencję do pokoju w bazie danych Firebase
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)

        // Inicjalizacja widoku TextView dla pytania
        questionTextView = findViewById(R.id.questionTextView)

        // Pobranie referencji do pytania z bazy danych
        val questionRef = roomRef.child("question")

        // Nasłuchuj zmian w pytaniu
        questionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val question = snapshot.getValue(String::class.java)
                question?.let {
                    // Wyświetlenie pytania w interfejsie użytkownika
                    questionTextView.text = it
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@QuestionActivity,
                    "Błąd pobierania pytania: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Sprawdź, czy pytanie już istnieje w bazie danych pokoju
        questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Jeśli pytanie nie istnieje, pobierz losowe pytanie i zapisz je w bazie danych pokoju
                    getRandomQuestion()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu pobierania pytania
            }
        })
    }

    private fun getRandomQuestion() {
        // Pobranie referencji do wszystkich pytań w bazie danych Firebase
        val questionsRef = FirebaseDatabase.getInstance().getReference("questions")

        questionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Sprawdzenie, czy istnieją jakiekolwiek pytania w bazie danych
                if (snapshot.exists()) {
                    // Losowanie jednego losowego pytania
                    val questions = snapshot.children.toList()
                    val randomQuestion = questions.random()

                    // Pobranie treści pytania
                    val questionText = randomQuestion.getValue(String::class.java)

                    // Zapisz wylosowane pytanie w bazie danych pokoju
                    roomRef.child("question").setValue(questionText)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Powiadomienie użytkownika o pomyślnym zapisaniu pytania
                                Toast.makeText(
                                    this@QuestionActivity,
                                    "Pomyślnie zapisano pytanie",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Obsługa błędu podczas zapisywania pytania
                                Toast.makeText(
                                    this@QuestionActivity,
                                    "Błąd zapisu pytania: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(
                        this@QuestionActivity,
                        "Brak pytań w bazie danych",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@QuestionActivity,
                    "Błąd pobierania pytań: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                                                        this@QuestionActivity,
                                                        "Pokój został usunięty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Przenieś gracza na stronę główną
                                                    val intent = Intent(
                                                        this@QuestionActivity,
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
                                        Intent(this@QuestionActivity, HomeFragment::class.java)
                                    startActivity(intent)
                                    finish() // Zakończ bieżącą aktywność, aby nie można było wrócić do pokoju za pomocą przycisku "wstecz"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@QuestionActivity,
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