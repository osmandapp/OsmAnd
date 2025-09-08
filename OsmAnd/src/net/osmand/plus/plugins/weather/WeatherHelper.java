package net.osmand.plus.plugins.weather;

import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.plus.download.local.LocalItemType.WEATHER_DATA;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_ANIMATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.FINISHED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.jni.BandIndexGeoBandSettingsHash;
import net.osmand.core.jni.GeoBandSettings;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.ZoomLevelDoubleListHash;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.enums.WeatherSource;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.plugins.weather.WeatherWebClient.DownloadState;
import net.osmand.plus.plugins.weather.WeatherWebClient.WeatherWebClientListener;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.settings.enums.TemperatureUnitsMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WeatherHelper {

	private static final Log log = PlatformUtil.getLog(WeatherHelper.class);
	private final OsmandApplication app;
	private final WeatherSettings weatherSettings;
	private final OfflineForecastHelper offlineForecastHelper;
	private final Map<Short, WeatherBand> weatherBands = new LinkedHashMap<>();
	private final AtomicInteger bandsSettingsVersion = new AtomicInteger(0);
	private final WeatherTotalCacheSize totalCacheSize;
	private List<WeakReference<WeatherWebClientListener>> downloadStateListeners = new ArrayList<>();
	private final StateChangedListener<TemperatureUnitsMode> temperaturePreferenceListener = weatherUnit -> updateBandsSettings();
	private WeatherWebClient webClient;

	private WeatherTileResourcesManager weatherTileResourcesManager;

	public WeatherHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.weatherSettings = new WeatherSettings(app);
		this.offlineForecastHelper = new OfflineForecastHelper(app);
		this.totalCacheSize = offlineForecastHelper.getTotalCacheSize();

		weatherBands.put(WEATHER_BAND_TEMPERATURE, WeatherBand.withWeatherBand(app, WEATHER_BAND_TEMPERATURE));
		weatherBands.put(WEATHER_BAND_PRESSURE, WeatherBand.withWeatherBand(app, WEATHER_BAND_PRESSURE));
		weatherBands.put(WEATHER_BAND_WIND_SPEED, WeatherBand.withWeatherBand(app, WEATHER_BAND_WIND_SPEED));
		weatherBands.put(WEATHER_BAND_CLOUD, WeatherBand.withWeatherBand(app, WEATHER_BAND_CLOUD));
		weatherBands.put(WEATHER_BAND_PRECIPITATION, WeatherBand.withWeatherBand(app, WEATHER_BAND_PRECIPITATION));
		weatherBands.put(WEATHER_BAND_WIND_ANIMATION, WeatherBand.withWeatherBand(app, WEATHER_BAND_WIND_ANIMATION));

		app.getSettings().UNIT_OF_TEMPERATURE.addListener(temperaturePreferenceListener);
	}

	@NonNull
	public WeatherSettings getWeatherSettings() {
		return weatherSettings;
	}

	@NonNull
	public List<WeatherBand> getWeatherBands() {
		return new ArrayList<>(weatherBands.values());
	}

	public boolean hasVisibleBands() {
		return !Algorithms.isEmpty(getVisibleBands());
	}

	@NonNull
	public List<WeatherBand> getVisibleBands() {
		List<WeatherBand> bands = new ArrayList<>();
		for (WeatherBand band : weatherBands.values()) {
			if (band.isBandVisible()) {
				bands.add(band);
			}
		}
		return bands;
	}

	@NonNull
	public List<WeatherBand> getVisibleForecastBands() {
		List<WeatherBand> bands = new ArrayList<>();
		for (WeatherBand band : weatherBands.values()) {
			if (band.isForecastBandVisible()) {
				bands.add(band);
			}
		}
		return bands;
	}

	@Nullable
	public WeatherBand getWeatherBand(short bandIndex) {
		return weatherBands.get(bandIndex);
	}

	public void updateMapPresentationEnvironment(@NonNull MapRendererContext mapRenderer) {
		MapPresentationEnvironment environment = mapRenderer.getMapPresentationEnvironment();
		if (weatherTileResourcesManager != null || environment == null) {
			return;
		}
		File cacheDir = getForecastCacheDir();
		String projResourcesPath = app.getAppPath(null).getAbsolutePath();
		int tileSize = 256;
		float densityFactor = environment.getDisplayDensityFactor();
		if (webClient != null) {
			webClient.cleanupResources();
		}
		webClient = new WeatherWebClient();
		WeatherTileResourcesManager weatherTileResourcesManager = new WeatherTileResourcesManager(
				new BandIndexGeoBandSettingsHash(), cacheDir.getAbsolutePath(), projResourcesPath,
				tileSize, densityFactor, webClient.instantiateProxy(true)
		);
		webClient.setDownloadStateListener(this::onDownloadStateChanged);
		webClient.swigReleaseOwnership();
		weatherTileResourcesManager.setBandSettings(getBandSettings(weatherTileResourcesManager));
		this.weatherTileResourcesManager = weatherTileResourcesManager;
		offlineForecastHelper.setWeatherResourcesManager(weatherTileResourcesManager);
		
		updateWeatherSource();
	}

	public boolean shouldUpdateForecastCache() {
		File dir = getForecastCacheDir();
		return Algorithms.isEmpty(dir.listFiles());
	}

	public void updateForecastCache() {
		LocalIndexHelper helper = new LocalIndexHelper(app);
		for (LocalItem item : helper.getLocalIndexItems(true, false, null, WEATHER_DATA)) {
			updateForecastCache(item.getPath());
		}
	}

	public void updateForecastCache(@NonNull String filePath) {
		boolean updateForecastCache = false;
		if (weatherTileResourcesManager != null) {
			updateForecastCache = weatherTileResourcesManager.importDbCache(filePath);
		}
		log.info("updateForecastCache " + filePath + " success " + updateForecastCache);
	}

	@NonNull
	private File getForecastCacheDir() {
		File dir = new File(app.getCacheDir(), WEATHER_FORECAST_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public void clearOutdatedCache() {
		totalCacheSize.reset();

		long dateTime = OsmAndFormatter.getStartOfToday();
		weatherTileResourcesManager.clearDbCache(dateTime);

		List<String> downloadedRegionIds = offlineForecastHelper.getTempForecastsWithDownloadStates(FINISHED);
		for (WorldRegion region : app.getRegions().getFlattenedWorldRegions()) {
			if (downloadedRegionIds.contains(region.getRegionId())) {
				offlineForecastHelper.calculateCacheSize(region, null);
			}
		}
	}

	@Nullable
	public WeatherTileResourcesManager getWeatherResourcesManager() {
		return weatherTileResourcesManager;
	}

	@NonNull
	public OfflineForecastHelper getOfflineForecastHelper() {
		return offlineForecastHelper;
	}

	public boolean updateBandsSettings() {
		WeatherTileResourcesManager weatherResourcesManager = getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return false;
		}
		return updateBandsSettings(weatherResourcesManager);
	}

	public void updateWeatherSource() {
		WeatherTileResourcesManager weatherResourcesManager = getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return;
		}
		
		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (plugin == null) {
			return;
		}
		
		WeatherSource weatherSource = plugin.getWeatherSource();
		net.osmand.core.jni.WeatherSource coreWeatherSource;
		
		if (weatherSource == WeatherSource.ECMWF) {
			coreWeatherSource = net.osmand.core.jni.WeatherSource.ECMWF;
		} else {
			coreWeatherSource = net.osmand.core.jni.WeatherSource.GFS;
		}
		
		weatherResourcesManager.setWeatherSource(coreWeatherSource);
	}

	private boolean updateBandsSettings(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		BandIndexGeoBandSettingsHash bandSettings = getBandSettings(weatherResourcesManager);
		weatherResourcesManager.setBandSettings(bandSettings);
		bandsSettingsVersion.incrementAndGet();
		return true;
	}

	@NonNull
	public BandIndexGeoBandSettingsHash getBandSettings(@NonNull WeatherTileResourcesManager resourcesManager) {
		BandIndexGeoBandSettingsHash bandSettings = new BandIndexGeoBandSettingsHash();

		for (WeatherBand band : weatherBands.values()) {
			WeatherUnit weatherUnit = band.getBandUnit();
			if (weatherUnit != null) {
				String unit = weatherUnit.getSymbol();
				String unitFormatGeneral = band.getBandGeneralUnitFormat();
				String unitFormatPrecise = band.getBandPreciseUnitFormat();
				String internalUnit = band.getInternalBandUnit();
				float opacity = band.getBandOpacity();
				String contourStyleName = band.getContourStyleName();
				String colorProfilePath = app.getAppPath(band.getColorFilePath()).getAbsolutePath();
				MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
				MapPresentationEnvironment environment = mapContext != null ? mapContext.getMapPresentationEnvironment() : null;
				ZoomLevelDoubleListHash contourLevels = band.getContourLevels(resourcesManager, environment);

				GeoBandSettings settings = new GeoBandSettings(unit, unitFormatGeneral, unitFormatPrecise,
						internalUnit, opacity, colorProfilePath, contourStyleName, contourLevels);
				bandSettings.set(band.getBandIndex(), settings);
			}
		}
		return bandSettings;
	}

	private void onDownloadStateChanged(@NonNull DownloadState downloadState, int activeRequestsCounter) {
		List<WeakReference<WeatherWebClientListener>> listeners = downloadStateListeners;
		for (WeakReference<WeatherWebClientListener> ref : listeners) {
			WeatherWebClientListener listener = ref.get();
			if (listener != null) {
				listener.onDownloadStateChanged(downloadState, activeRequestsCounter);
			}
		}
	}

	public void addDownloadStateListener(@NonNull WeatherWebClientListener listener) {
		downloadStateListeners = Algorithms.updateWeakReferencesList(downloadStateListeners, listener, true);
	}

	public void removeDownloadStateListener(@NonNull WeatherWebClientListener listener) {
		downloadStateListeners = Algorithms.updateWeakReferencesList(downloadStateListeners, listener, false);
	}

	public int getActiveRequestsCount() {
		return webClient != null ? webClient.getActiveRequestsCount() : 0;
	}

	public boolean isProcessingTiles() {
		return weatherTileResourcesManager != null && weatherTileResourcesManager.isProcessingTiles();
	}

	public int getBandsSettingsVersion() {
		return bandsSettingsVersion.get();
	}
}
