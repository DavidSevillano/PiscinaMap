package com.burixer85.piscinamap.util

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.domain.model.Review

object TestData {
    val review = Review(
        authorName = "Ana García",
        rating = 4.5f,
        text = "Piscina muy limpia y bien mantenida. El personal es amable.",
        relativeTimeDescription = "hace 2 semanas",
        publishedAt = 1700000000L,
        language = "es",
        profilePhotoUrl = null,
    )

    val pool = Pool(
        id = "test_pool_id_1",
        name = "Piscina Municipal Centro",
        latitude = 40.4168,
        longitude = -3.7038,
        address = "Calle Mayor 1, Madrid",
        rating = 4.2f,
        isOpenNow = true,
        photoUrls = emptyList(),
        formattedPhone = "+34 91 123 45 67",
        openingHours = listOf("Lunes: 9:00 – 21:00", "Martes: 9:00 – 21:00"),
        currentOpeningHours = "Hoy: 9:00 – 21:00",
        services = listOf("Swimming pool", "Changing room"),
        reviews = listOf(review),
        isHidden = false,
        isNew = false,
    )

    val poolList = listOf(
        pool,
        pool.copy(id = "test_pool_id_2", name = "Piscina Olímpica Norte"),
        pool.copy(id = "test_pool_id_3", name = "Club Deportivo Sur"),
    )
}
