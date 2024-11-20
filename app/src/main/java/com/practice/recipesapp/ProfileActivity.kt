package com.practice.recipesapp

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.practice.recipesapp.databinding.ActivityProfileBinding
import com.practice.recipesapp.models.UserProfile

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private var selectedImageUri: Uri? = null
    
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImage.setImageURI(it)
            uploadImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)

            auth = Firebase.auth

            loadUserProfile()
            setupClickListeners()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        
        binding.editProfileImage.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            updateProfile()
        }

        binding.myRecipesButton.setOnClickListener {
            startActivity(Intent(this, MyRecipesActivity::class.java))
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserProfile::class.java)
                user?.let {
                    binding.nameInput.setText(it.name)
                    binding.bioInput.setText(it.bio)
                    
                    if (it.profilePicUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(it.profilePicUrl)
                            .into(binding.profileImage)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImage(imageUri: Uri) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading image...")
        progressDialog.show()

        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateProfileImage(downloadUri.toString())
                    progressDialog.dismiss()
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfileImage(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .update("profilePicUrl", imageUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile() {
        val userId = auth.currentUser?.uid ?: return
        val name = binding.nameInput.text.toString()
        val bio = binding.bioInput.text.toString()
        
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "bio" to bio
        )
        
        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
} 