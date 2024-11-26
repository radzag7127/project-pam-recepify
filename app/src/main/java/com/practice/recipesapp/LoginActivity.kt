package com.practice.recipesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.practice.recipesapp.databinding.ActivityLoginBinding
import com.practice.recipesapp.models.UserProfile

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore

    //Menginisialisasi binding untuk mengakses view di layout XML secara type-safe
    // Menghindari findViewById dan null pointer exception dengan sistem binding yang lebih aman

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }


    // Memastikan user berhasil login dan objek user dari Firebase Auth tidak null
    // Safety check wajib sebelum mengakses data user untuk menghindari crash aplikasi

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        createDefaultProfileIfNeeded(user.uid, user.email ?: "")
                    }
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }


    // Membuat profil default untuk user baru yang belum memiliki data di Firestore
    // Mengisi data minimal seperti nama dari email dan field kosong untuk foto dan bio

    private fun createDefaultProfileIfNeeded(userId: String, email: String) {
        val userRef = Firebase.firestore.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val defaultProfile = UserProfile(
                    userId = userId,
                    email = email,
                    name = email.substringBefore("@"),  // Use email prefix as default name
                    profilePicUrl = "",
                    bio = ""
                )
                userRef.set(defaultProfile)
            }
        }
    }
} 