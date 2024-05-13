package com.juliaijustyna.apka

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AddPhotoActivity : AppCompatActivity() {

    private lateinit var submitButton: Button
    private lateinit var storage: FirebaseStorage
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_photo)
        submitButton = findViewById(R.id.addPhoto)
        storage = FirebaseStorage.getInstance()

        // Set onClickListener for the submit button
        submitButton.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                savePhotoToFirebase(imageUri)
            }
        }
    }

    private fun savePhotoToFirebase(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            val storageRef = storage.reference
            val questionId = generateQuestionId()
            val filePath = storageRef.child("images").child("$questionId.jpg")

            filePath.putFile(imageUri)
                .addOnSuccessListener {
                    filePath.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        val database = FirebaseDatabase.getInstance()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd przesyłania: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateQuestionId(): String {
        val random = java.util.Random()
        val questionId = StringBuilder()
        for (i in 0 until 4) {
            questionId.append(random.nextInt(10))
        }
        return questionId.toString()
    }
}
