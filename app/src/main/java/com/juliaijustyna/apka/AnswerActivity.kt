package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AnswerActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnswersAdapter
    private val answerList = mutableListOf<Answer>()
    private val playerNames = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer)

        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Nieprawidłowy identyfikator pokoju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        recyclerView = findViewById(R.id.answersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AnswersAdapter(answerList, playerNames)
        recyclerView.adapter = adapter

        loadPlayerNames()
    }

    private fun loadPlayerNames() {
        roomRef.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playerNames.clear()
                val playerIds = snapshot.children.map { it.key ?: "" }
                for (playerId in playerIds) {
                    usersRef.child(playerId).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(nameSnapshot: DataSnapshot) {
                            val playerName = nameSnapshot.getValue(String::class.java) ?: ""
                            playerNames[playerId] = playerName
                            if (playerNames.size == playerIds.size) {
                                fetchAnswers()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@AnswerActivity, "Błąd pobierania nazw graczy: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AnswerActivity, "Błąd pobierania graczy: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAnswers() {
        val answersRef = roomRef.child("answers")
        answersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val answersList = mutableListOf<Answer>()
                for (answerSnapshot in snapshot.children) {
                    val playerId = answerSnapshot.key ?: ""
                    val answerText = answerSnapshot.getValue(String::class.java) ?: ""
                    val playerName = playerNames[playerId] ?: "Unknown"
                    val answer = Answer(playerId, playerName, answerText)
                    answersList.add(answer)
                }
                // Aktualizujemy RecyclerView za pomocą nowych odpowiedzi
                adapter = AnswersAdapter(answersList, playerNames)
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AnswerActivity, "Błąd pobierania odpowiedzi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val playerId = currentUser.uid
            roomRef.child("players").child(playerId).removeValue()
                .addOnSuccessListener {
                    roomRef.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                roomRef.child("state").setValue("empty")
                                    .addOnSuccessListener {
                                        roomRef.removeValue()
                                            .addOnSuccessListener {
                                                Toast.makeText(this@AnswerActivity, "Pokój został usunięty", Toast.LENGTH_SHORT).show()
                                                startActivity(Intent(this@AnswerActivity, HomeFragment::class.java))
                                                finish()
                                            }
                                    }
                            } else {
                                startActivity(Intent(this@AnswerActivity, HomeFragment::class.java))
                                finish()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@AnswerActivity, "Błąd pobierania danych: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd podczas opuszczania pokoju: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}