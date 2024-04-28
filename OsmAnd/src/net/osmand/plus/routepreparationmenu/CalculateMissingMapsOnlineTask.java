package net.osmand.plus.routepreparationmenu;

import static net.osmand.plus.routing.RoutingHelperUtils.getParametersForDerivedProfile;

import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.RoutingType;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.MissingMapsCalculationResult;
import net.osmand.router.MissingMapsCalculator;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CalculateMissingMapsOnlineTask extends AsyncTask<Void, Void, Void> {

	private static final String ONLINE_CALCULATION_URL = "https://maptile.osmand.net/routing/route?routeMode=";

	private final OsmandApplication app;
	private final RoutingContext context;
	private final LatLon startPoint;
	private final LatLon endPoint;
	private final CalculateMissingMapsOnlineListener listener;

	public CalculateMissingMapsOnlineTask(@NonNull OsmandApplication app,
										  @NonNull RoutingContext context,
	                                      @NonNull LatLon startPoint, @NonNull LatLon endPoint,
	                                      @NonNull CalculateMissingMapsOnlineListener listener) {
		this.app = app;
		this.context = context;
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		MissingMapsCalculator calculator = RoutePlannerFrontEnd.getMissingMapsCalculator();
		if (calculator != null) {
			List<Location> locations = calculateRouteOnline(startPoint, endPoint);
			try {
				RoutingType routingType = app.getSettings().ROUTING_TYPE.get();
				MissingMapsCalculationResult result = calculator.calculateMissingMaps(
						context, convertLocationsToLatLon(locations), routingType.isHHRouting()
				);
				listener.onSuccess(result);
			} catch (IOException e) {
				listener.onError(e.getMessage());
			}
		} else {
			listener.onError(null);
		}
		return null;
	}

	@NonNull
	private List<Location> calculateRouteOnline(@NonNull LatLon start, @NonNull LatLon finish) {
		String fullURL = ONLINE_CALCULATION_URL + getRoutingProfile()
				+ "&" + formatPointString(start)
				+ "&" + formatPointString(finish)
				+ getFormattedRoutingParameters();
		try {
			OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
			String response = helper.makeRequest(fullURL);
			return parseOnlineCalculationResponse(response);
		} catch (Exception e) {
			listener.onError(e.getMessage());
		}
		return new ArrayList<>();
	}

	@NonNull
	private String getRoutingProfile() {
		RoutingConfiguration config = context.config;
		boolean isBicycle = config.router.getProfile() == GeneralRouterProfile.BICYCLE;
		return isBicycle ? "bicycle" : "car";
	}

	@NonNull
	private String formatPointString(@NonNull LatLon location) {
		return "points=" + location.getLatitude() + "," + location.getLongitude();
	}

	@NonNull
	private String getFormattedRoutingParameters() {
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		GeneralRouter router = app.getRouter(appMode);
		List<String> activeParameters = new ArrayList<>();
		if (router != null) {
			Map<String, RoutingParameter> parameters = getParametersForDerivedProfile(appMode, router);
			for (Map.Entry<String, RoutingParameter> e : parameters.entrySet()) {
				if (isParameterEnabled(e.getValue())) {
					activeParameters.add(e.getKey());
				}
			}
		}
		return !Algorithms.isEmpty(activeParameters)
				? "&params=car," + TextUtils.join(",", activeParameters)
				: "";
	}

	private boolean isParameterEnabled(@NonNull RoutingParameter parameter) {
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
		return preference.getModeValue(appMode);
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
	                                                     @NonNull RoutingContext context,
	                                                     @NonNull LatLon startPoint, @NonNull LatLon endPoint,
	                                                     @NonNull CalculateMissingMapsOnlineListener listener) {
		CalculateMissingMapsOnlineTask task = new CalculateMissingMapsOnlineTask(app, context, startPoint, endPoint, listener);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		return task;
	}

	public interface CalculateMissingMapsOnlineListener {
		void onSuccess(@NonNull MissingMapsCalculationResult result);
		void onError(@Nullable String error);
	}
}
