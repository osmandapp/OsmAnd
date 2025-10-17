package net.osmand.reviews;

import net.osmand.data.LatLon;

import java.util.List;

/**
 * @param location    location of the POI
 * @param elementType the {@code OsmElementType} of the POI
 * @param osmId       the OSM node or way id, where known
 * @param reviews     the reviews of this place
 */
public record ReviewedPlace(LatLon location,
                            OsmElementType elementType,
                            Long osmId,
                            List<Review> reviews) {
}
