package net.osmand.plus.plugins.weather.containers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class OfflineForecastInfo {

	private final Map<InfoType, Object> infoMap = new HashMap<>();

	public enum InfoType {
		LOCAL_SIZE,
		UPDATES_SIZE,
		SIZE_CALCULATED,
		PROGRESS_DOWNLOAD
	}

	@Nullable
	public Object get(@NonNull InfoType type) {
		return infoMap.get(type);
	}

	public void put(@NonNull InfoType type, @NonNull Object value) {
		infoMap.put(type, value);
	}

	public boolean contains(@NonNull InfoType type) {
		return infoMap.containsKey(type);
	}
}
