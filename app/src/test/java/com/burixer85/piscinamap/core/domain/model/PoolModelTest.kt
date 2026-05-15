package com.burixer85.piscinamap.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PoolModelTest {

    private fun makePool(
        photoUrls: List<String> = emptyList(),
        isHidden: Boolean = false,
        isNew: Boolean = false
    ) = Pool(
        id = "1",
        name = "Pool Test",
        latitude = 40.0,
        longitude = -3.0,
        address = "Calle Test",
        rating = null,
        isOpenNow = null,
        photoUrls = photoUrls,
        isHidden = isHidden,
        isNew = isNew
    )

    @Test
    fun `photoUrl returns first url when list has entries`() {
        val pool = makePool(photoUrls = listOf("url1", "url2", "url3"))
        assertEquals("url1", pool.photoUrl)
    }

    @Test
    fun `photoUrl returns null when photoUrls is empty`() {
        val pool = makePool(photoUrls = emptyList())
        assertNull(pool.photoUrl)
    }

    @Test
    fun `isHidden defaults to false`() {
        val pool = makePool()
        assertFalse(pool.isHidden)
    }

    @Test
    fun `isNew defaults to false`() {
        val pool = makePool()
        assertFalse(pool.isNew)
    }

    @Test
    fun `copy preserves isHidden flag`() {
        val pool = makePool(isHidden = true)
        val copy = pool.copy(name = "New Name")
        assertEquals(true, copy.isHidden)
    }

    @Test
    fun `openingHours defaults to empty list`() {
        val pool = makePool()
        assertEquals(emptyList<String>(), pool.openingHours)
    }

    @Test
    fun `reviews defaults to empty list`() {
        val pool = makePool()
        assertEquals(emptyList<Review>(), pool.reviews)
    }
}
