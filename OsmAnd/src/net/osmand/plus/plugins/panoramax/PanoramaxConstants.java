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

	/** Full Panoramax site focused on a single picture; append the picture id. */
	public static final String VIEWER_URL_TEMPLATE = INSTANCE_URL + "#focus=pic&pic=";

	/**
	 * Official photo only viewer bundle, pinned to the panoramax/web-viewer npm package 5.2.0.
	 * Changing the version means recomputing {@link #VIEWER_BUNDLE_INTEGRITY}, or the script
	 * is rejected and the viewer never loads.
	 */
	public static final String VIEWER_BUNDLE_URL =
			"https://cdn.jsdelivr.net/npm/@panoramax/web-viewer@5.2.0/build/cjs/index_photoviewer.js";

	/** SRI digest of exactly the file {@link #VIEWER_BUNDLE_URL} points at.
	 * Update the integrity hash when changing the bundle version. */
	public static final String VIEWER_BUNDLE_INTEGRITY =
			"sha384-A/XfT5HrbLfgrhBB5mk3bsNTuq7SqwhcUZqzYzFRko0jGYYIuf8CTk3y2ufVRPox";

	/** User search, no API key required; the query string has to be URL encoded. */
	public static final String USER_SEARCH_URL = API_URL + "users/search?q=%s";

	private PanoramaxConstants() {
	}

	public static String getViewerUrl(String imageId) {
		return VIEWER_URL_TEMPLATE + imageId;
	}
}
