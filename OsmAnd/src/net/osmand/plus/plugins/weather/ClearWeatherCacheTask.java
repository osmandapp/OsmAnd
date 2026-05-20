package net.osmand.plus.plugins.weather;

import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.FINISHED;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize;
import net.osmand.plus.utils.OsmAndFormatter;

import java.util.List;

class ClearWeatherCacheTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final WeatherTotalCacheSize totalCacheSize;
	private final OfflineForecastHelper offlineForecastHelper;
	@Nullable
	private final WeatherTileResourcesManager resourcesManager;

	ClearWeatherCacheTask(@NonNull OsmandApplication app, @NonNull WeatherTotalCacheSize totalCacheSize,
			@NonNull OfflineForecastHelper offlineForecastHelper, @Nullable WeatherTileResourcesManager resourcesManager) {
		this.app = app;
		this.totalCacheSize = totalCacheSize;
		this.offlineForecastHelper = offlineForecastHelper;
		this.resourcesManager = resourcesManager;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		clearOutdatedCache();
		return null;
	}

	private void clearOutdatedCache() {
		totalCacheSize.reset();

		long dateTime = OsmAndFormatter.getStartOfToday();
		if (resourcesManager != null) {
			resourcesManager.clearDbCache(dateTime);
		}

		List<String> downloadedRegionIds = offlineForecastHelper.getTempForecastsWithDownloadStates(FINISHED);
		for (WorldRegion region : app.getRegions().getFlattenedWorldRegions()) {
			if (downloadedRegionIds.contains(region.getRegionId())) {
				offlineForecastHelper.calculateCacheSize(region, null);
			}
		}
	}
}