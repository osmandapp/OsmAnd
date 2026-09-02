package net.osmand.plus.plugins.panoramax;

/**
 * Endpoints of the Panoramax instance the plugin talks to.
 *
 * Panoramax is federated: api.panoramax.xyz aggregates the member instances, and picture
 * requests against it redirect to whichever instance actually stores the file. Should instance
 * selection ever be exposed in settings, {@link #INSTANCE_URL} is the only value that has to
 * become configurable here.
 *
 * The vector tile URL is deliberately not in this class. It lives in TileSourceManager, which
 * is part of the OsmAnd-java module and cannot reference Android plugin code; keep the two in
 * step when changing instance.
 */
public class PanoramaxConstants {

	public static final String INSTANCE_URL = "https://api.panoramax.xyz/";

	public static final String API_URL = INSTANCE_URL + "api/";

	/** Web viewer focused on a single picture; append the picture id. */
	public static final String VIEWER_URL_TEMPLATE = INSTANCE_URL + "#focus=pic&pic=";

	/** User search, no API key required; the query string has to be URL encoded. */
	public static final String USER_SEARCH_URL = API_URL + "users/search?q=%s";

	private static final String PICTURES_PATH = API_URL + "pictures/";

	private PanoramaxConstants() {
	}

	public static String getViewerUrl(String imageId) {
		return VIEWER_URL_TEMPLATE + imageId;
	}

	public static String getHiResImageUrl(String imageId) {
		return PICTURES_PATH + imageId + "/hd.jpg";
	}

	public static String getImageUrl(String imageId) {
		return PICTURES_PATH + imageId + "/sd.jpg";
	}

	public static String getThumbnailUrl(String imageId) {
		return PICTURES_PATH + imageId + "/thumb.jpg";
	}
}
