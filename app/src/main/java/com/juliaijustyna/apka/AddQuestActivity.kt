package com.juliaijustyna.apka

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore



class AddQuestActivity : AppCompatActivity() {

    private lateinit var questionEditText: EditText
    private lateinit var submitButton: Button
    private val firestore = FirebaseFirestore.getInstance()


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
        // Access the "questions" collection in Firebase
        val questionsRef = firestore.collection("questions")

        // Create a new document with a generated ID
        val newQuestionRef = questionsRef.document()

        // Set the data for the document
        val data = hashMapOf(
            "question" to question
            // Add more fields if needed
        )

        // Add the data to the document
        newQuestionRef.set(data)
            .addOnSuccessListener {
                // Document saved successfully
                // You can add any success handling here
            }
            .addOnFailureListener { e ->
                // Error handling
                // You can add any error handling here
            }
    }


        }



