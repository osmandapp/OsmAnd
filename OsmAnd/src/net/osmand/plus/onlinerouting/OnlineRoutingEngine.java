package net.osmand.plus.onlinerouting;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class OnlineRoutingEngine {

	public final static String ONLINE_ROUTING_ENGINE_PREFIX = "online_routing_engine_";

	public enum EngineParameterType {
		CUSTOM_SERVER_URL,
		CUSTOM_NAME,
		API_KEY
	}

	private String stringKey;
	private ServerType serverType;
	private String vehicleKey;
	private Map<String, String> params = new HashMap<>();

	public OnlineRoutingEngine(@NonNull String stringKey,
							   @NonNull ServerType serverType,
							   @NonNull String vehicleKey,
							   @Nullable Map<String, String> params) {
		this(stringKey, serverType, vehicleKey);
		if (!Algorithms.isEmpty(params)) {
			this.params.putAll(params);
		}
	}

	public OnlineRoutingEngine(@NonNull String stringKey,
	                           @NonNull ServerType serverType,
	                           @NonNull String vehicleKey) {
		this.stringKey = stringKey;
		this.serverType = serverType;
		this.vehicleKey = vehicleKey;
	}

	public String getStringKey() {
		return stringKey;
	}

	public ServerType getServerType() {
		return serverType;
	}

	public String getVehicleKey() {
		return vehicleKey;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public String getBaseUrl() {
		String customServerUrl = getParameter(EngineParameterType.CUSTOM_SERVER_URL);
		if (!Algorithms.isEmpty(customServerUrl)) {
			return customServerUrl;
		} else {
			return serverType.getBaseUrl();
		}
	}

	public String getParameter(EngineParameterType paramType) {
		return params.get(paramType.name());
	}

	public void putParameter(EngineParameterType paramType, String paramValue) {
		params.put(paramType.name(), paramValue);
	}

	public String getName(@NonNull Context ctx) {
		String customName = getParameter(EngineParameterType.CUSTOM_NAME);
		if (customName != null) {
			return customName;
		} else {
			return getStandardName(ctx);
		}
	}

	private String getStandardName(@NonNull Context ctx) {
		return getStandardName(ctx, serverType, vehicleKey);
	}

	public static String getStandardName(@NonNull Context ctx,
	                                     @NonNull ServerType serverType,
	                                     @NonNull String vehicleKey) {
		String vehicleTitle = VehicleType.toHumanString(ctx, vehicleKey);
		String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_dash);
		return String.format(pattern, serverType.getTitle(), vehicleTitle);
	}

	public static OnlineRoutingEngine createNewEngine(@NonNull ServerType serverType,
													  @NonNull String vehicleKey,
													  @Nullable Map<String, String> params) {
		return new OnlineRoutingEngine(generateKey(), serverType, vehicleKey, params);
	}

	private static String generateKey() {
		return ONLINE_ROUTING_ENGINE_PREFIX + System.currentTimeMillis();
	}
}
