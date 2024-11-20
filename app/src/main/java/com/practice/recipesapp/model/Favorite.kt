package com.practice.recipesapp.model

data class Favorite(
    val userId: String = "",
    val recipeId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) 