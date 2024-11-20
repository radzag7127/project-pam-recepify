package com.practice.recipesapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.practice.recipesapp.databinding.ActivityFavoritesBinding
import com.practice.recipesapp.model.CloudRecipe

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var rvAdapter: PopularAdapter
    private lateinit var dataList: ArrayList<Recipe>
    private lateinit var allRecipes: ArrayList<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadFavorites()

        setupCategoryFilters()

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        dataList = ArrayList()
        binding.rvFavorites.layoutManager = LinearLayoutManager(this)
        rvAdapter = PopularAdapter(dataList, this)
        binding.rvFavorites.adapter = rvAdapter
    }

    private fun setupCategoryFilters() {
        binding.categoryChipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipAll -> filterRecipes(null)
                R.id.chipBreakfast -> filterRecipes("Breakfast")
                R.id.chipLunch -> filterRecipes("Lunch")
                R.id.chipDinner -> filterRecipes("Dinner")
                R.id.chipDesserts -> filterRecipes("Dessert")
            }
        }
    }

    private fun filterRecipes(category: String?) {
        dataList.clear()
        if (category == null) {
            dataList.addAll(allRecipes)
        } else {
            dataList.addAll(allRecipes.filter { it.category == category })
        }
        rvAdapter.notifyDataSetChanged()
    }

    private fun loadFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Loading
        binding.progressBar.visibility = View.VISIBLE

        Firebase.firestore.collection("favorites")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val recipeIds = documents.mapNotNull { it.getString("recipeId") }
                if (recipeIds.isEmpty()) {
                    dataList.clear()
                    allRecipes = ArrayList()
                    rvAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // Get all recipes
                Firebase.firestore.collection("recipes")
                    .whereIn(FieldPath.documentId(), recipeIds)
                    .get()
                    .addOnSuccessListener { recipeDocuments ->
                        dataList.clear()
                        allRecipes = ArrayList()

                        for (document in recipeDocuments) {
                            val cloudRecipe = document.toObject(CloudRecipe::class.java)
                            cloudRecipe?.let {
                                val recipe = Recipe(
                                    img = it.img,
                                    tittle = it.title,
                                    des = it.description,
                                    ing = "${it.duration}\n${it.ingredients.joinToString("\n")}",
                                    category = it.category
                                )
                                dataList.add(recipe)
                                allRecipes.add(recipe)
                                rvAdapter.setDocumentId(recipe, document.id)
                            }
                        }
                        rvAdapter.notifyDataSetChanged()
                        binding.progressBar.visibility = View.GONE
                    }
            }
    }
} 