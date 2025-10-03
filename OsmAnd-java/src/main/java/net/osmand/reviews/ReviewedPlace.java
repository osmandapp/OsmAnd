package net.osmand.reviews;

import java.util.List;

/**
 * @param lat     latitude of the POI
 * @param lon     longitude of the POI
 * @param elementType the {@code OsmElementType} of the POI
 * @param osmId   the OSM node or way id, where known
 * @param reviews the reviews of this place
 */
public record ReviewedPlace(double lat,
                            double lon,
                            OsmElementType elementType,
                            Long osmId,
                            List<Review> reviews) {
}
