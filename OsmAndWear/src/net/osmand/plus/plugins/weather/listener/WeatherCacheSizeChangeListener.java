package net.osmand.plus.plugins.weather.listener;

/**
 * Notifies of any changes to the total size of cached "Online" or "Offline" weather data.
 */
public interface WeatherCacheSizeChangeListener {
	void onWeatherCacheSizeChanged();
}
