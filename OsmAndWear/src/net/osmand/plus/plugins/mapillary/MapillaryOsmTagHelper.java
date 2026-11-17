package net.osmand.plus.plugins.mapillary;

import static net.osmand.map.TileSourceManager.MAPILLARY_ACCESS_TOKEN;
import static net.osmand.plus.plugins.mapillary.MapillaryImageDialog.MAPILLARY_VIEWER_URL_TEMPLATE;
import static net.osmand.plus.plugins.mapillary.MapillaryPlugin.TYPE_MAPILLARY_PHOTO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapillaryOsmTagHelper {

	private static final String GRAPH_URL_ENDPOINT = "https://graph.mapillary.com/";
	private static final String PARAM_ACCESS_TOKEN = "access_token=" + MAPILLARY_ACCESS_TOKEN;
	private static final String PARAM_FIELDS = "fields=id,geometry,compass_angle,captured_at,camera_type,thumb_256_url,thumb_1024_url";

	private static final String ID = "id";
	private static final String GEOMETRY = "geometry";
	private static final String COORDINATES = "coordinates";
	private static final String COMPASS_ANGLE = "compass_angle";
	private static final String CAPTURED_AT = "captured_at";
	private static final String CAMERA_TYPE = "camera_type";
	private static final String THUMB_256_URL = "thumb_256_url";
	private static final String THUMB_1024_URL = "thumb_1024_url";

	private static final Log LOG = PlatformUtil.getLog(MapillaryOsmTagHelper.class);

	@Nullable
	public static JSONObject getImageByKey(String key) {
		String url = GRAPH_URL_ENDPOINT + key + '?' + PARAM_ACCESS_TOKEN + '&' + PARAM_FIELDS;
		JSONObject response = request(url);
		return response != null ? parseResponse(response) : null;
	}

	@Nullable
	private static JSONObject request(@NonNull String url) {
		StringBuilder rawResponse = new StringBuilder();
		String errorMessage = NetworkUtils.sendGetRequest(url, null, rawResponse);
		if (errorMessage == null) {
			try {
				return new JSONObject(rawResponse.toString());
			} catch (Exception e) {
				errorMessage = e.getLocalizedMessage();
			}
		}
		LOG.error(errorMessage);
		return null;
	}

	private static JSONObject parseResponse(@NonNull JSONObject response) {
		try {
			JSONObject result = new JSONObject();
			result.put("type", TYPE_MAPILLARY_PHOTO);

			if (response.has(GEOMETRY)) {
				JSONObject geometry = response.getJSONObject(GEOMETRY);
				String geometryType = geometry.getString("type");
				if ("Point".equals(geometryType) && geometry.has(COORDINATES)) {
					JSONArray coordinates = geometry.getJSONArray(COORDINATES);
					result.put("lat", coordinates.get(1));
					result.put("lon", coordinates.get(0));
				}
			}

			if (response.has(CAPTURED_AT)) {
				String capturedAt = response.getString(CAPTURED_AT);
				result.put("timestamp", capturedAt);
			}

			if (response.has(ID)) {
				String id = response.getString(ID);
				result.put("key", id);
				result.put("url", MAPILLARY_VIEWER_URL_TEMPLATE + id);
			}

			if (response.has(COMPASS_ANGLE)) {
				String compassAngle = response.getString(COMPASS_ANGLE);
				result.put("ca", compassAngle);
			}

			if (response.has(CAMERA_TYPE)) {
				String cameraType = response.getString(CAMERA_TYPE);
				boolean is360 = Algorithms.stringsEqual(cameraType, "equirectangular")
						|| Algorithms.stringsEqual(cameraType, "spherical");
				result.put("360", is360);
			}

			if (response.has(THUMB_256_URL)) {
				String imgUrl = response.getString(THUMB_256_URL);
				result.put("imageUrl", imgUrl);
			}

			if (response.has(THUMB_1024_URL)) {
				String imgHiresUrl = response.getString(THUMB_1024_URL);
				result.put("imageHiresUrl", imgHiresUrl);
			}

			result.put("externalLink", false);
			result.put("topIcon", "ic_logo_mapillary");
			// compute distance and bearing (don't used now)
			return result;
		} catch (JSONException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return null;
	}

}
