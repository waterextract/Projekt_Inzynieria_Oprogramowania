package com.juliaijustyna.apka

import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class QuestionActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var answerEditText: EditText
    private lateinit var submitAnswerButton: Button
    private var numberOfAnswers: Int = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)

        // Inicjalizacja pola do wprowadzania odpowiedzi i przycisku
        answerEditText = findViewById(R.id.answerEditText)
        submitAnswerButton = findViewById(R.id.submitAnswerButton)

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

        val playersRef = roomRef.child("players")

        val answerCountRef = roomRef.child("numberOfAnswers")
        answerCountRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                numberOfAnswers = snapshot.getValue(Int::class.java) ?: 0
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu
            }
        })


// Nasłuchuj zmian w liście graczy
        playersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val numberOfPlayers = snapshot.childrenCount.toInt()

                val currentUser = FirebaseAuth.getInstance().currentUser
                val playerId = currentUser?.uid



                submitAnswerButton.setOnClickListener {
                    val answer = answerEditText.text.toString().trim()
                    if (answer.isNotEmpty()) {
                        if (playerId != null) {
                            roomRef.child("answers").child(playerId).setValue(answer)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Aktualizuj liczbę odpowiedzi w bazie danych
                                        roomRef.child("numberOfAnswers").setValue(numberOfAnswers + 1)
                                            .addOnCompleteListener { innerTask ->
                                                if (innerTask.isSuccessful) {
                                                    Toast.makeText(this@QuestionActivity, "Pomyślnie wysłano odpowiedź", Toast.LENGTH_SHORT).show()
                                                    answerEditText.text.clear()

                                                    // Sprawdź, czy liczba odpowiedzi jest równa liczbie graczy
                                                    if (numberOfAnswers == numberOfPlayers) {
                                                        roomRef.child("state").setValue("playing4")
                                                            .addOnSuccessListener {
                                                                // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
                                                                finish()
                                                            }
                                                            .addOnFailureListener { exception ->
                                                                // Obsługa błędu zmiany stanu pokoju
                                                                Toast.makeText(
                                                                    this@QuestionActivity,
                                                                    "Błąd zmiany stanu pokoju: ${exception.message}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        //moveToNewActivity()
                                                    }
                                                } else {
                                                    Toast.makeText(this@QuestionActivity, "Błąd aktualizacji liczby odpowiedzi: ${innerTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(this@QuestionActivity, "Błąd zapisu odpowiedzi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(this@QuestionActivity, "Wprowadź odpowiedź", Toast.LENGTH_SHORT).show()
                    }
                }



                // Pobranie referencji do odpowiedzi z bazy danych

            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu
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
                        4 -> startActivity(Intent(this@QuestionActivity, PaintActivity::class.java).apply { putExtra("roomId", roomId)})
                        // Dodaj więcej przypadków dla innych aktywności
                    }
                    // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu pobierania danych
                Toast.makeText(
                    this@QuestionActivity,
                    "Błąd pobierania stanu pokoju: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    }



    override fun onBackPressed() {
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