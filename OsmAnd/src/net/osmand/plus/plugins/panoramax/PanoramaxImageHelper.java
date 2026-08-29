package net.osmand.plus.plugins.panoramax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Resolves the real file location and the attribution of a Panoramax picture.
 *
 * The convenience endpoints under /api/pictures/{id}/ answer with an HTTP 308 redirect to
 * whichever federated instance actually stores the file. HttpURLConnection, which OsmAnd's
 * image downloader uses, follows 301, 302 and 303 but not 308, so handing those URLs straight
 * to the downloader yields an undecodable body and the picture silently fails to load. Asking
 * the STAC API for the item gives the final URL directly, and carries the licence and the
 * contributor name in the same response, both of which have to be shown with the picture.
 */
public class PanoramaxImageHelper {

	private static final Log LOG = PlatformUtil.getLog(PanoramaxImageHelper.class);

	public static class PictureInfo {
		public String hiResUrl;
		public String imageUrl;
		public String thumbnailUrl;
		public String license;
		public String producer;
	}

	/**
	 * Blocking network call, must not run on the main thread.
	 *
	 * @return resolved urls and attribution, or null when the lookup fails. Callers should fall
	 * back to the constructed url so a lookup failure degrades to the previous behaviour rather
	 * than to no picture at all.
	 */
	@Nullable
	public static PictureInfo fetchPictureInfo(@NonNull String imageId) {
		try {
			URL url = new URL(PanoramaxConstants.API_URL + "search?ids=" + imageId);
			URLConnection connection = NetworkUtils.getHttpURLConnection(url);
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(15000);

			StringBuilder json = new StringBuilder(2048);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					json.append(line);
				}
			}

			JSONArray features = new JSONObject(json.toString()).optJSONArray("features");
			if (features == null || features.length() == 0) {
				return null;
			}
			JSONObject feature = features.getJSONObject(0);

			PictureInfo info = new PictureInfo();
			JSONObject assets = feature.optJSONObject("assets");
			if (assets != null) {
				info.hiResUrl = optHref(assets, "hd");
				info.imageUrl = optHref(assets, "sd");
				info.thumbnailUrl = optHref(assets, "thumb");
			}
			JSONObject properties = feature.optJSONObject("properties");
			if (properties != null) {
				info.license = properties.optString("license", null);
				info.producer = properties.optString("geovisio:producer", null);
			}
			return info.hiResUrl != null || info.imageUrl != null ? info : null;
		} catch (Exception e) {
			LOG.error("Failed to resolve Panoramax picture " + imageId, e);
			return null;
		}
	}

	@Nullable
	private static String optHref(@NonNull JSONObject assets, @NonNull String key) {
		JSONObject asset = assets.optJSONObject(key);
		return asset != null ? asset.optString("href", null) : null;
	}
}
