package net.osmand.plus.onlinerouting;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.type.EngineType;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineRoutingEngine {

	public final static String ONLINE_ROUTING_ENGINE_PREFIX = "online_routing_engine_";

	public enum EngineParameter {
		CUSTOM_NAME,
		CUSTOM_URL,
		API_KEY
	}

	private String stringKey;
	private EngineType type;
	private String customUrl;
	private String vehicleKey;
	private Map<String, String> params = new HashMap<>();

	private OnlineRoutingEngine() {};

	public OnlineRoutingEngine(@NonNull String stringKey,
	                           @NonNull EngineType type,
	                           @NonNull String vehicleKey,
	                           Map<String, String> params) {
		this(stringKey, type, vehicleKey);
		this.params = params;
	}

	public OnlineRoutingEngine(@NonNull String stringKey,
	                           @NonNull EngineType type,
	                           @NonNull String vehicleKey) {
		this(type, vehicleKey);
		this.stringKey = stringKey;
	}

	private OnlineRoutingEngine(@NonNull EngineType type,
	                            @NonNull String vehicleKey) {
		this.type = type;
		this.vehicleKey = vehicleKey;
	}

	public String getStringKey() {
		return stringKey;
	}

	public EngineType getType() {
		return type;
	}

	public String getBaseUrl() {
		if (Algorithms.isEmpty(customUrl)) {
			return type.getStandardUrl();
		}
		return customUrl;
	}

	public String getVehicleKey() {
		return vehicleKey;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public String getParameter(EngineParameter paramKey) {
		return params.get(paramKey.name());
	}

	public void putParameter(EngineParameter paramKey, String paramValue) {
		params.put(paramKey.name(), paramValue);
	}

	public String getName(@NonNull Context ctx) {
		String customName = getParameter(EngineParameter.CUSTOM_NAME);
		if (customName != null) {
			return customName;
		} else {
			return getStandardName(ctx);
		}
	}

	public String createFullUrl(@NonNull List<LatLon> path) {
		return type.createFullUrl(this, path);
	}

	private String getStandardName(@NonNull Context ctx) {
		return getStandardName(ctx, type, vehicleKey);
	}

	public static String getStandardName(@NonNull Context ctx,
	                                     @NonNull EngineType type,
	                                     @NonNull String vehicleKey) {
		String vehicleTitle = VehicleType.toHumanString(ctx, vehicleKey);
		String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_dash);
		return String.format(pattern, type.getTitle(), vehicleTitle);
	}

	public static OnlineRoutingEngine createNewEngine(@NonNull EngineType type,
	                                                  @NonNull String vehicleKey) {
		return new OnlineRoutingEngine(generateKey(), type, vehicleKey);
	}

	public static OnlineRoutingEngine createTmpEngine(@NonNull EngineType type,
	                                                  @NonNull String vehicleKey) {
		return new OnlineRoutingEngine(type, vehicleKey);
	}

	private static String generateKey() {
		return ONLINE_ROUTING_ENGINE_PREFIX + System.currentTimeMillis();
	}
}
