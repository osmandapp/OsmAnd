package net.osmand.reviews;

import java.net.URI;
import java.time.LocalDate;

/**
 * @param id      the review id, source system-specific
 * @param opinion a written opinion
 * @param rating  a numerical (star) rating in the range 1-100
 * @param author  the review author
 * @param date    the review date
 * @param link    a link to the review in the source system
 */
public record Review(
        String id,
        String opinion,
        int rating,
        String author,
        LocalDate date,
        URI link
) {
}
