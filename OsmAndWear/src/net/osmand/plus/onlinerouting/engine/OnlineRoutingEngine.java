package net.osmand.plus.onlinerouting.engine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.util.Algorithms.isEmpty;

public abstract class OnlineRoutingEngine implements Cloneable {

	protected static final Log LOG = PlatformUtil.getLog(OnlineRoutingEngine.class);

	public static final String ONLINE_ROUTING_ENGINE_PREFIX = "online_routing_engine_";
	public static final String PREDEFINED_PREFIX = ONLINE_ROUTING_ENGINE_PREFIX + "predefined_";

	public static final VehicleType CUSTOM_VEHICLE = new VehicleType("", R.string.shared_string_custom);
	public static final VehicleType NONE_VEHICLE = new VehicleType("None", R.string.shared_string_none);

	private final Map<String, String> params = new HashMap<>();
	private final List<VehicleType> allowedVehicles = new ArrayList<>();
	private final Set<EngineParameter> allowedParameters = new HashSet<>();

	public OnlineRoutingEngine(@Nullable Map<String, String> params) {
		// Params represents the entire state of an engine object.
		// An engine object with null params used only to provide information about the engine type
		if (!isEmpty(params)) {
			this.params.putAll(params);
		}
		collectAllowedVehicles();
		collectAllowedParameters();
	}

	@NonNull
	public abstract OnlineRoutingEngine getType();

	@NonNull
	public abstract String getTitle();

	@NonNull
	public abstract String getTypeName();

	@Nullable
	public String getStringKey() {
		return get(EngineParameter.KEY);
	}

	/**
	 * Only used when creating a full API url
	 * @return a string that represents the type of vehicle, or an empty string
	 * if the vehicle type not provided
	 */
	@NonNull
	public String getVehicleKeyForUrl() {
		String key = get(EngineParameter.VEHICLE_KEY);
		if (key == null || NONE_VEHICLE.getKey().equals(key)) {
			return "";
		}
		return key;
	}

	@NonNull
	public String getName(@NonNull Context ctx) {
		String name = get(EngineParameter.CUSTOM_NAME);
		if (name == null) {
			name = getStandardName(ctx);
		}
		String index = get(EngineParameter.NAME_INDEX);
		return !isEmpty(index) ? name + " " + index : name;
	}

	@NonNull
	public String getStandardName(@NonNull Context ctx) {
		String vehicleTitle = getSelectedVehicleName(ctx);
		if (isEmpty(vehicleTitle)) {
			return getType().getTitle();
		} else {
			String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_dash);
			return String.format(pattern, getType().getTitle(), vehicleTitle);
		}
	}

	public boolean shouldApproximateRoute() {
		String value = get(EngineParameter.APPROXIMATION_ROUTING_PROFILE);
		return !Algorithms.isEmpty(value);
	}

	public boolean isOnlineEngineWithApproximation() {
		return get(EngineParameter.CUSTOM_URL) != null && shouldApproximateRoute();
	}

	@Nullable
	public String getApproximationRoutingProfile() {
		String routingProfile = get(EngineParameter.APPROXIMATION_ROUTING_PROFILE);
		return !Algorithms.isEmpty(routingProfile) ? routingProfile : null;
	}

	@Nullable
	public String getApproximationDerivedProfile() {
		String derivedProfile = get(EngineParameter.APPROXIMATION_DERIVED_PROFILE);
		return !Algorithms.isEmpty(derivedProfile) ? derivedProfile : null;
	}

	public boolean useExternalTimestamps() {
		String value = get(EngineParameter.USE_EXTERNAL_TIMESTAMPS);
		return !Algorithms.isEmpty(value) && Boolean.parseBoolean(value);
	}

	public boolean useRoutingFallback() {
		String value = get(EngineParameter.USE_ROUTING_FALLBACK);
		return !Algorithms.isEmpty(value) && Boolean.parseBoolean(value);
	}

	@NonNull
	public String getFullUrl(@NonNull List<LatLon> path, @Nullable Float startBearing) {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		makeFullUrl(sb, path, startBearing);
		return sb.toString();
	}

	protected abstract void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path, @Nullable Float startBearing);

	@NonNull
	public String getBaseUrl() {
		String customUrl = get(EngineParameter.CUSTOM_URL);
		if (isEmpty(customUrl)) {
			return getStandardUrl();
		}
		return customUrl;
	}

	@NonNull
	public abstract String getStandardUrl();

	@NonNull
	public String getHTTPMethod() {
		return "GET";
	}

	@Nullable
	public Map<String, String> getRequestHeaders() {
		return null;
	}

	@Nullable
	public String getRequestBody(@NonNull List<LatLon> path, @Nullable Float startBearing) throws JSONException {
		return null;
	}

	@Nullable
	public abstract OnlineRoutingResponse responseByContent(@NonNull OsmandApplication app, @NonNull String content,
	                                                        boolean leftSideNavigation, boolean initialCalculation,
	                                                        @Nullable RouteCalculationProgress calculationProgress) throws JSONException;

	public abstract OnlineRoutingResponse responseByGpxFile(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile,
	                                                        boolean initialCalculation, @Nullable RouteCalculationProgress calculationProgress);

	public abstract boolean isResultOk(@NonNull StringBuilder errorMessage,
	                                   @NonNull String content) throws JSONException;

	@NonNull
	public Map<String, String> getParams() {
		return params;
	}

	@Nullable
	public String get(@NonNull EngineParameter key) {
		return params.get(key.name());
	}

	public void put(@NonNull EngineParameter key, @NonNull String value) {
		params.put(key.name(), value);
	}

	public void remove(@NonNull EngineParameter key) {
		params.remove(key.name());
	}

	private void collectAllowedVehicles() {
		allowedVehicles.clear();
		collectAllowedVehicles(allowedVehicles);
		allowedVehicles.add(CUSTOM_VEHICLE);
		allowedVehicles.add(NONE_VEHICLE);
	}

	protected abstract void collectAllowedVehicles(@NonNull List<VehicleType> vehicles);

	@NonNull
	public List<VehicleType> getAllowedVehicles() {
		return Collections.unmodifiableList(allowedVehicles);
	}

	private void collectAllowedParameters() {
		collectAllowedParameters(allowedParameters);
	}

	protected abstract void collectAllowedParameters(@NonNull Set<EngineParameter> params);

	public boolean isParameterAllowed(EngineParameter key) {
		return allowedParameters.contains(key);
	}

	public boolean isPredefined() {
		return isPredefinedEngineKey(getStringKey());
	}

	public void updateRouteParameters(@NonNull RouteCalculationParams params, @Nullable RouteCalculationResult previousRoute) {
	}

	@Nullable
	protected String getSelectedVehicleName(@NonNull Context ctx) {
		if (isCustomParameterizedVehicle()) {
			return CUSTOM_VEHICLE.getTitle(ctx);
		}
		String key = get(EngineParameter.VEHICLE_KEY);
		VehicleType vt = getVehicleTypeByKey(key);
		if (!vt.equals(CUSTOM_VEHICLE)) {
			return vt.getTitle(ctx);
		}
		return key != null ? Algorithms.capitalizeFirstLetter(key) : null;
	}

	@NonNull
	public VehicleType getSelectedVehicleType() {
		String key = get(EngineParameter.VEHICLE_KEY);
		return getVehicleTypeByKey(key);
	}

	@NonNull
	public VehicleType getVehicleTypeByKey(@Nullable String vehicleKey) {
		if (!isEmpty(vehicleKey)) {
			for (VehicleType vt : allowedVehicles) {
				if (Algorithms.objectEquals(vt.getKey(), vehicleKey)) {
					return vt;
				}
			}
		}
		return CUSTOM_VEHICLE;
	}

	public boolean isCustomParameterizedVehicle() {
		return isCustomParameterizedValue(get(EngineParameter.VEHICLE_KEY));
	}

	/**
	 * @return 'true' if the custom input has any custom parameters, 'false' - otherwise.
	 * For example, for custom input "&profile=car&locale=en" the method returns 'true'.
	 */
	public boolean isCustomParameterizedValue(@Nullable String value) {
		if (value != null) {
			return value.startsWith("&") || value.indexOf("=") < value.indexOf("&");
		}
		return false;
	}

	@NonNull
	@Override
	public Object clone() {
		return newInstance(getParams());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OnlineRoutingEngine)) return false;

		OnlineRoutingEngine engine = (OnlineRoutingEngine) o;
		if (getType() != engine.getType()) return false;
		return Algorithms.objectEquals(getParams(), engine.getParams());
	}

	public abstract OnlineRoutingEngine newInstance(Map<String, String> params);

	@NonNull
	public static String generateKey() {
		return ONLINE_ROUTING_ENGINE_PREFIX + System.currentTimeMillis();
	}

	@NonNull
	public static String generatePredefinedKey(@NonNull String provider,
	                                           @NonNull String type) {
		String key = PREDEFINED_PREFIX + provider + "_" + type;
		return key.replaceAll(" ", "_").toLowerCase();
	}

	public static boolean isPredefinedEngineKey(@Nullable String stringKey) {
		return stringKey != null && stringKey.startsWith(PREDEFINED_PREFIX);
	}

	public static boolean isOnlineEngineKey(@Nullable String stringKey) {
		return stringKey != null && stringKey.startsWith(ONLINE_ROUTING_ENGINE_PREFIX);
	}

	public static class OnlineRoutingResponse {

		private List<Location> route;
		private List<RouteDirectionInfo> directions;

		private GpxFile gpxFile;
		private boolean calculatedTimeSpeed;

		// constructor for JSON responses
		public OnlineRoutingResponse(List<Location> route, List<RouteDirectionInfo> directions) {
			this.route = route;
			this.directions = directions;
		}

		// constructor for GPX responses
		public OnlineRoutingResponse(GpxFile gpxFile, boolean calculatedTimeSpeed) {
			this.gpxFile = gpxFile;
			this.calculatedTimeSpeed = calculatedTimeSpeed;
		}

		public List<Location> getRoute() {
			return route;
		}

		public List<RouteDirectionInfo> getDirections() {
			return directions;
		}

		public GpxFile getGpxFile() {
			return gpxFile;
		}

		public boolean hasCalculatedTimeSpeed() {
			return calculatedTimeSpeed;
		}
	}

}
