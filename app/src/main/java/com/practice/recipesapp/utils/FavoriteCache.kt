object FavoriteCache {
    private val favoriteCache = mutableMapOf<String, Boolean>()
    
    fun isFavorite(recipeId: String): Boolean? = favoriteCache[recipeId]
    
    fun setFavorite(recipeId: String, isFavorite: Boolean) {
        favoriteCache[recipeId] = isFavorite
    }
    
    fun clear() {
        favoriteCache.clear()
    }
} 