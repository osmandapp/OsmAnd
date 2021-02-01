package net.osmand.plus.onlinerouting.engine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingFactory;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.util.Algorithms.isEmpty;

public abstract class OnlineRoutingEngine implements Cloneable {

	public final static String ONLINE_ROUTING_ENGINE_PREFIX = "online_routing_engine_";
	public final static VehicleType CUSTOM_VEHICLE = new VehicleType("", R.string.shared_string_custom);

	private final Map<String, String> params = new HashMap<>();
	private final List<VehicleType> allowedVehicles = new ArrayList<>();
	private final Set<EngineParameter> allowedParameters = new HashSet<>();

	public OnlineRoutingEngine(@Nullable Map<String, String> params) {
		if (!isEmpty(params)) {
			this.params.putAll(params);
		}
		collectAllowedVehiclesInternal();
		collectAllowedParametersInternal();
	}

	@NonNull
	public abstract EngineType getType();

	@Nullable
	public String getStringKey() {
		return get(EngineParameter.KEY);
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

	@NonNull
	public String getFullUrl(@NonNull List<LatLon> path) {
		StringBuilder sb = new StringBuilder(getBaseUrl());
		makeFullUrl(sb, path);
		return sb.toString();
	}

	protected abstract void makeFullUrl(@NonNull StringBuilder sb,
	                                    @NonNull List<LatLon> path);

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

	public OnlineRoutingResponse parseServerResponse(@NonNull String content,
	                                                 @NonNull OsmandApplication app,
	                                                 boolean leftSideNavigation) throws JSONException {
		JSONObject root = parseRootResponseObject(content);
		return root != null ? parseServerResponse(root, app, leftSideNavigation) : null;
	}

	@Nullable
	protected abstract OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                             @NonNull OsmandApplication app,
	                                                             boolean leftSideNavigation) throws JSONException;

	@Nullable
	protected JSONObject parseRootResponseObject(@NonNull String content) throws JSONException {
		JSONObject fullJSON = new JSONObject(content);
		String responseArrayKey = getRootArrayKey();
		JSONArray array = null;
		if (fullJSON.has(responseArrayKey)) {
			array = fullJSON.getJSONArray(responseArrayKey);
		}
		return array != null && array.length() > 0 ? array.getJSONObject(0) : null;
	}

	@NonNull
	protected abstract String getRootArrayKey();

	@NonNull
	protected List<Location> convertRouteToLocationsList(@NonNull List<LatLon> route) {
		List<Location> result = new ArrayList<>();
		if (!isEmpty(route)) {
			for (LatLon pt : route) {
				WptPt wpt = new WptPt();
				wpt.lat = pt.getLatitude();
				wpt.lon = pt.getLongitude();
				result.add(RouteProvider.createLocation(wpt));
			}
		}
		return result;
	}

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

	private void collectAllowedVehiclesInternal() {
		allowedVehicles.clear();
		collectAllowedVehicles(allowedVehicles);
		allowedVehicles.add(CUSTOM_VEHICLE);
	}

	protected abstract void collectAllowedVehicles(@NonNull List<VehicleType> vehicles);

	@NonNull
	public List<VehicleType> getAllowedVehicles() {
		return Collections.unmodifiableList(allowedVehicles);
	}

	private void collectAllowedParametersInternal() {
		allowedParameters.clear();
		allowParameters(EngineParameter.KEY, EngineParameter.VEHICLE_KEY,
				EngineParameter.CUSTOM_NAME, EngineParameter.NAME_INDEX, EngineParameter.CUSTOM_URL);
		collectAllowedParameters();
	}

	protected abstract void collectAllowedParameters();

	public boolean isParameterAllowed(EngineParameter key) {
		return allowedParameters.contains(key);
	}

	protected void allowParameters(@NonNull EngineParameter... allowedParams) {
		allowedParameters.addAll(Arrays.asList(allowedParams));
	}

	@Nullable
	private String getSelectedVehicleName(@NonNull Context ctx) {
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

	public boolean checkServerResponse(@NonNull StringBuilder errorMessage,
	                                   @NonNull String content) throws JSONException {
		JSONObject obj = new JSONObject(content);
		String messageKey = getErrorMessageKey();
		if (obj.has(messageKey)) {
			String message = obj.getString(messageKey);
			errorMessage.append(message);
		}
		return obj.has(getRootArrayKey());
	}

	@NonNull
	protected abstract String getErrorMessageKey();

	@NonNull
	@Override
	public Object clone() {
		return OnlineRoutingFactory.createEngine(getType(), getParams());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OnlineRoutingEngine)) return false;

		OnlineRoutingEngine engine = (OnlineRoutingEngine) o;
		if (getType() != engine.getType()) return false;
		return Algorithms.objectEquals(getParams(), engine.getParams());
	}

	@NonNull
	public static String generateKey() {
		return ONLINE_ROUTING_ENGINE_PREFIX + System.currentTimeMillis();
	}

	public static class OnlineRoutingResponse {
		private List<Location> route;
		private List<RouteDirectionInfo> directions;

		public OnlineRoutingResponse(List<Location> route, List<RouteDirectionInfo> directions) {
			this.route = route;
			this.directions = directions;
		}

		public List<Location> getRoute() {
			return route;
		}

		public List<RouteDirectionInfo> getDirections() {
			return directions;
		}
	}
}
