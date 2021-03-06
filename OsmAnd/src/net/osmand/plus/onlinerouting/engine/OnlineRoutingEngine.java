package net.osmand.plus.onlinerouting.engine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteDirectionInfo;
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

	public final static String ONLINE_ROUTING_ENGINE_PREFIX = "online_routing_engine_";
	public final static VehicleType CUSTOM_VEHICLE = new VehicleType("", R.string.shared_string_custom);

	private final Map<String, String> params = new HashMap<>();
	private final List<VehicleType> allowedVehicles = new ArrayList<>();
	private final Set<EngineParameter> allowedParameters = new HashSet<>();

	public OnlineRoutingEngine(@Nullable Map<String, String> params) {
		// Params represents the entire state of an engine object.
		// An engine object with null params used only to provide information about the engine type
		if (!isEmpty(params)) {
			this.params.putAll(params);
		}
		collectAllowedVehiclesInternal();
		collectAllowedParameters(allowedParameters);
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

	@Nullable
	public abstract OnlineRoutingResponse parseResponse(@NonNull String content,
	                                                    @NonNull OsmandApplication app,
	                                                    boolean leftSideNavigation) throws JSONException;

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

	protected abstract void collectAllowedParameters(@NonNull Set<EngineParameter> params);

	public boolean isParameterAllowed(EngineParameter key) {
		return allowedParameters.contains(key);
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

	public static class OnlineRoutingResponse {

		private List<Location> route;
		private List<RouteDirectionInfo> directions;
		private GPXFile gpxFile;

		// constructor for JSON responses
		public OnlineRoutingResponse(List<Location> route, List<RouteDirectionInfo> directions) {
			this.route = route;
			this.directions = directions;
		}

		// constructor for GPX responses
		public OnlineRoutingResponse(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
		}

		public List<Location> getRoute() {
			return route;
		}

		public List<RouteDirectionInfo> getDirections() {
			return directions;
		}

		public GPXFile getGpxFile() {
			return gpxFile;
		}
	}

}
