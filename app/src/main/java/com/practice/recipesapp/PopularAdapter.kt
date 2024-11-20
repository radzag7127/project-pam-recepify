package com.practice.recipesapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.practice.recipesapp.databinding.PopularRvItemBinding
import com.practice.recipesapp.model.Favorite

class PopularAdapter(var dataList: ArrayList<Recipe>, var context: Context) :
    RecyclerView.Adapter<PopularAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val documentIds = mutableMapOf<Recipe, String>()

    inner class ViewHolder(var binding: PopularRvItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var binding = PopularRvItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = dataList[position]

        Glide.with(context).load(recipe.img).into(holder.binding.popularImg)
        holder.binding.popularTxt.text = recipe.tittle

        var time = recipe.ing.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()

        holder.binding.popularTime.text = time.get(0)
        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecipeActivity::class.java).apply {
                putExtra("DOCUMENT_ID", documentIds[recipe])
                putExtra("img", recipe.img)
                putExtra("title", recipe.tittle)
                putExtra("des", recipe.des)
                putExtra("ing", recipe.ing)
            }
            context.startActivity(intent)
        }

    }

    fun setDocumentId(recipe: Recipe, documentId: String) {
        documentIds[recipe] = documentId
    }

}