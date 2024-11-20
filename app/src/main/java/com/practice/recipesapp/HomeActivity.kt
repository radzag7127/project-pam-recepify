package com.practice.recipesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.practice.recipesapp.databinding.ActivityHomeBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.practice.recipesapp.model.CloudRecipe
import android.util.Log
import android.widget.Toast
import android.view.View

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var rvAdapter: PopularAdapter
    private lateinit var dataList: ArrayList<Recipe>
    private lateinit var allRecipes: ArrayList<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupCategoryClickListeners()
        loadAllRecipes()

        binding.favoritesButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        binding.search.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // Add logout button to menu
        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }

        binding.addRecipeButton.setOnClickListener {
            startActivity(Intent(this, UploadRecipeActivity::class.java))
        }

        binding.profileButton.setOnClickListener {
            try {
                startActivity(Intent(this, ProfileActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening profile: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HomeActivity", "Error opening profile", e)
            }
        }
    }

    private fun setupRecyclerView() {
        dataList = ArrayList()
        binding.rvPopular.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAdapter = PopularAdapter(dataList, this)
        binding.rvPopular.adapter = rvAdapter
    }

    private fun setupCategoryClickListeners() {
        binding.apply {
            breakfast.setOnClickListener { navigateToCategory("Breakfast") }
            lunch.setOnClickListener { navigateToCategory("Lunch") }
            dinner.setOnClickListener { navigateToCategory("Dinner") }
            desserts.setOnClickListener { navigateToCategory("Dessert") }
        }
    }

    private fun navigateToCategory(category: String) {
        startActivity(Intent(this, CategoryActivity::class.java).apply {
            putExtra("CATEGORY", category)
            putExtra("TITLE", category)
        })
    }

    private fun loadAllRecipes() {
        binding.progressBar.visibility = View.VISIBLE

        Firebase.firestore.collection("recipes")
            .get()
            .addOnSuccessListener { documents ->
                dataList.clear()
                allRecipes = ArrayList()

                for (document in documents) {
                    val cloudRecipe = document.toObject(CloudRecipe::class.java)
                    val recipe = Recipe(
                        img = cloudRecipe.img,
                        tittle = cloudRecipe.title,
                        des = cloudRecipe.description,
                        ing = "${cloudRecipe.duration}\n${cloudRecipe.ingredients.joinToString("\n")}",
                        category = cloudRecipe.category
                    )
                    dataList.add(recipe)
                    allRecipes.add(recipe)
                    (rvAdapter as PopularAdapter).setDocumentId(recipe, document.id)
                }
                rvAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading recipes: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}