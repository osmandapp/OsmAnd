package net.osmand.plus.plugins.weather.containers;

import androidx.annotation.NonNull;

public class WeatherTotalCacheSize {

	private final ResetTotalWeatherCacheSizeListener resetSizeListener;
	private long onlineCacheSize = 0;
	private long offlineCacheSize = 0;
	private boolean onlineCacheCalculated;
	private boolean offlineCacheCalculated;

	public WeatherTotalCacheSize(@NonNull ResetTotalWeatherCacheSizeListener resetSizeListener) {
		this.resetSizeListener = resetSizeListener;
	}

	public long get(boolean forLocal) {
		return forLocal ? offlineCacheSize : onlineCacheSize;
	}

	public boolean isCalculated() {
		return onlineCacheCalculated && offlineCacheCalculated;
	}

	public boolean isCalculated(boolean forLocal) {
		return forLocal ? offlineCacheCalculated : onlineCacheCalculated;
	}

	public void set(long size, boolean forLocal) {
		if (forLocal) {
			offlineCacheSize = size;
			offlineCacheCalculated = true;
		} else {
			onlineCacheSize = size;
			onlineCacheCalculated = true;
		}
	}

	public void reset() {
		offlineCacheSize = 0;
		onlineCacheSize = 0;
		offlineCacheCalculated = false;
		onlineCacheCalculated = false;
		resetSizeListener.onResetTotalWeatherCacheSize();
	}

	public void reset(boolean forLocal) {
		if (forLocal) {
			offlineCacheSize = 0;
			offlineCacheCalculated = false;
		} else {
			onlineCacheSize = 0;
			onlineCacheCalculated = false;
		}
		resetSizeListener.onResetTotalWeatherCacheSize();
	}

	public interface ResetTotalWeatherCacheSizeListener {
		void onResetTotalWeatherCacheSize();
	}

}
