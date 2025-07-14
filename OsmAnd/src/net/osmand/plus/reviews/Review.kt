package net.osmand.plus.reviews

import java.time.LocalDate

/**
 * A POI review.
 * @property rating the review rating, a number between 1 and 100, inclusive
 * @property opinion optional opinion text, might be in any language
 * @property date the date when the review was last edited
 * @property author the review author's nickname
 */
data class Review(
    val rating: Int,
    val opinion: String?,
    val author: String,
    val date: LocalDate,
)
