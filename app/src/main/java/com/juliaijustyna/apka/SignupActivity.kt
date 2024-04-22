package com.juliaijustyna.apka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.juliaijustyna.apka.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth


    private var email=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        binding.signupButton.setOnClickListener{

            email = binding.signupEmail.text.toString()
            val username = binding.signupUsername.text.toString()
            val password = binding.signupPassword.text.toString()
            val confirmPassword = binding.signupConfirm.text.toString()
            if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()){
                // Sprawdź, czy nazwa użytkownika jest już w użyciu
                val ref = FirebaseDatabase.getInstance().getReference("Users")
                ref.orderByChild("name").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Nazwa użytkownika jest już zajęta, wyświetl komunikat
                            Toast.makeText(this@SignupActivity, "Username is already taken", Toast.LENGTH_SHORT).show()
                        } else {
                            // Nazwa użytkownika jest dostępna, utwórz konto użytkownika
                            if (password == confirmPassword){
                                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this@SignupActivity){
                                    if (it.isSuccessful){
                                        updateUserInfo()
                                        val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this@SignupActivity, it.exception.toString(), Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                }
                            } else {
                                Toast.makeText(this@SignupActivity, "Password does not match", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Obsłużanie błędu zapytania do bazy danych
                        Toast.makeText(
                            this@SignupActivity,
                            "Database error: ${databaseError.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirectText.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }

    private fun updateUserInfo(){

        val timestamp=System.currentTimeMillis()

        val uid=firebaseAuth.uid

        val username = binding.signupUsername.text.toString() // Pobierz nazwę użytkownika

        val hashMap : HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = username
        hashMap["profileImage"] = ""
        hashMap["timestamp"] = timestamp

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)

    }
}