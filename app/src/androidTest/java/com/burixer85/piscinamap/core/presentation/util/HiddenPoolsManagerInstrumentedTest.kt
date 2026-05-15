package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiddenPoolsManagerInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clean slate before each test
        context.getSharedPreferences("hidden_pools", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("hidden_pools", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `isHidden returns false for pool not in hidden set`() {
        assertFalse(HiddenPoolsManager.isHidden(context, "pool_new"))
    }

    @Test
    fun `hidePool persists pool id and isHidden returns true`() {
        HiddenPoolsManager.hidePool(context, "pool_A")

        assertTrue(HiddenPoolsManager.isHidden(context, "pool_A"))
    }

    @Test
    fun `hidePool does not affect other pools`() {
        HiddenPoolsManager.hidePool(context, "pool_A")

        assertFalse(HiddenPoolsManager.isHidden(context, "pool_B"))
    }

    @Test
    fun `showPool removes pool from hidden set`() {
        HiddenPoolsManager.hidePool(context, "pool_A")
        HiddenPoolsManager.showPool(context, "pool_A")

        assertFalse(HiddenPoolsManager.isHidden(context, "pool_A"))
    }

    @Test
    fun `showPool on non-hidden pool does not throw`() {
        HiddenPoolsManager.showPool(context, "pool_not_hidden")

        assertFalse(HiddenPoolsManager.isHidden(context, "pool_not_hidden"))
    }

    @Test
    fun `getHiddenIds returns all hidden pool ids`() {
        HiddenPoolsManager.hidePool(context, "pool_1")
        HiddenPoolsManager.hidePool(context, "pool_2")
        HiddenPoolsManager.hidePool(context, "pool_3")

        val hiddenIds = HiddenPoolsManager.getHiddenIds(context)

        assertEquals(setOf("pool_1", "pool_2", "pool_3"), hiddenIds)
    }

    @Test
    fun `getHiddenIds returns empty set when no pools hidden`() {
        assertTrue(HiddenPoolsManager.getHiddenIds(context).isEmpty())
    }

    @Test
    fun `hidePool is idempotent for same pool id`() {
        HiddenPoolsManager.hidePool(context, "pool_A")
        HiddenPoolsManager.hidePool(context, "pool_A")

        assertEquals(1, HiddenPoolsManager.getHiddenIds(context).size)
        assertTrue(HiddenPoolsManager.isHidden(context, "pool_A"))
    }

    @Test
    fun `hidden state persists across multiple hide and show cycles`() {
        HiddenPoolsManager.hidePool(context, "pool_A")
        HiddenPoolsManager.showPool(context, "pool_A")
        HiddenPoolsManager.hidePool(context, "pool_A")

        assertTrue(HiddenPoolsManager.isHidden(context, "pool_A"))
    }

    @Test
    fun `showPool removes only the specified pool from hidden set`() {
        HiddenPoolsManager.hidePool(context, "pool_1")
        HiddenPoolsManager.hidePool(context, "pool_2")

        HiddenPoolsManager.showPool(context, "pool_1")

        assertFalse(HiddenPoolsManager.isHidden(context, "pool_1"))
        assertTrue(HiddenPoolsManager.isHidden(context, "pool_2"))
    }
}
