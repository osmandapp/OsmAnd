package net.osmand.plus.flyover;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * What a flyover recording changes in OsmAnd: the map's camera, its link to my location, the shown tracks and the map
 * magnifier. Kept on disk while the recording runs, so that after a crash the next start of OsmAnd puts them back and
 * nobody lands on a map with the video's settings.
 */
public final class FlyoverSession {

	private static final Log LOG = PlatformUtil.getLog(FlyoverSession.class);
	private static final String PREFS = "flyover_session";
	private static final String KEY = "restore";

	private final JSONObject state = new JSONObject();

	/** On the UI thread, before the recording changes anything. */
	FlyoverSession(@NonNull OsmandApplication app, @NonNull OsmandMapTileView mapView) {
		OsmandSettings settings = app.getSettings();
		try {
			state.put("profile", settings.getApplicationMode().getStringKey());
			state.put("lat", mapView.getLatitude());
			state.put("lon", mapView.getLongitude());
			state.put("zoom", mapView.getZoom());
			state.put("zoom_float", mapView.getZoomFloatPart());
			state.put("rotation", mapView.getRotate());
			state.put("elevation", mapView.getElevationAngle());
			state.put("linked", settings.MAP_LINKED_TO_LOCATION.get());
			state.put("selected_gpx", settings.SELECTED_GPX.get());
			state.put("map_density", settings.MAP_DENSITY.get());
		} catch (JSONException e) {
			LOG.error(e);
		}
		prefs(app).edit().putString(KEY, state.toString()).commit();
	}

	/** After the recording has put everything else back: the camera where the user left it. */
	void finish(@NonNull OsmandApplication app, @NonNull OsmandMapTileView mapView) {
		mapView.setLatLon(state.optDouble("lat"), state.optDouble("lon"));
		mapView.setZoomWithFloatPart(state.optInt("zoom"), (float) state.optDouble("zoom_float"));
		mapView.rotateToAnimate((float) state.optDouble("rotation"));
		mapView.setElevationAngle((float) state.optDouble("elevation"));
		prefs(app).edit().remove(KEY).commit();
	}

	/**
	 * At startup, before the tracks are loaded: a recording that did not finish (OsmAnd crashed or was killed) left the
	 * video's settings; they are put back as they were before it.
	 */
	public static void recover(@NonNull OsmandApplication app) {
		SharedPreferences prefs = prefs(app);
		String saved = prefs.getString(KEY, null);
		if (saved == null) {
			return;
		}
		try {
			JSONObject state = new JSONObject(saved);
			OsmandSettings settings = app.getSettings();
			ApplicationMode mode = ApplicationMode.valueOfStringKey(state.optString("profile"), settings.getApplicationMode());
			settings.SELECTED_GPX.set(state.optString("selected_gpx"));
			settings.MAP_LINKED_TO_LOCATION.set(state.optBoolean("linked", true));
			settings.MAP_DENSITY.setModeValue(mode, (float) state.optDouble("map_density", 1));
			settings.setLastKnownMapLocation(new LatLon(state.optDouble("lat"), state.optDouble("lon")));
			settings.setLastKnownMapZoom(state.optInt("zoom"));
			settings.setLastKnownMapZoomFloatPart((float) state.optDouble("zoom_float"));
			settings.setLastKnownMapRotation(mode, (float) state.optDouble("rotation"));
			settings.setLastKnownMapElevation(mode, (float) state.optDouble("elevation"));
			LOG.info("Settings of an unfinished flyover recording restored");
		} catch (JSONException e) {
			LOG.error(e);
		}
		prefs.edit().remove(KEY).commit();
	}

	@NonNull
	private static SharedPreferences prefs(@NonNull Context context) {
		return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}
}
