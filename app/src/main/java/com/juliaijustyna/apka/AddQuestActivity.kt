package com.juliaijustyna.apka

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore



class AddQuestActivity : AppCompatActivity() {

    private lateinit var questionEditText: EditText
    private lateinit var submitButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_quest)

        questionEditText = findViewById(R.id.pytanie)
        submitButton = findViewById(R.id.send)

        // Set onClickListener for the submit button
        submitButton.setOnClickListener {
            val questionText = questionEditText.text.toString().trim()

            if (questionText.isNotEmpty()) {
                // Save question to Firebase
                saveQuestionToFirebase(questionText)
            }
        }
    }

    private fun saveQuestionToFirebase(question: String) {

        val questionsRef = FirebaseFirestore.getInstance().collection("questions")

        val newQuestionRef = questionsRef.document()

        val data = hashMapOf("question" to question)
        newQuestionRef.set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "przeslano pomyślnie", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {  Toast.makeText(this, "Błąd przesylania: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


        }



