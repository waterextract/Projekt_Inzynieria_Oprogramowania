package com.juliaijustyna.apka

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.result.contract.ActivityResultContracts
import com.squareup.picasso.Picasso


class PhotoActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
   // private lateinit var takePictureLauncher: ActivityResultLauncher
    private lateinit var sendPhoto: Button
    private lateinit var photoAct: ImageView
    private var UserUid: String? = null
    private var PhotoUri: Uri? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_photo)

        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Nieprawidłowy identyfikator pokoju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicjalizuj referencję do pokoju w bazie danych Firebase
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sendPhoto = findViewById(R.id.sendPhoto)
        photoAct = findViewById(R.id.photoAct)
        val photoRef = roomRef.child("images")

// Nasłuchuj zmian w zdjęciu
        photoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.getValue(String::class.java)
                imageUrl?.let {
                    // Wyświetlenie zdjęcia w interfejsie użytkownika
                    Picasso.get().load(it).into(photoAct)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PhotoActivity,
                    "Błąd pobierania zdjęcia: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

// Sprawdź, czy zdjęcie już istnieje w bazie danych pokoju
        photoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Jeśli zdjęcie nie istnieje, pobierz losowe zdjęcie i zapisz je w bazie danych pokoju
                    getRandomPhoto()
                }
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PhotoActivity,
                    "Błąd pobierania zdjęcia: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

      /* val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                // Pomyślnie wykonano zdjęcie, tutaj możesz przetwarzać lub aktualizować obraz profilowy
                updatePhoto()
            }
        }

        fun openCamera() {
            val photoUri = createImageUri() // Tutaj pozostawiamy typ Uri
            takePictureLauncher.launch(photoUri)
        }

        sendPhoto.setOnClickListener { openCamera() }

    }

    // Gdy chcesz otworzyć aparat, wywołaj launcher


    // Funkcja do tworzenia URI dla zapisanego zdjęcia
    private fun createImageUri(): Uri {
        val context = applicationContext
        val contentValues = ContentValues().apply {
           put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From the camera")
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
    }



    private fun updatePhoto() {
        PhotoUri?.let { uri ->
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("game_images").child("$uid.jpg")

            imageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Sprawdź, czy downloadUri nie jest nullem
                        downloadUri?.let { uri ->
                            // Zapisz URL obrazu w bazie danych Firebase
                            val ref =
                                FirebaseDatabase.getInstance().getReference("Users").child(uid!!)
                            ref.child("Photo").setValue(uri.toString())
                                .addOnSuccessListener {
                                }
                                .addOnFailureListener { exception ->
                                    // Obsługa błędu podczas ustawiania wartości
                                }
                        }
                    }
                }

        }
    }
*/


    private fun getRandomPhoto() {
        val storageRef = FirebaseStorage.getInstance().getReference("images")
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    // Wybierz losowy plik z listy
                    val randomFile = listResult.items.random()

                    // Pobierz URL wybranego pliku
                    randomFile.downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            val imageUrl = downloadUrl.toString()

                            // Zapisz adres URL wybranego zdjęcia w bazie danych pokoju
                            roomRef.child("images").setValue(imageUrl)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Powiadomienie użytkownika o pomyślnym zapisaniu zdjęcia
                                        Toast.makeText(
                                            this@PhotoActivity,
                                            "Pomyślnie zapisano zdjęcie",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // Obsługa błędu podczas zapisywania zdjęcia
                                        Toast.makeText(
                                            this@PhotoActivity,
                                            "Błąd zapisu zdjęcia: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                } else {
                    Toast.makeText(
                        this@PhotoActivity,
                        "Brak zdjęć w bazie danych",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this@PhotoActivity,
                    "Błąd pobierania listy plików: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun displayPhotoForParticipants(imageUrl: String) {
        Picasso.get().load(imageUrl).into(photoAct)
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
                                                        this@PhotoActivity,
                                                        "Pokój został usunięty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Przenieś gracza na stronę główną
                                                    val intent = Intent(
                                                        this@PhotoActivity,
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
                                        Intent(this@PhotoActivity, HomeFragment::class.java)
                                    startActivity(intent)
                                    finish() // Zakończ bieżącą aktywność, aby nie można było wrócić do pokoju za pomocą przycisku "wstecz"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@PhotoActivity,
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