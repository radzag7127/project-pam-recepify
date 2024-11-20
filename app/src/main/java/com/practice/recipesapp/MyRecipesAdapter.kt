package com.practice.recipesapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.practice.recipesapp.databinding.RecipeItemBinding

class MyRecipesAdapter(
    private val recipes: ArrayList<Recipe>,
    private val context: Context,
    private val onEditClick: (Recipe, String) -> Unit
) : RecyclerView.Adapter<MyRecipesAdapter.ViewHolder>() {

    private var documentIds = mutableMapOf<Recipe, String>()

    fun setDocumentId(recipe: Recipe, documentId: String) {
        documentIds[recipe] = documentId
    }

    inner class ViewHolder(val binding: RecipeItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecipeItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.binding.apply {
            recipeTitle.text = recipe.tittle
            recipeCategory.text = recipe.category
            Glide.with(context).load(recipe.img).into(recipeImage)

            root.setOnClickListener {
                val intent = Intent(context, RecipeActivity::class.java).apply {
                    putExtra("img", recipe.img)
                    putExtra("tittle", recipe.tittle)
                    putExtra("des", recipe.des)
                    putExtra("ing", recipe.ing)
                }
                context.startActivity(intent)
            }

            editButton.setOnClickListener {
                documentIds[recipe]?.let { docId ->
                    onEditClick(recipe, docId)
                }
            }
        }
    }

    override fun getItemCount(): Int = recipes.size
} 