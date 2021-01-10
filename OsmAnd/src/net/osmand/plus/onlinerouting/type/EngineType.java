package net.osmand.plus.onlinerouting.type;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public abstract class EngineType {

	public abstract String getStringKey();

	public abstract String getTitle();

	public abstract String getStandardUrl();

	public String createFullUrl(OnlineRoutingEngine engine, List<LatLon> path) {
		StringBuilder sb = new StringBuilder(engine.getBaseUrl());
		createFullUrl(sb, engine, path);
		return sb.toString();
	}

	protected abstract void createFullUrl(StringBuilder sb,
	                                      OnlineRoutingEngine engine,
	                                      List<LatLon> path);

	public abstract List<LatLon> parseResponse(@NonNull String content) throws JSONException;

	public static EngineType[] values() {
		EngineType[] types = new EngineType[] {
				new GraphhoperEngine(),
				new OsrmEngine()
		};
		return types;
	}

	public static EngineType valueOf(String key) {
		EngineType[] values = values();
		for (EngineType type : values) {
			if (Algorithms.objectEquals(type.getStringKey(), key)) {
				return type;
			}
		}
		return values[0];
	}
}
