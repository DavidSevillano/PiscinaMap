package com.burixer85.piscinamap.core.presentation.util

object PoolStateManager {
    private val listeners = mutableListOf<(String, Boolean) -> Unit>()
    private val favoriteListeners = mutableListOf<(String, Boolean) -> Unit>()

    fun subscribe(listener: (String, Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (String, Boolean) -> Unit) {
        listeners.remove(listener)
    }

    fun emitHiddenStateChange(poolId: String, isHidden: Boolean) {
        listeners.forEach { it(poolId, isHidden) }
    }

    fun subscribeFavorite(listener: (String, Boolean) -> Unit) {
        favoriteListeners.add(listener)
    }

    fun unsubscribeFavorite(listener: (String, Boolean) -> Unit) {
        favoriteListeners.remove(listener)
    }

    fun emitFavoriteStateChange(poolId: String, isFavorite: Boolean) {
        favoriteListeners.forEach { it(poolId, isFavorite) }
    }
}