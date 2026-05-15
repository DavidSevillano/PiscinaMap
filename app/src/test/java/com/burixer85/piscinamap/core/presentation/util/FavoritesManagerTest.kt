package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavoritesManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val backingSet = mutableSetOf<String>()
    private val capturedSet = slot<Set<String>>()

    @Before
    fun setUp() {
        backingSet.clear()
        editor = mockk(relaxed = true)
        prefs = mockk()
        context = mockk()

        every { context.getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getStringSet("favorite_pool_ids", any()) } answers { backingSet.toSet() }
        every { prefs.edit() } returns editor
        every { editor.putStringSet("favorite_pool_ids", capture(capturedSet)) } answers {
            backingSet.clear()
            backingSet.addAll(capturedSet.captured)
            editor
        }
    }

    @Test
    fun `isFavorite returns false when set is empty`() {
        assertFalse(FavoritesManager.isFavorite(context, "pool1"))
    }

    @Test
    fun `addFavorite makes isFavorite return true`() {
        FavoritesManager.addFavorite(context, "pool1")
        assertTrue(FavoritesManager.isFavorite(context, "pool1"))
    }

    @Test
    fun `removeFavorite makes isFavorite return false`() {
        backingSet.add("pool1")
        FavoritesManager.removeFavorite(context, "pool1")
        assertFalse(FavoritesManager.isFavorite(context, "pool1"))
    }

    @Test
    fun `getFavoriteIds returns all added ids`() {
        backingSet.addAll(listOf("pool1", "pool2"))
        val ids = FavoritesManager.getFavoriteIds(context)
        assertEquals(setOf("pool1", "pool2"), ids)
    }

    @Test
    fun `addFavorite does not duplicate existing id`() {
        FavoritesManager.addFavorite(context, "pool1")
        FavoritesManager.addFavorite(context, "pool1")
        assertEquals(1, FavoritesManager.getFavoriteIds(context).size)
    }

    @Test
    fun `removeFavorite on absent id leaves set unchanged`() {
        backingSet.add("pool2")
        FavoritesManager.removeFavorite(context, "pool99")
        assertEquals(setOf("pool2"), FavoritesManager.getFavoriteIds(context))
    }
}
