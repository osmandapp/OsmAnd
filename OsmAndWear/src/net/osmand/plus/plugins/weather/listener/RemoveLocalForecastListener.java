package net.osmand.plus.plugins.weather.listener;

/**
 * Notifies about start and finish of the offline weather tile removal process.
 */
public interface RemoveLocalForecastListener {
	void onRemoveLocalForecastEvent();
}
