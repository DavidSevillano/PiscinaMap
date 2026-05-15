package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {
    private const val PREFS_NAME = "favorite_pools"
    private const val KEY_FAVORITE_IDS = "favorite_pool_ids"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFavorite(context: Context, poolId: String): Boolean {
        val ids = getPrefs(context).getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
        return poolId in ids
    }

    fun addFavorite(context: Context, poolId: String) {
        val prefs = getPrefs(context)
        val ids = prefs.getStringSet(KEY_FAVORITE_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.add(poolId)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, ids).apply()
    }

    fun removeFavorite(context: Context, poolId: String) {
        val prefs = getPrefs(context)
        val ids = prefs.getStringSet(KEY_FAVORITE_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.remove(poolId)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, ids).apply()
    }

    fun getFavoriteIds(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
}
