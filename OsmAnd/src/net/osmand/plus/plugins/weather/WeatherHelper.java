package net.osmand.plus.plugins.weather;

import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.plus.download.LocalIndexHelper.LocalIndexType.WEATHER_DATA;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.FINISHED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.BandIndexGeoBandSettingsHash;
import net.osmand.core.jni.GeoBandSettings;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.ZoomLevelDoubleListHash;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.download.LocalIndexHelper;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
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
		if (weatherTileResourcesManager != null) {
			return;
		}
		File cacheDir = getForecastCacheDir();
		String projResourcesPath = app.getAppPath(null).getAbsolutePath();
		int tileSize = 256;
		MapPresentationEnvironment mapPresentationEnvironment = mapRenderer.getMapPresentationEnvironment();
		float densityFactor = mapPresentationEnvironment.getDisplayDensityFactor();

		WeatherWebClient webClient = new WeatherWebClient();
		WeatherTileResourcesManager weatherTileResourcesManager = new WeatherTileResourcesManager(
				new BandIndexGeoBandSettingsHash(), cacheDir.getAbsolutePath(), projResourcesPath,
				tileSize, densityFactor, webClient.instantiateProxy(true)
		);
		webClient.swigReleaseOwnership();
		weatherTileResourcesManager.setBandSettings(getBandSettings(weatherTileResourcesManager));
		this.weatherTileResourcesManager = weatherTileResourcesManager;
		offlineForecastHelper.setWeatherResourcesManager(weatherTileResourcesManager);
	}

	public boolean shouldUpdateForecastCache() {
		File dir = getForecastCacheDir();
		return Algorithms.isEmpty(dir.listFiles());
	}

	public void updateForecastCache() {
		LocalIndexHelper helper = new LocalIndexHelper(app);
		List<LocalIndexInfo> indexData = helper.getLocalIndexData(true, false, null, WEATHER_DATA);
		if (!Algorithms.isEmpty(indexData)) {
			for (LocalIndexInfo indexInfo : indexData) {
				updateForecastCache(indexInfo.getPathToData());
			}
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

		Date date = OsmAndFormatter.getStartOfToday();
		long dateTime = date.getTime();
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

	private boolean updateBandsSettings(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		BandIndexGeoBandSettingsHash bandSettings = getBandSettings(weatherResourcesManager);
		weatherResourcesManager.setBandSettings(bandSettings);
		bandsSettingsVersion.incrementAndGet();
		return true;
	}

	@NonNull
	public BandIndexGeoBandSettingsHash getBandSettings(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
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
				MapPresentationEnvironment mapPresentationEnvironment =
						mapContext != null ? mapContext.getMapPresentationEnvironment() : null;
				ZoomLevelDoubleListHash contourLevels = band.getContourLevels(
						weatherResourcesManager, mapPresentationEnvironment);
				GeoBandSettings settings = new GeoBandSettings(unit, unitFormatGeneral, unitFormatPrecise,
						internalUnit, opacity, colorProfilePath, contourStyleName, contourLevels);
				bandSettings.set(band.getBandIndex(), settings);
			}
		}
		return bandSettings;
	}

	public int getBandsSettingsVersion() {
		return bandsSettingsVersion.get();
	}


	public static long roundForecastTimeToHour(long time) {
		long hour = 60 * 60 * 1000;
		return (time + hour / 2) / hour * hour;
	}

	public static boolean isWeatherSupported(@NonNull OsmandApplication app) {
		return app.getSettings().USE_OPENGL_RENDER.get() && Version.isOpenGlAvailable(app);
	}
}
