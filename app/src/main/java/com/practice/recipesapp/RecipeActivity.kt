package com.practice.recipesapp

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.practice.recipesapp.databinding.ActivityRecipeBinding
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.practice.recipesapp.model.Favorite
import com.google.firebase.firestore.QuerySnapshot
import android.widget.Toast
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.Manifest
import android.os.Environment
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase

class RecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeBinding
    private var imgCrop = true
    private lateinit var storage: FirebaseStorage

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Storage
        storage = Firebase.storage
        
        setupFavoriteButton()

        binding.addToFavoriteButton.setOnClickListener {
            toggleFavorite()
        }

        loadImage(intent.getStringExtra("img"))
        binding.tittle.text = intent.getStringExtra("title") ?: intent.getStringExtra("tittle") ?: "No Title"
        binding.stepData.text = intent.getStringExtra("des")?.split("\n")
            ?.joinToString("\n") { "• $it" }
            ?.trim() ?: "No Description"

        val allLines = intent.getStringExtra("ing")?.split("\n".toRegex())?.dropLastWhile { it.isEmpty() }
            ?.toTypedArray()
        
        binding.time.text = allLines?.getOrNull(0) ?: "N/A"

        binding.ingData.text = ""
        binding.ingData.text = allLines?.drop(1)
            ?.joinToString("\n") { "🟢 $it" }
            ?.trim()
        
        binding.step.background = null
        binding.step.setTextColor(getColor(R.color.black))
        binding.step.setOnClickListener {
            binding.step.setBackgroundResource(R.drawable.btn_ing)
            binding.step.setTextColor(getColor(R.color.white))
            binding.ing.setTextColor(getColor(R.color.black))
            binding.ing.background = null
            binding.stepScroll.visibility = View.VISIBLE
            binding.ingScroll.visibility = View.GONE
        }

        binding.ing.setOnClickListener {
            binding.ing.setBackgroundResource(R.drawable.btn_ing)
            binding.ing.setTextColor(getColor(R.color.white))
            binding.step.setTextColor(getColor(R.color.black))
            binding.step.background = null
            binding.ingScroll.visibility = View.VISIBLE
            binding.stepScroll.visibility = View.GONE
        }

        binding.fullScreen.setOnClickListener {
            if (imgCrop) {
                binding.itemImg.scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(this).load(intent.getStringExtra("img")).into(binding.itemImg)
                binding.fullScreen.setColorFilter(Color.BLACK)
                binding.shade.visibility = View.GONE
                imgCrop = !imgCrop
            } else {
                binding.itemImg.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this).load(intent.getStringExtra("img")).into(binding.itemImg)
                binding.fullScreen.setColorFilter(null)
                binding.shade.visibility = View.VISIBLE
                imgCrop = !imgCrop
            }
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        setupDownloadButton()
    }

    private fun loadImage(imagePath: String?) {
        try {
            when {
                imagePath == null -> {
                    binding.itemImg.setImageResource(R.drawable.placeholder_image)
                }
                imagePath.startsWith("/data/") -> {
                    val file = File(imagePath)
                    if (file.exists()) {
                        Glide.with(this)
                            .load(file)
                            .error(R.drawable.placeholder_image)
                            .into(binding.itemImg)
                    } else {
                        binding.itemImg.setImageResource(R.drawable.placeholder_image)
                        Log.e("RecipeActivity", "Local file not found: $imagePath")
                    }
                }
                else -> {
                    Glide.with(this)
                        .load(imagePath)
                        .error(R.drawable.placeholder_image)
                        .into(binding.itemImg)
                }
            }
        } catch (e: Exception) {
            Log.e("RecipeActivity", "Error loading image", e)
            binding.itemImg.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun setupFavoriteButton() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val recipeId = intent.getStringExtra("DOCUMENT_ID") ?: return
        
        // Check cache first
        FavoriteCache.isFavorite(recipeId)?.let {
            updateFavoriteButtonState(it)
            return
        }
        
        binding.addToFavoriteButton.isEnabled = false
        binding.favoriteProgressBar.visibility = View.VISIBLE
        
        FirebaseFirestore.getInstance().collection("favorites")
            .whereEqualTo("userId", userId)
            .whereEqualTo("recipeId", recipeId)
            .get()
            .addOnSuccessListener { documents ->
                val isFavorited = !documents.isEmpty
                FavoriteCache.setFavorite(recipeId, isFavorited)
                updateFavoriteButtonState(isFavorited)
                binding.favoriteProgressBar.visibility = View.GONE
                binding.addToFavoriteButton.isEnabled = true
                
                // Set click listener after we know the state
                binding.addToFavoriteButton.setOnClickListener {
                    toggleFavorite()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking favorite status: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.favoriteProgressBar.visibility = View.GONE
                binding.addToFavoriteButton.isEnabled = true
            }
    }

    private fun toggleFavorite() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val recipeId = intent.getStringExtra("DOCUMENT_ID") ?: return
        
        binding.addToFavoriteButton.isEnabled = false
        binding.favoriteProgressBar.visibility = View.VISIBLE
        
        FirebaseFirestore.getInstance().collection("favorites")
            .whereEqualTo("userId", userId)
            .whereEqualTo("recipeId", recipeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    addToFavorites(userId, recipeId)
                } else {
                    removeFromFavorites(documents, recipeId)
                }
            }
            .addOnFailureListener { e ->
                handleFavoriteError(e)
            }
    }

    private fun addToFavorites(userId: String, recipeId: String) {
        val favorite = Favorite(userId, recipeId)
        FirebaseFirestore.getInstance().collection("favorites")
            .add(favorite)
            .addOnSuccessListener {
                FavoriteCache.setFavorite(recipeId, true)
                updateFavoriteButtonState(true)
                binding.favoriteProgressBar.visibility = View.GONE
                binding.addToFavoriteButton.isEnabled = true
            }
            .addOnFailureListener { e ->
                handleFavoriteError(e)
            }
    }

    private fun removeFromFavorites(documents: QuerySnapshot, recipeId: String) {
        documents.forEach { document ->
            document.reference.delete()
                .addOnSuccessListener {
                    FavoriteCache.setFavorite(recipeId, false)
                    updateFavoriteButtonState(false)
                    binding.favoriteProgressBar.visibility = View.GONE
                    binding.addToFavoriteButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    handleFavoriteError(e)
                }
        }
    }

    private fun handleFavoriteError(e: Exception) {
        Toast.makeText(this, "Error updating favorite: ${e.message}", Toast.LENGTH_SHORT).show()
        binding.favoriteProgressBar.visibility = View.GONE
        binding.addToFavoriteButton.isEnabled = true
    }

    private fun updateFavoriteButtonState(isFavorited: Boolean) {
        binding.addToFavoriteButton.apply {
            setCompoundDrawablesWithIntrinsicBounds(
                if (isFavorited) R.drawable.ic_favorite_filled 
                else R.drawable.ic_favorite_border, 
                0, 0, 0
            )
            text = if (isFavorited) "Remove from Favorite" else "Add to Favorite"
        }
    }

    private fun downloadImage() {
        val imageUrl = intent.getStringExtra("img")
        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "No image to download", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Downloading image...")
            setCancelable(false)
            show()
        }

        try {
            // Get reference from URL
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            
            // Create directory if it doesn't exist
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RecipesApp")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Create local file
            val fileName = "recipe_${System.currentTimeMillis()}.jpg"
            val localFile = File(directory, fileName)

            // Download directly to local file
            storageRef.getFile(localFile)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Image saved to Downloads/RecipesApp", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to download image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDownloadButton() {
        binding.downloadButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 and above
                if (Environment.isExternalStorageManager()) {
                    downloadImage()
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.parse("package:${applicationContext.packageName}")
                        startActivityForResult(intent, 2001)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, 2001)
                    }
                }
            } else {
                // Android 10 and below
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    downloadImage()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadImage()
            } else {
                Toast.makeText(this, "Storage permission required to download images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    downloadImage()
                } else {
                    Toast.makeText(this, "Storage permission required to download images", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}