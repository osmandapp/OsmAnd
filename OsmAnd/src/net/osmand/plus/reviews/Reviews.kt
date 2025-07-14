package net.osmand.plus.reviews

import org.apache.commons.logging.Log
import kotlin.math.roundToInt

object Reviews {
    val LOG: Log = net.osmand.PlatformUtil.getLog(Reviews::class.java)

    /**
     * @param rating a rating in the range 1..100
     * @return a string showing rating of 0..5 stars
     */
    fun formatStarRating(rating: Int): String =
        if (rating < 1 || rating > 100) {
            LOG.warn("invalid rating: $rating")
            "?"
        } else {
            val starCount = (rating.toFloat() / 20).roundToInt()
            "★".repeat(starCount) + "☆".repeat(5 - starCount)
        }
}