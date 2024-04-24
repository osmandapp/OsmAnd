package net.osmand.plus.routepreparationmenu;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.OnResultCallback;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.settings.enums.RoutingType;
import net.osmand.router.MissingMapsCalculator;
import net.osmand.router.RoutePlannerFrontEnd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CalculateMissingMapsOnlineTask extends AsyncTask<Void, Void, Void> {

	private static final String ONLINE_CALCULATION_URL = "https://maptile.osmand.net/routing/route?routeMode=car";

	private final OsmandApplication app;
	private final CalculateMissingMapsOnlineListener listener;

	public CalculateMissingMapsOnlineTask(@NonNull OsmandApplication app,
	                                      @NonNull CalculateMissingMapsOnlineListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		MissingMapsCalculator calculator = RoutePlannerFrontEnd.getMissingMapsCalculator();
		if (calculator != null && calculator.getStartPoint() != null && calculator.getEndPoint() != null) {
			LatLon startPoint = calculator.getStartPoint();
			LatLon endPoint = calculator.getEndPoint();
			onlineCalculateRequestStartPoint(startPoint, endPoint, locations -> {
				try {
					RoutingType routingType = app.getSettings().ROUTING_TYPE.get();
					calculator.checkIfThereAreMissingMaps(convertLocationsToLatLon(locations), !routingType.isHHRouting());
					listener.onSuccess();
				} catch (IOException e) {
					listener.onError(e.getMessage());
				}
			});
		} else {
			listener.onError(null);
		}
		return null;
	}

	private void onlineCalculateRequestStartPoint(@NonNull LatLon start,
	                                              @NonNull LatLon finish,
	                                              @NonNull OnResultCallback<List<Location>> callback) {
		String fullURL = ONLINE_CALCULATION_URL + "&" + formatPointString(start) + "&" + formatPointString(finish);
		try {
			OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
			String response = helper.makeRequest(fullURL);
			callback.onResult(parseOnlineCalculationResponse(response));
		} catch (Exception e) {
			listener.onError(e.getMessage());
		}
	}

	@NonNull
	private String formatPointString(@NonNull LatLon location) {
		return "points=" + location.getLatitude() + "," + location.getLongitude();
	}

	@NonNull
	private List<Location> parseOnlineCalculationResponse(@NonNull String response) throws JSONException {
		List<Location> result = new ArrayList<>();
		JSONObject fullJSON = new JSONObject(response);
		JSONArray features = fullJSON.getJSONArray("features");
		for (int i = 0; i < features.length(); i++) {
			JSONObject feature = features.getJSONObject(i);
			JSONObject geometry = feature.getJSONObject("geometry");
			String type = geometry.getString("type");
			if (Objects.equals(type, "LineString")) {
				JSONArray coordinates = geometry.getJSONArray("coordinates");
				for (int j = 0; j < coordinates.length(); j++) {
					JSONArray coordinate = coordinates.getJSONArray(j);
					parseAndAddLocation(result, coordinate);
				}
			} else if (Objects.equals(type, "Point")) {
				JSONArray coordinates = geometry.getJSONArray("coordinates");
				parseAndAddLocation(result, coordinates);
			}
		}
		return result;
	}

	private void parseAndAddLocation(@NonNull List<Location> locations, @NonNull JSONArray coordinate) throws JSONException {
		if (coordinate.length() >= 2) {
			WptPt wpt = new WptPt();
			wpt.lat = coordinate.getDouble(1);
			wpt.lon = coordinate.getDouble(0);
			locations.add(RouteProvider.createLocation(wpt));
		}
	}

	@NonNull
	private List<LatLon> convertLocationsToLatLon(@NonNull List<Location> locations) {
		List<LatLon> result = new ArrayList<>();
		for (Location location : locations) {
			result.add(new LatLon(location.getLatitude(), location.getLongitude()));
		}
		return result;
	}

	@NonNull
	public static CalculateMissingMapsOnlineTask execute(@NonNull OsmandApplication app,
	                                                     @NonNull CalculateMissingMapsOnlineListener listener) {
		CalculateMissingMapsOnlineTask task = new CalculateMissingMapsOnlineTask(app, listener);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		return task;
	}

	public interface CalculateMissingMapsOnlineListener {
		void onSuccess();
		void onError(@Nullable String error);
	}
}
