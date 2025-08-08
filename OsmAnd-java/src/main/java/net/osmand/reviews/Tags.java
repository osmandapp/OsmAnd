package net.osmand.reviews;

/**
 * A reference of OSM-style tags used for reviews.
 */
public final class Tags {
    /** The key of the tag used to derive the POI category when generating the reviews OBF */
    public static final String REVIEWS_MARKER_TAG = "osmreviews";
    /** The value of the marker tag used to derive the POI category when generating the reviews OBF */
    public static final String REVIEWS_MARKER_VALUE = "reviewed_place";
    /** The key of the tag containing the review data */
    public static final String REVIEWS_KEY = "reviews";
    private Tags() {}
}
