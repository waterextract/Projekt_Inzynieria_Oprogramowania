package com.juliaijustyna.apka

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
import android.Manifest
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

class PhotoActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomRef: DatabaseReference
    private lateinit var sendPhoto: Button
    private lateinit var photoAct: ImageView

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
        sendPhoto.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

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
                        4 -> startActivity(Intent(this@PhotoActivity, AnswerActivity::class.java).apply { putExtra("roomId", roomId)})
                        // Dodaj więcej przypadków dla innych aktywności
                    }
                    // Upewnij się, że obecna aktywność zostanie zakończona, aby użytkownik nie mógł wrócić do niej za pomocą przycisku "wstecz"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Obsługa błędu pobierania danych
                Toast.makeText(
                    this@PhotoActivity,
                    "Błąd pobierania stanu pokoju: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Uprawnienia do aparatu zostały udzielone, możesz teraz uruchomić kamerę
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra("android.intent.extras.CAMERA_FACING", 1) // 1 indicates front camera
                putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
            startForResult.launch(takePictureIntent)
        } else {
            // Obsłuż sytuację, gdy użytkownik nie udzielił uprawnień
            Toast.makeText(this, "Uprawnienia do aparatu nie zostały udzielone", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Sprawdź, czy dane nie są puste
            result.data?.extras?.get("data")?.let { imageBitmap ->
                savePhotoToDatabase(imageBitmap as Bitmap)
            }
        }
    }

    private fun savePhotoToDatabase(imageBitmap: Bitmap) {
        val storageRef = FirebaseStorage.getInstance().getReference("room_images")

        // Konwertuj obraz na tablicę bajtów
        val imageByteArray = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, imageByteArray)
        val imageData = imageByteArray.toByteArray()

        // Utwórz nazwę pliku
        val fileName = "photo_${System.currentTimeMillis()}.png"
        val imageRef = storageRef.child(fileName)

        // Prześlij dane obrazu do Firebase Storage
        imageRef.putBytes(imageData)
            .addOnSuccessListener {
                // Pobierz liczbę dodanych zdjęć z bazy danych
                roomRef.child("photoCount").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var photoCount = snapshot.getValue(Int::class.java) ?: 0
                        photoCount++

                        // Zapisz liczbę dodanych zdjęć w bazie danych
                        roomRef.child("photoCount").setValue(photoCount)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Powiadomienie użytkownika o pomyślnym zapisaniu liczby dodanych zdjęć
                                    Toast.makeText(
                                        this@PhotoActivity,
                                        "Pomyślnie zapisano zdjęcie w bazie danych",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Pobierz liczbę graczy z bazy danych
                                    roomRef.child("playerCount").addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(playersSnapshot: DataSnapshot) {
                                            val numberOfPlayers = playersSnapshot.getValue(Int::class.java) ?: 0
                                            if (photoCount == numberOfPlayers) {
                                                roomRef.child("state").setValue("playing4")
                                                    .addOnSuccessListener {
                                                        // Upewnij się, że obecna aktywność zostanie zakończona
                                                        finish()
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        // Obsługa błędu zmiany stanu pokoju
                                                        Toast.makeText(
                                                            this@PhotoActivity,
                                                            "Błąd zmiany stanu pokoju: ${exception.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Obsługa błędu podczas odczytu liczby graczy
                                            Toast.makeText(
                                                this@PhotoActivity,
                                                "Błąd odczytu liczby graczy: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                                } else {
                                    // Obsługa błędu podczas zapisywania liczby dodanych zdjęć
                                    Toast.makeText(
                                        this@PhotoActivity,
                                        "Błąd zapisu liczby dodanych zdjęć w bazie danych: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Obsługa błędu podczas odczytu liczby dodanych zdjęć
                        Toast.makeText(
                            this@PhotoActivity,
                            "Błąd odczytu liczby dodanych zdjęć z bazy danych: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
            .addOnFailureListener { exception ->
                // Obsługa błędu podczas przesyłania obrazu do Firebase Storage
                Toast.makeText(
                    this@PhotoActivity,
                    "Błąd przesyłania obrazu do Firebase Storage: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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