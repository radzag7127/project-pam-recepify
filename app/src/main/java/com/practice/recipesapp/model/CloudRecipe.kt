package com.practice.recipesapp.model

data class CloudRecipe(
    val img: String = "",
    val title: String = "",
    val duration: String = "",
    val description: String = "",
    val ingredients: List<String> = listOf(),
    val category: String = "",
    val userId: String = ""
) 