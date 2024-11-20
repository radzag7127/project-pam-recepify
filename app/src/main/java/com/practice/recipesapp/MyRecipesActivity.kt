package com.practice.recipesapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.practice.recipesapp.databinding.ActivityMyRecipesBinding
import com.practice.recipesapp.model.CloudRecipe

class MyRecipesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyRecipesBinding
    private lateinit var rvAdapter: MyRecipesAdapter
    private lateinit var dataList: ArrayList<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRecipesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadMyRecipes()

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        dataList = ArrayList()
        binding.rvMyRecipes.layoutManager = LinearLayoutManager(this)
        rvAdapter = MyRecipesAdapter(dataList, this) { recipe, documentId ->
            // Launch UploadRecipeActivity in edit mode
            val intent = Intent(this, UploadRecipeActivity::class.java).apply {
                putExtra("EDIT_MODE", true)
                putExtra("DOCUMENT_ID", documentId)
                putExtra("RECIPE_TITLE", recipe.tittle)
                putExtra("RECIPE_DESCRIPTION", recipe.des)
                putExtra("RECIPE_CATEGORY", recipe.category)
                putExtra("RECIPE_IMAGE", recipe.img)
                putExtra("RECIPE_INGREDIENTS", recipe.ing)
            }
            startActivity(intent)
        }
        binding.rvMyRecipes.adapter = rvAdapter
    }

    private fun loadMyRecipes() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.firestore.collection("recipes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                dataList.clear()
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
                    (rvAdapter as MyRecipesAdapter).setDocumentId(recipe, document.id)
                }
                rvAdapter.notifyDataSetChanged()
            }
    }
} 