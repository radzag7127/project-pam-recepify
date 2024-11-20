package com.practice.recipesapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.practice.recipesapp.databinding.ActivitySearchBinding
import com.practice.recipesapp.model.CloudRecipe

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var rvAdapter: SearchAdapter
    private lateinit var dataList: ArrayList<Recipe>

    @SuppressLint("ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.search.requestFocus()

        setupRecyclerView()
        loadFirestoreRecipes()

        binding.goBackHome.setOnClickListener {
            finish()
        }

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    filterData(s.toString())
                } else {
                    // When search is empty, show all recipes
                    rvAdapter.filerList(dataList)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterData(filterText: String) {
        val filterData = ArrayList<Recipe>()
        for (recipe in dataList) {
            if (recipe.tittle.lowercase().contains(filterText.lowercase())) {
                filterData.add(recipe)
            }
        }
        rvAdapter.filerList(filterData)
    }

    private fun setupRecyclerView() {
        dataList = ArrayList()
        binding.rvSearch.layoutManager = LinearLayoutManager(this)
        rvAdapter = SearchAdapter(dataList, this)
        binding.rvSearch.adapter = rvAdapter
    }

    private fun loadFirestoreRecipes() {
        Firebase.firestore.collection("recipes")
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
                }
                rvAdapter.notifyDataSetChanged()
            }
    }
}