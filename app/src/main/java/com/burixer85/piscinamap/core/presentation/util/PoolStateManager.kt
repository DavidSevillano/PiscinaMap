package com.burixer85.piscinamap.core.presentation.util

import android.util.Log

object PoolStateManager {
    private const val TAG = "PoolStateManager"

    private val listeners = mutableListOf<(String, Boolean) -> Unit>()

    fun subscribe(listener: (String, Boolean) -> Unit) {
        Log.d(TAG, "Listener subscribed: $listener")
        listeners.add(listener)
    }

    fun unsubscribe(listener: (String, Boolean) -> Unit) {
        Log.d(TAG, "Listener unsubscribed: $listener")
        listeners.remove(listener)
    }

    fun emitHiddenStateChange(poolId: String, isHidden: Boolean) {
        Log.d(TAG, "emitHiddenStateChange: poolId=$poolId, isHidden=$isHidden, listeners=${listeners.size}")
        listeners.forEach { it(poolId, isHidden) }
    }
}