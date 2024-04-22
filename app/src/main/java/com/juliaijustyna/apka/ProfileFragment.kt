package com.juliaijustyna.apka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [InfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    private lateinit var textViewUsername: TextView
    private lateinit var changeProfileImageButton: ImageButton
    private lateinit var profileImageView: ImageView
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var currentUserUid: String? = null
    private var profileImageUri: Uri? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicjalizacja launcher'a do otwierania galerii
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                profileImageUri = it
                // Aktualizacja obrazu profilowego w bazie danych Firebase
                updateProfileImage()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid = it }
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        textViewUsername = view.findViewById(R.id.textViewUsername)
        profileImageView = view.findViewById(R.id.imageViewProfile)
        changeProfileImageButton = view.findViewById(R.id.profile_edit_button)
        changeProfileImageButton.setOnClickListener { changeProfileImageOnClick() }
        val buttonBack: ImageButton = view.findViewById(R.id.back_button)
        loadProfileImage()
        loadUsername()

        buttonBack.setOnClickListener {
            val homeFragment = HomeFragment()

            val transaction = requireActivity().supportFragmentManager.beginTransaction()

            transaction.replace(R.id.fragment_container, homeFragment)

            transaction.addToBackStack(null)

            transaction.commit()
        }



        return view
    }

    private fun loadProfileImage() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_images").child("$uid.jpg")

        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            // Pobierz adres URL obrazu
            val imageUrl = downloadUri.toString()


             Picasso.get().load(imageUrl).into(profileImageView)
        }.addOnFailureListener { exception ->
            // Obsługa błędu podczas pobierania adresu URL obrazu
        }
    }

    private fun loadUsername() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            currentUserUid = it.uid
            FirebaseDatabase.getInstance().getReference("Users").child(it.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val username = dataSnapshot.child("name").value.toString()
                            textViewUsername.text = username
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
        }
    }

    private fun changeProfileImageOnClick() {
        // Uruchom galerię zdjęć za pomocą launchera
        galleryLauncher.launch("image/*")
    }

    private fun updateProfileImage() {
            profileImageUri?.let { uri ->
                val storageRef = FirebaseStorage.getInstance().reference
                val imageRef = storageRef.child("profile_images").child("$uid.jpg")

                imageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // Sprawdź, czy downloadUri nie jest nullem
                            downloadUri?.let { uri ->
                                // Zapisz URL obrazu w bazie danych Firebase
                                val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid!!)
                                ref.child("profileImage").setValue(uri.toString())
                                    .addOnSuccessListener {
                                        // Sukces
                                    }
                                    .addOnFailureListener { exception ->
                                        // Obsługa błędu podczas ustawiania wartości
                                    }
                            }
                        }
                    }

            }
        }
    }

