package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.content.SharedPreferences

object HiddenPoolsManager {
    private const val PREFS_NAME = "hidden_pools"
    private const val KEY_HIDDEN_IDS = "hidden_pool_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isHidden(context: Context, poolId: String): Boolean {
        val prefs = getPrefs(context)
        val hiddenIds = prefs.getStringSet(KEY_HIDDEN_IDS, emptySet()) ?: emptySet()
        return poolId in hiddenIds
    }

    fun hidePool(context: Context, poolId: String) {
        val prefs = getPrefs(context)
        val hiddenIds = prefs.getStringSet(KEY_HIDDEN_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        hiddenIds.add(poolId)
        prefs.edit().putStringSet(KEY_HIDDEN_IDS, hiddenIds).apply()
    }

    fun showPool(context: Context, poolId: String) {
        val prefs = getPrefs(context)
        val hiddenIds = prefs.getStringSet(KEY_HIDDEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        hiddenIds.remove(poolId)
        prefs.edit().putStringSet(KEY_HIDDEN_IDS, hiddenIds).apply()
    }

    fun getHiddenIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_HIDDEN_IDS, emptySet()) ?: emptySet()
    }
}