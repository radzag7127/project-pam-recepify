package com.practice.recipesapp.models

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String = "",
    val bio: String = ""
) 