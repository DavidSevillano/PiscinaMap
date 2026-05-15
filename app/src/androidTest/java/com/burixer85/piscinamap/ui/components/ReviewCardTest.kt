package com.burixer85.piscinamap.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.burixer85.piscinamap.core.presentation.components.ReviewCard
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class ReviewCardTest {

    @get:Rule val rule = createComposeRule()

    private fun setReviewCard(
        authorName: String = TestData.review.authorName,
        rating: Float = TestData.review.rating,
        text: String = TestData.review.text,
        relativeTime: String = TestData.review.relativeTimeDescription,
    ) {
        rule.setContent {
            PiscinaMapTheme {
                ReviewCard(
                    authorName = authorName,
                    rating = rating,
                    text = text,
                    relativeTime = relativeTime,
                )
            }
        }
    }

    @Test
    fun showsAuthorName() {
        setReviewCard()
        rule.onNodeWithText("Ana García").assertIsDisplayed()
    }

    @Test
    fun showsRating() {
        setReviewCard()
        rule.onNodeWithText("4.5").assertIsDisplayed()
    }

    @Test
    fun showsReviewText() {
        setReviewCard(text = "Piscina muy limpia.")
        rule.onNodeWithText("Piscina muy limpia.").assertIsDisplayed()
    }

    @Test
    fun showsRelativeTime() {
        setReviewCard()
        rule.onNodeWithText("hace 2 semanas").assertIsDisplayed()
    }
}
