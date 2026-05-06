package net.osmand.plus.plugins.weather;

import static net.osmand.plus.download.local.LocalItemType.WEATHER_DATA;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;

import org.apache.commons.logging.Log;

class UpdateWeatherCacheTask extends AsyncTask<Void, Void, Void> {

	private static final Log LOG = PlatformUtil.getLog(UpdateWeatherCacheTask.class);

	private final OsmandApplication app;
	@Nullable
	private final WeatherTileResourcesManager resourcesManager;
	@Nullable
	private final String filePath;

	UpdateWeatherCacheTask(@NonNull OsmandApplication app, @Nullable WeatherTileResourcesManager resourcesManager) {
		this(app, resourcesManager, null);
	}

	UpdateWeatherCacheTask(@NonNull OsmandApplication app, @Nullable WeatherTileResourcesManager resourcesManager, @Nullable String filePath) {
		this.app = app;
		this.resourcesManager = resourcesManager;
		this.filePath = filePath;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		if (filePath == null) {
			importForecastCache();
		} else {
			importForecastCache(filePath);
		}
		return null;
	}

	private void importForecastCache() {
		LocalIndexHelper helper = new LocalIndexHelper(app);
		for (LocalItem item : helper.getLocalIndexItems(true, false, null, WEATHER_DATA)) {
			importForecastCache(item.getPath());
		}
	}

	private void importForecastCache(@NonNull String filePath) {
		boolean updateForecastCache = false;
		if (resourcesManager != null) {
			updateForecastCache = resourcesManager.importDbCache(filePath);
		}
		LOG.info("updateForecastCache " + filePath + " success " + updateForecastCache);
	}
}