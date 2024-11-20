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

class RecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeBinding
    private var imgCrop = true

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}