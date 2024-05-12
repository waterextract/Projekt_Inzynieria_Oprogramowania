package com.juliaijustyna.apka

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddPhotoActivity : AppCompatActivity() {

    private lateinit var submitButton : Button
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_photo)
        submitButton = findViewById(R.id.addPhoto)
        database = FirebaseDatabase.getInstance()

        // Set onClickListener for the submit button
        submitButton.setOnClickListener {

                // Save question to Firebase
                savePhotoToFirebase()

        }
    }

    private fun savePhotoToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            // Generowanie unikalnego 3-liczbowego identyfikatora
            val questionId = generateQuestionId()
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