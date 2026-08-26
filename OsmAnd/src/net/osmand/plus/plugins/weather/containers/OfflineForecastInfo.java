package net.osmand.plus.plugins.weather.containers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OfflineForecastInfo {

	private final ConcurrentHashMap<InfoType, Object> infoMap = new ConcurrentHashMap<>();
	private final AtomicInteger downloadProgress = new AtomicInteger(0);

	public enum InfoType {
		LOCAL_SIZE,
		UPDATES_SIZE,
		SIZE_CALCULATED,
		PROGRESS_DOWNLOAD
	}

	@Nullable
	public Object get(@NonNull InfoType type) {
		if (type == InfoType.PROGRESS_DOWNLOAD) {
			return downloadProgress.get();
		}
		return infoMap.get(type);
	}

	public void put(@NonNull InfoType type, @NonNull Object value) {
		if (type == InfoType.PROGRESS_DOWNLOAD && value instanceof Integer) {
			downloadProgress.set((Integer) value);
		} else {
			infoMap.put(type, value);
		}
	}

	public boolean contains(@NonNull InfoType type) {
		if (type == InfoType.PROGRESS_DOWNLOAD) {
			return true;
		}
		return infoMap.containsKey(type);
	}

	public int incrementDownloadProgress() {
		return downloadProgress.incrementAndGet();
	}
}
