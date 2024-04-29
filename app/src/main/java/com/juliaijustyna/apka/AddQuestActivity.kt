package com.juliaijustyna.apka

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase




class AddQuestActivity : AppCompatActivity() {

    private lateinit var questionEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_quest)


        questionEditText = findViewById(R.id.pytanie)
        submitButton = findViewById(R.id.send)
        database = FirebaseDatabase.getInstance()

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
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            // Generowanie unikalnego 3-liczbowego identyfikatora
            val questionId = generateQuestionId()

            val questionsRef = database.reference.child("questions").child(questionId)
            questionsRef.setValue(question)
                .addOnSuccessListener {
                    Toast.makeText(this, "Przesłano pomyślnie", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd przesyłania: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateQuestionId(): String {
        val random = java.util.Random()
        val questionId = StringBuilder()
        for (i in 0 until 3) {
            questionId.append(random.nextInt(10))
        }
        return questionId.toString()
    }

}






