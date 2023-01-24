package net.osmand.plus.plugins.weather;

public class WeatherCacheSize {

	private long onlineCacheSize = 0;
	private long offlineCacheSize = 0;

	public long get(boolean forLocal) {
		return forLocal ? offlineCacheSize : onlineCacheSize;
	}

	public void set(long size, boolean forLocal) {
		if (forLocal) {
			offlineCacheSize = size;
		} else {
			onlineCacheSize = size;
		}
	}

	public void reset() {
		reset(true);
		reset(false);
	}

	public void reset(boolean forLocal) {
		if (forLocal) {
			offlineCacheSize = 0;
		} else {
			onlineCacheSize = 0;
		}
	}

}
