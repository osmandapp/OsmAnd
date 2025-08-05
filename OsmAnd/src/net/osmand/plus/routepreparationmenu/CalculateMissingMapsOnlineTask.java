package net.osmand.plus.routepreparationmenu;

import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.RoutingType;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.MissingMapsCalculationResult;
import net.osmand.router.MissingMapsCalculator;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CalculateMissingMapsOnlineTask extends AsyncTask<Void, Void, Void> {

	private static final String ONLINE_CALCULATION_URL = "https://maptile.osmand.net/routing/route?routeMode=";

	private final OsmandApplication app;
	private final CalculateMissingMapsOnlineListener listener;

	public CalculateMissingMapsOnlineTask(@NonNull OsmandApplication app,
	                                      @NonNull CalculateMissingMapsOnlineListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		MissingMapsCalculator calculator = new MissingMapsCalculator(app.getRegions());
		RouteCalculationResult route = app.getRoutingHelper().getRoute();
		MissingMapsCalculationResult previousResult = route.getMissingMapsCalculationResult();
		RoutingContext routingContext = previousResult != null ? previousResult.getMissingMapsRoutingContext() : null;
		List<LatLon> routePoints = previousResult != null ? previousResult.getMissingMapsPoints() : null;

		TargetPointsHelper pointsHelper = app.getTargetPointsHelper();
		TargetPoint start = pointsHelper.getPointToStart();
		TargetPoint end = pointsHelper.getPointToNavigate();
		Location lastKnownLocation = app.getLocationProvider().getLastStaleKnownLocation();
		if ((start != null || lastKnownLocation != null) && end != null) {
			LatLon startPoint = start != null ? start.getLatLon()
					: new LatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
			routePoints = CollectionUtils.asOneList(
					Collections.singletonList(startPoint),
					pointsHelper.getIntermediatePointsLatLon(),
					Collections.singletonList(end.getLatLon())
			);
		}

		if (routingContext != null && routePoints != null) {
			StringBuilder url = new StringBuilder(ONLINE_CALCULATION_URL)
					.append(getRoutingProfile())
					.append(getFormattedRoutingParameters());
			for (LatLon point : routePoints) {
				url.append("&").append(formatPointString(point));
			}
			try {
				RoutingType routingType = app.getSettings().ROUTING_TYPE.get();
				OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
				String response = helper.makeRequest(url.toString());
				List<LatLon> locations = parseOnlineCalculationResponse(response);
				calculator.checkIfThereAreMissingMaps(
						routingContext, routePoints.get(0), locations, routingType.isHHRouting()
				);
				if (routingContext.calculationProgress.missingMapsCalculationResult != null) {
					route.setMissingMapsCalculationResult(routingContext.calculationProgress.missingMapsCalculationResult);
				}
				listener.onSuccess();
			} catch (Exception e) {
				listener.onError(e.getMessage());
			}
		} else {
			listener.onError(null);
		}
		return null;
	}

	@NonNull
	private String getRoutingProfile() {
		RouteCalculationResult prevRoute = app.getRoutingHelper().getRoute();
		RoutingConfiguration config = prevRoute.getMissingMapsCalculationResult().getMissingMapsRoutingContext().config;
		GeneralRouterProfile profile = config.router.getProfile();
		boolean useBicycle = profile == GeneralRouterProfile.BICYCLE || profile == GeneralRouterProfile.PEDESTRIAN;
		return useBicycle ? "bicycle" : "car";
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
			Map<String, RoutingParameter> parameters = RoutingHelperUtils.getParametersForDerivedProfile(appMode, router);
			for (Map.Entry<String, RoutingParameter> e : parameters.entrySet()) {
				if (isParameterEnabled(e.getValue())) {
					activeParameters.add(e.getKey());
				}
			}
		}
		String profile = getRoutingProfile();
		return !Algorithms.isEmpty(activeParameters)
				? "&params=" + profile + "," + TextUtils.join(",", activeParameters)
				: "";
	}

	private boolean isParameterEnabled(@NonNull RoutingParameter parameter) {
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
		return preference.getModeValue(appMode);
	}

	@NonNull
	private List<LatLon> parseOnlineCalculationResponse(@NonNull String response) throws JSONException {
		List<LatLon> result = new ArrayList<>();
		JSONObject fullJSON = new JSONObject(response);
		JSONArray features = fullJSON.getJSONArray("features");
		for (int i = 0; i < features.length(); i++) {
			JSONObject feature = features.getJSONObject(i);
			JSONObject geometry = feature.getJSONObject("geometry");
			if (Objects.equals(geometry.getString("type"), "LineString")) {
				JSONArray coordinates = geometry.getJSONArray("coordinates");
				for (int j = 0; j < coordinates.length(); j++) {
					JSONArray coordinate = coordinates.getJSONArray(j);
					parseAndAddLocation(result, coordinate);
				}
			}
		}
		return result;
	}

	private void parseAndAddLocation(@NonNull List<LatLon> locations, @NonNull JSONArray coordinate) throws JSONException {
		if (coordinate.length() >= 2) {
			double lat = coordinate.getDouble(1);
			double lon = coordinate.getDouble(0);
			locations.add(new LatLon(lat, lon));
		}
	}

	@NonNull
	public static CalculateMissingMapsOnlineTask execute(@NonNull OsmandApplication app,
	                                                     @NonNull CalculateMissingMapsOnlineListener listener) {
		CalculateMissingMapsOnlineTask task = new CalculateMissingMapsOnlineTask(app, listener);
		OsmAndTaskManager.executeTask(task);
		return task;
	}

	public interface CalculateMissingMapsOnlineListener {
		void onSuccess();

		void onError(@Nullable String error);
	}
}
