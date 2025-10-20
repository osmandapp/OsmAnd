package net.osmand.reviews;

import net.osmand.data.LatLon;

import java.util.List;

/**
 * @param location        location of the POI
 * @param elementType     the {@code OsmElementType} of the POI
 * @param osmId           the OSM node or way id, where known
 * @param aggregateRating the aggregate (usually average) rating of this place
 * @param reviews         the reviews of this place
 */
public record ReviewedPlace(LatLon location,
                            OsmElementType elementType,
                            Long osmId,
                            int aggregateRating,
                            List<Review> reviews) {
    public static final int AGGREGATE_RATING_UNDEFINED = 0;

    public ReviewedPlace withAggregateRating(int averageRating) {
        return new ReviewedPlace(location, elementType, osmId, averageRating, reviews);
    }
}
