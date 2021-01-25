package net.osmand.plus.onlinerouting.engine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingFactory;
import net.osmand.plus.onlinerouting.OnlineRoutingResponse;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.util.Algorithms;

import org.json.JSONException;

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
	public final static int INVALID_ID = -1;

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
	public String getBaseUrl() {
		String customUrl = get(EngineParameter.CUSTOM_URL);
		if (isEmpty(customUrl)) {
			return getStandardUrl();
		}
		return customUrl;
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
	public abstract OnlineRoutingResponse parseServerResponse(@NonNull String content,
	                                                          boolean leftSideNavigation) throws JSONException;

	@NonNull
	public abstract String getStandardUrl();

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

	protected void allowParameters(@NonNull EngineParameter ... allowedParams) {
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

	public abstract boolean parseServerMessage(@NonNull StringBuilder sb,
	                                           @NonNull String content) throws JSONException;

	@NonNull
	@Override
	public Object clone() {
		return OnlineRoutingFactory.createEngine(getType(), getParams());
	}

	@NonNull
	public static String generateKey() {
		return ONLINE_ROUTING_ENGINE_PREFIX + System.currentTimeMillis();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OnlineRoutingEngine)) return false;

		OnlineRoutingEngine engine = (OnlineRoutingEngine) o;
		if (getType() != engine.getType()) return false;
		return Algorithms.objectEquals(getParams(), engine.getParams());
	}
}
