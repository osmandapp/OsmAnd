package net.osmand.plus.onlinerouting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class OnlineRoutingEngine {

	public enum EngineParameterType {
		CUSTOM_SERVER_URL,
		API_KEY
	}

	private String stringKey;
	private String name;
	private ServerType serverType;
	private String vehicleKey;
	private Map<String, String> params;

	public OnlineRoutingEngine(@Nullable String stringKey,
	                           @NonNull String name,
	                           @NonNull ServerType serverType,
	                           @NonNull String vehicleKey,
	                           Map<String, String> params) {
		if (stringKey == null) {
			stringKey = generateKey(vehicleKey);
		}
		this.stringKey = stringKey;
		this.name = name;
		this.serverType = serverType;
		this.vehicleKey = vehicleKey;
		this.params = params;
	}

	public String getStringKey() {
		return stringKey;
	}

	public String getName() {
		return name;
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
		if (params != null) {
			return params.get(paramType.name());
		}
		return null;
	}

	public void putParameter(EngineParameterType paramType, String paramValue) {
		if (params == null) {
			params = new HashMap<>();
		}
		params.put(paramType.name(), paramValue);
	}

	private static String generateKey(String vehicleKey) {
		return "online_" + vehicleKey + "_" + System.currentTimeMillis();
	}
}
