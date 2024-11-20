package com.practice.recipesapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.firestore.ktx.firestore
import com.practice.recipesapp.databinding.ActivityRegisterBinding
import com.practice.recipesapp.models.UserProfile

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        storage = Firebase.storage

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.addPhotoButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()
            val name = binding.nameInput.text.toString()

            if (validateInputs(email, password, confirmPassword, name)) {
                registerUser(email, password)
            }
        }

        binding.loginText.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String, name: String): Boolean {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (selectedImageUri != null) {
                        uploadProfileImage(user?.uid ?: "", email)
                    } else {
                        createUserProfile(user?.uid ?: "", email, "")
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadProfileImage(userId: String, email: String) {
        val imageRef = storage.reference.child("profile_images/$userId.jpg")
        imageRef.putFile(selectedImageUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                imageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    createUserProfile(userId, email, task.result.toString())
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createUserProfile(userId: String, email: String, profilePicUrl: String) {
        val userProfile = UserProfile(
            userId = userId,
            email = email,
            name = binding.nameInput.text.toString(),
            profilePicUrl = profilePicUrl,
            bio = ""
        )

        Firebase.firestore.collection("users")
            .document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show()
            }
    }
} 