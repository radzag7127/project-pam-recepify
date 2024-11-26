package com.practice.recipesapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.practice.recipesapp.databinding.ActivityCategoryBinding
import com.practice.recipesapp.model.CloudRecipe

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding 
    private lateinit var rvAdapter: PopularAdapter
    private lateinit var dataList: ArrayList<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category = intent.getStringExtra("CATEGORY") ?: return
        binding.tittle.text = category

        setupRecyclerView()
        loadCategoryRecipes(category)

        binding.goBackHome.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        dataList = ArrayList()
        binding.rvCategory.layoutManager = LinearLayoutManager(this)
        rvAdapter = PopularAdapter(dataList, this)
        binding.rvCategory.adapter = rvAdapter
    }

    private fun loadCategoryRecipes(category: String) {
        // Show loading state
        binding.progressBar?.visibility = View.VISIBLE

        Firebase.firestore.collection("recipes")
            .whereEqualTo("category", category)
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
                    rvAdapter.setDocumentId(recipe, document.id)
                }
                rvAdapter.notifyDataSetChanged()
                binding.progressBar?.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(this, "Error loading recipes: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}