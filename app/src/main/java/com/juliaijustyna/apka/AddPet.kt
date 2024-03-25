package com.juliaijustyna.apka

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.juliaijustyna.apka.databinding.ActivityAddpetBinding

class AddPet : AppCompatActivity() {
    private lateinit var binding: ActivityAddpetBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAddpetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.addButton.setOnClickListener{
            validateData()
        }

    }
    private var name = ""
    private var date = ""

    private fun validateData(){

        name = binding.name.text.toString().trim()

        if(name.isEmpty()){
            Toast.makeText(this, "Enter Name...", Toast.LENGTH_SHORT).show()
        }
        else{
            addPetFirebase()
        }

    }

    private fun addPetFirebase(){

        progressDialog.show()

        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["PetName"] = name
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Names")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener{
                progressDialog.dismiss()
                Toast.makeText(this,"Added successfully!", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener{
                progressDialog.dismiss()
                Toast.makeText(this,"Failed to add ", Toast.LENGTH_SHORT).show()

            }


    }
}