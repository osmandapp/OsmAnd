package net.osmand.reviews;

import java.util.List;

/**
 * @param type    the type of amenity, as described by `amenity=` tag in OSM
 * @param lat     latitude of the POI
 * @param lon     longitude of the POI
 * @param osmId   the OSM node id, where known
 * @param reviews the reviews of this amenity
 */
public record Amenity(String type,
                      double lat,
                      double lon,
                      Long osmId,
                      List<Review> reviews) {
}
