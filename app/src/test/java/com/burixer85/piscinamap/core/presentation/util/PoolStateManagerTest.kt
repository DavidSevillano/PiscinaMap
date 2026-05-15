package com.burixer85.piscinamap.core.presentation.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoolStateManagerTest {

    private val registeredListeners = mutableListOf<(String, Boolean) -> Unit>()
    private val registeredFavoriteListeners = mutableListOf<(String, Boolean) -> Unit>()

    @After
    fun tearDown() {
        registeredListeners.forEach { PoolStateManager.unsubscribe(it) }
        registeredListeners.clear()
        registeredFavoriteListeners.forEach { PoolStateManager.unsubscribeFavorite(it) }
        registeredFavoriteListeners.clear()
    }

    private fun subscribe(listener: (String, Boolean) -> Unit) {
        PoolStateManager.subscribe(listener)
        registeredListeners.add(listener)
    }

    private fun subscribeFavorite(listener: (String, Boolean) -> Unit) {
        PoolStateManager.subscribeFavorite(listener)
        registeredFavoriteListeners.add(listener)
    }

    @Test
    fun `subscribed listener receives emitted hidden state change`() {
        val received = mutableListOf<Pair<String, Boolean>>()
        subscribe { poolId, isHidden -> received.add(poolId to isHidden) }

        PoolStateManager.emitHiddenStateChange("pool123", true)

        assertEquals(1, received.size)
        assertEquals("pool123", received[0].first)
        assertEquals(true, received[0].second)
    }

    @Test
    fun `subscribed listener receives show pool event`() {
        val received = mutableListOf<Pair<String, Boolean>>()
        subscribe { poolId, isHidden -> received.add(poolId to isHidden) }

        PoolStateManager.emitHiddenStateChange("pool456", false)

        assertEquals(1, received.size)
        assertEquals("pool456", received[0].first)
        assertEquals(false, received[0].second)
    }

    @Test
    fun `multiple listeners are all notified on emit`() {
        val calls1 = mutableListOf<String>()
        val calls2 = mutableListOf<String>()
        subscribe { poolId, _ -> calls1.add(poolId) }
        subscribe { poolId, _ -> calls2.add(poolId) }

        PoolStateManager.emitHiddenStateChange("poolABC", true)

        assertEquals(listOf("poolABC"), calls1)
        assertEquals(listOf("poolABC"), calls2)
    }

    @Test
    fun `unsubscribed listener does not receive events`() {
        val received = mutableListOf<String>()
        val listener: (String, Boolean) -> Unit = { poolId, _ -> received.add(poolId) }

        PoolStateManager.subscribe(listener)
        PoolStateManager.unsubscribe(listener)

        PoolStateManager.emitHiddenStateChange("pool789", true)

        assertTrue(received.isEmpty())
    }

    @Test
    fun `only unsubscribed listener stops receiving while others continue`() {
        val calls1 = mutableListOf<String>()
        val calls2 = mutableListOf<String>()

        val listener1: (String, Boolean) -> Unit = { poolId, _ -> calls1.add(poolId) }
        val listener2: (String, Boolean) -> Unit = { poolId, _ -> calls2.add(poolId) }

        PoolStateManager.subscribe(listener1)
        PoolStateManager.subscribe(listener2)
        registeredListeners.add(listener2)

        PoolStateManager.unsubscribe(listener1)

        PoolStateManager.emitHiddenStateChange("poolXYZ", false)

        assertTrue(calls1.isEmpty())
        assertEquals(listOf("poolXYZ"), calls2)
    }

    @Test
    fun `listener receives multiple sequential events correctly`() {
        val received = mutableListOf<Pair<String, Boolean>>()
        subscribe { poolId, isHidden -> received.add(poolId to isHidden) }

        PoolStateManager.emitHiddenStateChange("p1", true)
        PoolStateManager.emitHiddenStateChange("p2", false)
        PoolStateManager.emitHiddenStateChange("p1", false)

        assertEquals(3, received.size)
        assertEquals("p1" to true, received[0])
        assertEquals("p2" to false, received[1])
        assertEquals("p1" to false, received[2])
    }

    @Test
    fun `subscribed favorite listener receives emitted favorite state change`() {
        val received = mutableListOf<Pair<String, Boolean>>()
        subscribeFavorite { poolId, isFav -> received.add(poolId to isFav) }

        PoolStateManager.emitFavoriteStateChange("pool1", true)

        assertEquals(1, received.size)
        assertEquals("pool1" to true, received[0])
    }

    @Test
    fun `unsubscribed favorite listener does not receive events`() {
        val received = mutableListOf<String>()
        val listener: (String, Boolean) -> Unit = { poolId, _ -> received.add(poolId) }

        PoolStateManager.subscribeFavorite(listener)
        PoolStateManager.unsubscribeFavorite(listener)
        PoolStateManager.emitFavoriteStateChange("pool1", true)

        assertTrue(received.isEmpty())
    }

    @Test
    fun `favorite listeners do not receive hidden state events`() {
        val favReceived = mutableListOf<String>()
        subscribeFavorite { poolId, _ -> favReceived.add(poolId) }

        PoolStateManager.emitHiddenStateChange("pool1", true)

        assertTrue(favReceived.isEmpty())
    }

    @Test
    fun `hidden listeners do not receive favorite state events`() {
        val hiddenReceived = mutableListOf<String>()
        subscribe { poolId, _ -> hiddenReceived.add(poolId) }

        PoolStateManager.emitFavoriteStateChange("pool1", true)

        assertTrue(hiddenReceived.isEmpty())
    }
}
