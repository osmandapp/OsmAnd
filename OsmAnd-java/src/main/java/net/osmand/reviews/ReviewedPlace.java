package net.osmand.reviews;

import java.util.List;

/**
 * @param lat     latitude of the POI
 * @param lon     longitude of the POI
 * @param osmId   the OSM node id, where known
 * @param reviews the reviews of this place
 */
public record ReviewedPlace(double lat,
                            double lon,
                            Long osmId,
                            List<Review> reviews) {
}
