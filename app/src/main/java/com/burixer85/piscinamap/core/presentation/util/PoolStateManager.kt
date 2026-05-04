package com.burixer85.piscinamap.core.presentation.util

object PoolStateManager {
    private val listeners = mutableListOf<(String, Boolean) -> Unit>()

    fun subscribe(listener: (String, Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (String, Boolean) -> Unit) {
        listeners.remove(listener)
    }

    fun emitHiddenStateChange(poolId: String, isHidden: Boolean) {
        listeners.forEach { it(poolId, isHidden) }
    }
}