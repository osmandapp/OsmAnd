package net.osmand.plus.plugins.weather;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static net.osmand.map.WorldRegion.RUSSIA_REGION_ID;
import static net.osmand.map.WorldRegion.WORLD;
import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;
import static net.osmand.plus.helpers.FileNameTranslationHelper.getWeatherName;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.LOCAL_SIZE;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.PROGRESS_DOWNLOAD;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.SIZE_CALCULATED;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.UPDATES_SIZE;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.FINISHED;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.IN_PROGRESS;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.UNDEFINED;
import static net.osmand.plus.plugins.weather.WeatherUtils.getRegionBounds;
import static net.osmand.plus.utils.OsmAndFormatter.getTimeForTimeZone;

import android.os.AsyncTask;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.OnCompleteCallback;
import net.osmand.PlatformUtil;
import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.SWIGTYPE_p_std__shared_ptrT_Metric_t;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.TileIdList;
import net.osmand.core.jni.TileIdVector;
import net.osmand.core.jni.WeatherTileResourceProvider;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherTileResourcesManager.DownloadGeoTileRequest;
import net.osmand.core.jni.WeatherTileResourcesManager.IDownloadGeoTilesAsyncCallback;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_IQueryController;
import net.osmand.data.QuadRect;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.containers.OfflineForecastInfo;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize.ResetTotalWeatherCacheSizeListener;
import net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState;
import net.osmand.plus.plugins.weather.enums.WeatherForecastUpdatesFrequency;
import net.osmand.plus.plugins.weather.listener.RemoveLocalForecastListener;
import net.osmand.plus.plugins.weather.indexitem.WeatherIndexItem;
import net.osmand.plus.plugins.weather.listener.WeatherCacheSizeChangeListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineForecastHelper implements ResetTotalWeatherCacheSizeListener {

	private static final Log LOG = PlatformUtil.getLog(OfflineForecastHelper.class);

	private static final String PREF_FORECAST_DOWNLOAD_STATE_PREFIX = "forecast_download_state_";
	private static final String PREF_FORECAST_LAST_UPDATE_PREFIX = "forecast_last_update_";
	private static final String PREF_FORECAST_FREQUENCY_PREFIX = "forecast_frequency_";
	private static final String PREF_FORECAST_WIFI_PREFIX = "forecast_download_via_wifi_";

	public static final int TILE_SIZE = 40_000;
	public static final int FORECAST_DATES_COUNT = 24 + (6 * 8) + 1;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private WeatherTileResourcesManager weatherResourcesManager;

	private final Map<String, OfflineForecastInfo> offlineForecastInfo;
	private final Map<String, WeatherIndexItem> cachedWeatherIndexes;
	private final WeatherTotalCacheSize totalCacheSize;
	private List<WeatherCacheSizeChangeListener> weatherCacheSizeChangeListeners = new ArrayList<>();
	private boolean clearOnlineCacheInProgress;
	private boolean totalCacheSizeCalculationInProgress;
	private List<RemoveLocalForecastListener> removeLocalForecastListeners = new ArrayList<>();
	private List<String> regionsRemoveInProgress = new ArrayList<>();

	public OfflineForecastHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		cachedWeatherIndexes = new HashMap<>();
		offlineForecastInfo = new HashMap<>();
		this.totalCacheSize = new WeatherTotalCacheSize(this);
	}

	public void setWeatherResourcesManager(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		this.weatherResourcesManager = weatherResourcesManager;
	}

	public boolean shouldHaveWeatherForecast(@NonNull WorldRegion region) {
		String regionId = region.getRegionId();
		int level = region.getLevel();

		boolean russia = RUSSIA_REGION_ID.equals(regionId);
		boolean russiaPrefix = regionId.startsWith(RUSSIA_REGION_ID);
		boolean unitedKingdom = regionId.equals(WorldRegion.UNITED_KINGDOM_REGION_ID);

		return WORLD.equals(regionId) ||
				(level == 1 && russia) ||
				(level > 1 && !russiaPrefix && ((level == 2 && !unitedKingdom) || (level == 3 && unitedKingdom)));
	}

	public void checkAndDownloadForecastsByRegionIds(List<String> regionIds) {
		if (!settings.isInternetConnectionAvailable()) {
			return;
		}
		int forecastDownloading = 0;
		for (WorldRegion region : app.getRegions().getFlattenedWorldRegions()) {
			String regionId = region.getRegionId();
			if (regionIds.contains(regionId)) {
				forecastDownloading++;
				if (!settings.isWifiConnected() && getPreferenceWifi(regionId)) {
					continue;
				}
				long lastUpdateTime = getPreferenceLastUpdate(regionId);
				long nowTime = System.currentTimeMillis();
				WeatherForecastUpdatesFrequency updatesFrequency = getPreferenceFrequency(regionId);
				int secondsRequired = updatesFrequency.getSecondsRequired();
				if (nowTime >= lastUpdateTime + secondsRequired) {
					downloadForecastByRegion(region);
				}
			}
			if (forecastDownloading == regionIds.size()) {
				break;
			}
		}
	}

	public void downloadForecastsByRegionIds(@NonNull List<String> regionIds) {
		int forecastsDownloading = 0;
		for (WorldRegion region : app.getRegions().getFlattenedWorldRegions()) {
			String regionId = region.getRegionId();
			if (regionIds.contains(regionId)) {
				downloadForecastByRegion(region);
				forecastsDownloading++;
			}
			if (forecastsDownloading == regionIds.size()) {
				break;
			}
		}
	}

	public boolean downloadForecastByRegion(@NonNull WorldRegion region) {
		return downloadForecastByRegion(region, null);
	}

	public boolean downloadForecastByRegion(@NonNull WorldRegion region, @Nullable IProgress progress) {
		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (plugin == null || !plugin.isActive()) {
			return false;
		}
		String regionId = region.getRegionId();
		if (!settings.isInternetConnectionAvailable()) {
			return false;
		}
		if (!settings.isWifiConnected() && getPreferenceWifi(regionId)) {
			return false;
		}

		QuadRect regionBounds = getRegionBounds(region);
		LatLon topLeft = new LatLon(regionBounds.top, regionBounds.left);
		LatLon bottomRight = new LatLon(regionBounds.bottom, regionBounds.right);

		setOfflineForecastProgressInfo(regionId, 0);
		setPreferenceDownloadState(regionId, WeatherForecastDownloadState.IN_PROGRESS);

		onDownloadStarted(region, progress);

		interface_IQueryController queryController = new interface_IQueryController() {
			@Override
			public boolean isAborted() {
				return getPreferenceDownloadState(regionId) != WeatherForecastDownloadState.IN_PROGRESS;
			}
		};

		Date date = OsmAndFormatter.getStartOfToday();
		long dateTime = date.getTime();
		for (int i = 0; i < FORECAST_DATES_COUNT; i++) {
			if (!isDownloadStateInProgress(regionId)) {
				// download was canceled by user
				break;
			}
			DownloadGeoTileRequest request = new DownloadGeoTileRequest();
			request.setDateTime(dateTime);
			request.setTopLeft(topLeft);
			request.setBottomRight(bottomRight);
			request.setForceDownload(true);
			request.setLocalData(true);
			request.setQueryController(queryController.instantiateProxy());
			IDownloadGeoTilesAsyncCallback callback = new IDownloadGeoTilesAsyncCallback() {
				@Override
				public void method(boolean succeeded,
				                   BigInteger downloadedTiles,
				                   BigInteger totalTiles,
				                   SWIGTYPE_p_std__shared_ptrT_Metric_t metric) {
					onUpdateDownloadProgress(region, progress, succeeded);
				}
			};
			queryController.swigReleaseOwnership();
			callback.swigReleaseOwnership();
			weatherResourcesManager.downloadGeoTiles(request, callback.getBinding());
			dateTime += HOUR_IN_MILLIS * (i < 24 ? 1 : 3);
		}

		return isDownloadStateFinished(regionId);
	}

	public void checkAndStopWeatherDownload(@NonNull WeatherIndexItem weatherIndexItem) {
		String regionId = weatherIndexItem.getRegionId();
		prepareToStopDownloading(regionId);

		if (isDownloadStateUndefined(regionId)) {
			removeLocalForecastAsync(regionId, false, false);
		} else if (isDownloadStateFinished(regionId)) {
			calculateCacheSize(weatherIndexItem.getRegion(), null);
		}
	}

	public void prepareToStopDownloading(@NonNull String regionId) {
		totalCacheSize.reset(true);
		if (isDownloadStateInProgress(regionId)) {
			setPreferenceDownloadState(regionId, UNDEFINED);
			if (getPreferenceLastUpdate(regionId) == -1) {
				removeOfflineForecastInfo(regionId);
			} else {
				setPreferenceDownloadState(regionId, FINISHED);
				int destination = getProgressDestination(regionId);
				setOfflineForecastProgressInfo(regionId, destination);
			}
		}
	}

	public void calculateCacheSizeIfNeeded(@NonNull WeatherIndexItem indexItem, @Nullable OnCompleteCallback callback) {
		String regionId = indexItem.getRegionId();
		if (!isOfflineForecastSizesInfoCalculated(regionId)) {
			calculateCacheSize(indexItem.getRegion(), () -> {
				DecimalFormat decimalFormat = new DecimalFormat("#.#");
				long contentSize = getOfflineForecastSizeInfo(regionId, true);
				long containerSize = getOfflineForecastSizeInfo(regionId, false);
				String size = decimalFormat.format(containerSize / (1024f * 1024f));
				indexItem.updateSize(size, contentSize, containerSize);
				notifyOnComplete(callback);
			});
		} else {
			notifyOnComplete(callback);
		}
	}

	public void calculateCacheSize(@NonNull WorldRegion region, @Nullable OnCompleteCallback callback) {
		String regionId = region.getRegionId();
		setOfflineForecastSizeInfo(regionId, 0, true);
		setOfflineForecastSizeInfo(regionId, 0, false);
		setOfflineForecastSizesInfoCalculated(regionId, false);
		runAsync(() -> {
			List<Long> tileIds = getTileIds(region);
			TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
			ZoomLevel zoom = getGeoTileZoom();
			if (!qTileIds.isEmpty()) {
				long localSize = weatherResourcesManager.calculateDbCacheSize(qTileIds, new TileIdList(), zoom).longValue();
				setOfflineForecastSizeInfo(regionId, localSize, true);
				setOfflineForecastSizeInfo(regionId, calculateApproxUpdatesSize(tileIds), false);
				setOfflineForecastSizesInfoCalculated(regionId, true);
			}
			runInUiThread(() -> {
				if (callback != null) {
					callback.onComplete();
				}
			});
		});
	}

	private int calculateApproxUpdatesSize(@NonNull List<Long> tileIds) {
		return tileIds.size() * FORECAST_DATES_COUNT * TILE_SIZE;
	}

	public void calculateTotalCacheSizeAsync(boolean forceCalculation) {
		if ((totalCacheSize.isCalculated() && !forceCalculation)) {
			// calculation is not required
			return;
		}
		if (isTotalCacheSizeCalculationInProgress() || isClearOnlineCacheInProgress()) {
			return;
		}
		runAsync(() -> {
			totalCacheSizeCalculationInProgress = true;
			notifyOnWeatherCacheSizeChanged();

			calculateTotalCacheSize(false, forceCalculation);
			calculateTotalCacheSize(true, forceCalculation);

			totalCacheSizeCalculationInProgress = false;
			notifyOnWeatherCacheSizeChanged();
		});
	}

	private void calculateTotalCacheSize(boolean forLocal, boolean forceCalculation) {
		List<Long> offlineTileIds = getOfflineTileIds();
		TileIdList qOfflineTileIds = NativeUtilities.convertToQListTileIds(offlineTileIds);
		ZoomLevel zoom = getGeoTileZoom();
		if (!totalCacheSize.isCalculated(forLocal) || forceCalculation) {
			if (forLocal && qOfflineTileIds.isEmpty()) {
				// skip calculation if no offline tiles are downloaded
				totalCacheSize.set(0, true);
			} else {
				TileIdList tileIds = forLocal ? qOfflineTileIds : new TileIdList();
				TileIdList excludeIds = forLocal ? new TileIdList() : qOfflineTileIds;
				long size = weatherResourcesManager.calculateDbCacheSize(tileIds, excludeIds, zoom).longValue();
				totalCacheSize.set(size, forLocal);
			}
		}
		// notify after cache was calculated
		notifyOnWeatherCacheSizeChanged();
	}

	public boolean isTotalCacheSizeCalculationInProgress() {
		return totalCacheSizeCalculationInProgress;
	}

	public void clearOnlineCacheAsync() {
		runAsync(this::clearOnlineCache);
	}

	private void clearOnlineCache() {
		clearOnlineCacheInProgress = true;
		totalCacheSize.reset(false);

		List<Long> offlineTileIds = getOfflineTileIds();
		TileIdList qOfflineTileIds = NativeUtilities.convertToQListTileIds(offlineTileIds);
		ZoomLevel zoom = getGeoTileZoom();
		// remove all tiles except related to offline weather forecast
		weatherResourcesManager.clearDbCache(new TileIdList(), qOfflineTileIds, zoom);
		runInUiThread(this::updateWeatherLayers);

		clearOnlineCacheInProgress = false;
		notifyOnWeatherCacheSizeChanged();
	}

	public void clearOfflineCacheAsync(@Nullable List<String> regionIds) {
		runAsync(() -> clearOfflineCache(regionIds));
	}

	private void clearOfflineCache(@Nullable List<String> regionIds) {
		if (Algorithms.isEmpty(regionIds)) {
			regionIds = getTempForecastsWithDownloadStates(IN_PROGRESS, FINISHED);
		}
		for (String regionId : regionIds) {
			setOfflineForecastSizeInfo(regionId, 0, true);
			prepareToStopDownloading(regionId);
		}
		totalCacheSize.reset(true);

		List<Long> offlineTileIds = getOfflineTileIds(regionIds);
		TileIdList qOfflineTileIds = NativeUtilities.convertToQListTileIds(offlineTileIds);
		ZoomLevel zoom = getGeoTileZoom();
		weatherResourcesManager.clearDbCache(qOfflineTileIds, new TileIdList(), zoom);
		runInUiThread(this::updateWeatherLayers);
	}

	public boolean canClearOnlineCache() {
		if (!isClearOnlineCacheInProgress()) {
			return totalCacheSize.isCalculated(false)
					&& totalCacheSize.get(false) > 0;
		}
		return false;
	}

	@NonNull
	public WeatherTotalCacheSize getTotalCacheSize() {
		return totalCacheSize;
	}

	public boolean isClearOnlineCacheInProgress() {
		return clearOnlineCacheInProgress;
	}

	@Override
	public void onResetTotalWeatherCacheSize() {
		notifyOnWeatherCacheSizeChanged();
	}

	public void notifyOnWeatherCacheSizeChanged() {
		runInUiThread(() -> {
			for (WeatherCacheSizeChangeListener listener : weatherCacheSizeChangeListeners) {
				listener.onWeatherCacheSizeChanged();
			}
		});
	}

	public void registerWeatherCacheSizeChangeListener(@NonNull WeatherCacheSizeChangeListener listener) {
		weatherCacheSizeChangeListeners = Algorithms.addToList(weatherCacheSizeChangeListeners, listener);
	}

	public void unregisterWeatherCacheSizeChangeListener(@NonNull WeatherCacheSizeChangeListener listener) {
		weatherCacheSizeChangeListeners = Algorithms.removeFromList(weatherCacheSizeChangeListeners, listener);
	}

	public void removeLocalForecastAsync(@NonNull String regionId, boolean refreshMap, boolean notifyUserOnFinish) {
		runAsync(() -> removeLocalForecast(new String[]{regionId}, refreshMap, notifyUserOnFinish));
	}

	public void removeLocalForecast(@NonNull String[] regionIds, boolean refreshMap, boolean notifyUserOnFinish) {
		List<String> regionIdsList = Arrays.asList(regionIds);
		regionsRemoveInProgress = Algorithms.addAllToList(regionsRemoveInProgress, regionIdsList);

		// notify before remove and after region ids were registered
		runInUiThread(this::notifyOnRemoveLocalForecastEvent);

		totalCacheSize.reset();
		List<Long> tileIds = new ArrayList<>();
		for (String regionId : regionIds) {
			List<Long> regionTileIds = getTileIds(regionId);
			if (regionTileIds == null) {
				continue;
			}
			for (Long tileId : regionTileIds) {
				if (!tileIds.contains(tileId) && !isContainsInOfflineRegions(tileId, regionId)) {
					tileIds.add(tileId);
				}
			}
			removePreferences(regionId);
			removeOfflineForecastInfo(regionId);
			setOfflineForecastSizeInfo(regionId, calculateApproxUpdatesSize(regionTileIds), false);
		}

		TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
		ZoomLevel zoom = getGeoTileZoom();
		if (!qTileIds.isEmpty()) {
			weatherResourcesManager.clearDbCache(qTileIds, new TileIdList(), zoom);
		}
		if (notifyUserOnFinish) {
			for (String regionId : regionIds) {
				StringBuilder fileName = new StringBuilder()
						.append(getWeatherName(app, app.getRegions(), regionId)).append(" ")
						.append(DownloadActivityType.WEATHER_FORECAST.getString(app));
				app.showToastMessage(app.getString(R.string.item_deleted, fileName));
			}
		}
		regionsRemoveInProgress = Algorithms.removeAllFromList(regionsRemoveInProgress, regionIdsList);
		app.getDownloadThread().updateLoadedFiles();

		// notify after remove was completed and region ids were unregistered
		runInUiThread(this::notifyOnRemoveLocalForecastEvent);

		if (refreshMap) {
			runInUiThread(this::updateWeatherLayers);
		}
	}

	public boolean isRemoveLocalForecastInProgress(@NonNull String regionId) {
		return regionsRemoveInProgress.contains(regionId);
	}

	public void notifyOnRemoveLocalForecastEvent() {
		for (RemoveLocalForecastListener listener : removeLocalForecastListeners) {
			listener.onRemoveLocalForecastEvent();
		}
	}

	public void registerRemoveLocalForecastListener(@NonNull RemoveLocalForecastListener listener) {
		removeLocalForecastListeners = Algorithms.addToList(removeLocalForecastListeners, listener);
	}

	public void unregisterRemoveLocalForecastListener(@NonNull RemoveLocalForecastListener listener) {
		removeLocalForecastListeners = Algorithms.removeFromList(removeLocalForecastListeners, listener);
	}

	public List<Long> getOfflineTileIds() {
		return getOfflineTileIds(null);
	}

	@NonNull
	public List<Long> getOfflineTileIds(List<String> regionIds) {
		if (regionIds == null) {
			regionIds = getTempForecastsWithDownloadStates(IN_PROGRESS, FINISHED);
		}
		List<Long> offlineTileIds = new ArrayList<>();
		for (String regionId : regionIds) {
			List<Long> regionTileIds = getTileIds(regionId);
			if (regionTileIds != null) {
				for (Long tileId : regionTileIds) {
					if (!offlineTileIds.contains(tileId)) {
						offlineTileIds.add(tileId);
					}
				}
			}
		}
		return offlineTileIds;
	}

	public boolean isForecastOutdated(@NonNull String regionId) {
		if (isDownloadStateFinished(regionId)) {
			int daysGone = 0;
			long lastUpdate = getPreferenceLastUpdate(regionId);
			if (lastUpdate != -1) {
				Date dayNow = OsmAndFormatter.getStartOfToday();
				long passedTime = dayNow.getTime() - lastUpdate;
				daysGone = (int) (passedTime / DateUtils.DAY_IN_MILLIS);
			}
			return daysGone >= 7;
		}
		return false;
	}

	public void firstInitForecast(@NonNull String regionId) {
		if (isDownloadStateInProgress(regionId)) {
			if (getPreferenceLastUpdate(regionId) == -1) {
				removeLocalForecastAsync(regionId, false, false);
			}
		} else if (isDownloadStateFinished(regionId)) {
			int destination = getProgressDestination(regionId);
			setOfflineForecastProgressInfo(regionId, destination);
		}
	}

	public boolean isContainsInOfflineRegions(@NonNull Long tileId, @NonNull String excludeRegionId) {
		List<String> regionIds = getTempForecastsWithDownloadStates(IN_PROGRESS, FINISHED);
		for (String regionId : regionIds) {
			if (!regionId.equals(excludeRegionId)) {
				List<Long> regionTileIds = getTileIds(regionId);
				if (regionTileIds != null) {
					for (Long offlineTileId : regionTileIds) {
						if (offlineTileId.equals(tileId)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public List<String> getTempForecastsWithDownloadStates(@NonNull WeatherForecastDownloadState... states) {
		List<String> forecasts = new ArrayList<>();
		List<WeatherForecastDownloadState> statesList = Arrays.asList(states);
		for (String regionId : getFlattenedWorldRegionIds()) {
			if (statesList.contains(getPreferenceDownloadState(regionId))) {
				forecasts.add(regionId);
			}
		}
		return forecasts;
	}

	private List<String> getFlattenedWorldRegionIds() {
		OsmandRegions regions = app.getRegions();
		return regions.getFlattenedWorldRegionIds();
	}

	public void removeOfflineForecastInfo(@NonNull String regionId) {
		offlineForecastInfo.remove(regionId);
	}

	public void setOfflineForecastSizeInfo(@NonNull String regionId, long size, boolean local) {
		OfflineForecastInfo info = getOrCreateCachedInfo(regionId);
		info.put(local ? LOCAL_SIZE : UPDATES_SIZE, size);
	}

	public long getOfflineForecastSizeInfo(@NonNull String regionId, boolean local) {
		OfflineForecastInfo info = getCachedInfo(regionId);
		if (info != null) {
			Object size = info.get(local ? LOCAL_SIZE : UPDATES_SIZE);
			if (size instanceof Long) {
				return (long) size;
			}
		}
		return 0;
	}

	public void setOfflineForecastSizesInfoCalculated(@NonNull String regionId, boolean value) {
		OfflineForecastInfo info = getOrCreateCachedInfo(regionId);
		info.put(SIZE_CALCULATED, value);
	}

	public boolean isOfflineForecastSizesInfoCalculated(@NonNull String regionId) {
		OfflineForecastInfo info = getCachedInfo(regionId);
		if (info != null) {
			Object sizeCalculated = info.get(SIZE_CALCULATED);
			if (sizeCalculated instanceof Boolean) {
				return (boolean) sizeCalculated;
			}
		}
		return false;
	}

	public void setOfflineForecastProgressInfo(@NonNull String regionId, int value) {
		OfflineForecastInfo info = getOrCreateCachedInfo(regionId);
		info.put(PROGRESS_DOWNLOAD, value);
	}

	public int getOfflineForecastProgressInfo(@NonNull String regionId) {
		OfflineForecastInfo info = getCachedInfo(regionId);
		if (info != null) {
			Object progress = info.get(PROGRESS_DOWNLOAD);
			if (progress instanceof Integer) {
				return (int) progress;
			}
		}
		return 0;
	}

	public int getProgressDestination(@NonNull String regionId) {
		List<Long> tileIds = getTileIds(regionId);
		return tileIds != null ? tileIds.size() * FORECAST_DATES_COUNT : -1;
	}

	public void onDownloadStarted(@NonNull WorldRegion region, @Nullable IProgress progress) {
		if (progress != null) {
			String regionId = region.getRegionId();
			StringBuilder taskName = new StringBuilder()
					.append(getWeatherName(app, app.getRegions(), regionId))
					.append(" ").append(DownloadActivityType.WEATHER_FORECAST.getString(app));
			String message = app.getString(R.string.shared_string_downloading_formatted, taskName);
			int totalWork = getProgressDestination(regionId);
			progress.startTask(message, totalWork);
		}
	}

	public void onUpdateDownloadProgress(@NonNull WorldRegion region, @Nullable IProgress progress, boolean success) {
		String regionId = region.getRegionId();
		if (!isDownloadStateInProgress(regionId)) {
			LOG.debug("Weather offline forecast download " + regionId + " : cancel");
			return;
		}
		int destinationTilesCount = getProgressDestination(regionId);
		int downloadedTilesCount = getOfflineForecastProgressInfo(regionId);
		setOfflineForecastProgressInfo(regionId, ++downloadedTilesCount);

		float currentProgress = (float) downloadedTilesCount / destinationTilesCount;
		if (progress != null) {
			int remainingWork = destinationTilesCount - downloadedTilesCount;
			progress.remaining(remainingWork);
		}

		String status = success ? "done" : "error";
		LOG.debug("Weather offline forecast download " + regionId + " : " + currentProgress + "% " + status);

		if (currentProgress >= 1.f) {
			setPreferenceDownloadState(regionId, FINISHED);
			long lastUpdateTime = getTimeForTimeZone(System.currentTimeMillis(), "GMT").getTime();
			setPreferenceLastUpdate(regionId, lastUpdateTime);
			totalCacheSize.reset();

			runInUiThread(() -> {
				updateWeatherLayers();
				calculateCacheSize(region, null);
			});
		}
	}

	@NonNull
	public IndexItem createIndexItem(@NonNull WorldRegion region) {
		String regionId = region.getRegionId();
		DecimalFormat decimalFormat = new DecimalFormat("#.#");
		long contentSize = getOfflineForecastSizeInfo(regionId, true);
		long containerSize = getOfflineForecastSizeInfo(regionId, false);
		String size = decimalFormat.format(containerSize / (1024f * 1024f));

		long timestamp;
		WeatherForecastDownloadState downloadState = getPreferenceDownloadState(regionId);
		if (downloadState == UNDEFINED || downloadState == IN_PROGRESS) {
			timestamp = OsmAndFormatter.getStartOfToday().getTime();
		} else {
			timestamp = getPreferenceLastUpdate(regionId);
		}

		WeatherIndexItem indexItem = new WeatherIndexItem(
				region, timestamp, size, contentSize, containerSize
		);
		cachedWeatherIndexes.put(regionId, indexItem);
		return indexItem;
	}

	public boolean checkIfItemOutdated(@NonNull WeatherIndexItem weatherIndexItem) {
		String regionId = weatherIndexItem.getRegionId();
		if (!isDownloadStateFinished(regionId)) {
			return false;
		}
		weatherIndexItem.setDownloaded(true);
		boolean outdated = isForecastOutdated(regionId);
		weatherIndexItem.setOutdated(outdated);
		long lastUpdateTime = getPreferenceLastUpdate(regionId);
		if (lastUpdateTime != -1) {
			weatherIndexItem.setLocalTimestamp(lastUpdateTime);
		}
		return outdated;
	}

	public boolean isDownloadStateUndefined(@NonNull String regionId) {
		return getPreferenceDownloadState(regionId) == UNDEFINED;
	}

	public boolean isDownloadStateInProgress(@NonNull String regionId) {
		return getPreferenceDownloadState(regionId) == IN_PROGRESS;
	}

	public boolean isDownloadStateFinished(@NonNull String regionId) {
		return getPreferenceDownloadState(regionId) == FINISHED;
	}

	public WeatherForecastDownloadState getPreferenceDownloadState(@NonNull String regionId) {
		return getDownloadStatePreference(regionId).get();
	}

	public void setPreferenceDownloadState(@NonNull String regionId,
	                                       @NonNull WeatherForecastDownloadState downloadState) {
		getDownloadStatePreference(regionId).set(downloadState);
	}

	@NonNull
	private EnumStringPreference<WeatherForecastDownloadState> getDownloadStatePreference(@NonNull String regionId) {
		String prefId = PREF_FORECAST_DOWNLOAD_STATE_PREFIX + regionId;
		return (EnumStringPreference<WeatherForecastDownloadState>) settings.registerEnumStringPreference(prefId,
						WeatherForecastDownloadState.UNDEFINED,
						WeatherForecastDownloadState.values(),
						WeatherForecastDownloadState.class)
				.makeGlobal();
	}

	public long getPreferenceLastUpdate(@NonNull String regionId) {
		return getLastUpdatePreference(regionId).get();
	}

	public void setPreferenceLastUpdate(@NonNull String regionId, long lastUpdateTime) {
		getLastUpdatePreference(regionId).set(lastUpdateTime);
	}

	@NonNull
	private CommonPreference<Long> getLastUpdatePreference(@NonNull String regionId) {
		String prefId = PREF_FORECAST_LAST_UPDATE_PREFIX + regionId;
		return settings.registerLongPreference(prefId, -1).makeGlobal();
	}

	public WeatherForecastUpdatesFrequency getPreferenceFrequency(@NonNull String regionId) {
		return getFrequencyPreference(regionId).get();
	}

	public void setPreferenceFrequency(@NonNull String regionId, @NonNull WeatherForecastUpdatesFrequency value) {
		getFrequencyPreference(regionId).set(value);
	}

	@NonNull
	private EnumStringPreference<WeatherForecastUpdatesFrequency> getFrequencyPreference(@NonNull String regionId) {
		String prefId = PREF_FORECAST_FREQUENCY_PREFIX + regionId;
		return (EnumStringPreference<WeatherForecastUpdatesFrequency>) settings.registerEnumStringPreference(prefId,
						WeatherForecastUpdatesFrequency.UNDEFINED,
						WeatherForecastUpdatesFrequency.values(),
						WeatherForecastUpdatesFrequency.class)
				.makeGlobal();
	}

	public static Long getTileId(int x, int y) {
		return ((long) y << 32) + (long) x;
	}

	public static int getTileX(long tileId) {
		return (int) (tileId & 0xFFFF);
	}

	public static int getTileY(long tileId) {
		return (int) (tileId >> 32);
	}

	@Nullable
	public List<Long> getTileIds(@NonNull String regionId) {
		WorldRegion region = app.getRegions().getRegionData(regionId);
		return region != null ? getTileIds(region) : null;
	}

	public static List<Long> getTileIds(@NonNull WorldRegion region) {
		QuadRect regionBounds = getRegionBounds(region);
		LatLon topLeft = new LatLon(regionBounds.top, regionBounds.left);
		LatLon bottomRight = new LatLon(regionBounds.bottom, regionBounds.right);
		ZoomLevel zoomLevel = getGeoTileZoom();

		TileIdVector tileIdVector = WeatherTileResourcesManager.generateGeoTileIds(topLeft, bottomRight, zoomLevel);
		List<Long> tileIds = new ArrayList<>();
		for (int i = 0; i < tileIdVector.size(); i++) {
			TileId tileId = tileIdVector.get(i);
			tileIds.add(getTileId(tileId.getX(), tileId.getY()));
		}
		return tileIds;
	}

	public boolean getPreferenceWifi(@NonNull String regionId) {
		return getWifiPreference(regionId).get();
	}

	public void setPreferenceWifi(@NonNull String regionId, boolean value) {
		getWifiPreference(regionId).set(value);
	}

	public CommonPreference<Boolean> getWifiPreference(@NonNull String regionId) {
		String prefKey = PREF_FORECAST_WIFI_PREFIX + regionId;
		return settings.registerBooleanPreference(prefKey, false).makeGlobal();
	}

	public String[] getPreferenceKeys(@NonNull String regionId) {
		return new String[]{
				PREF_FORECAST_DOWNLOAD_STATE_PREFIX + regionId,
				PREF_FORECAST_LAST_UPDATE_PREFIX + regionId,
				PREF_FORECAST_FREQUENCY_PREFIX + regionId,
				PREF_FORECAST_WIFI_PREFIX + regionId
		};
	}

	public void removePreferences(@NonNull String regionId) {
		String[] prefKeys = getPreferenceKeys(regionId);
		settings.removeFromGlobalPreferences(prefKeys);
	}

	private void updateWeatherLayers() {
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (weatherPlugin != null) {
			weatherPlugin.updateLayers(app, null);
		}
	}

	@NonNull
	public List<IndexItem> findWeatherIndexesAt(@NonNull net.osmand.data.LatLon location, boolean includeDownloaded) {
		List<IndexItem> items = new ArrayList<>();
		try {
			items = DownloadResources.findIndexItemsAt(app, location, WEATHER_FORECAST, includeDownloaded);
		} catch (IOException e) {
			LOG.error(e);
		}
		WeatherIndexItem worldIndexItem = cachedWeatherIndexes.get(WORLD);
		if (worldIndexItem != null && !items.contains(worldIndexItem)) {
			items.add(0, worldIndexItem);
		}
		return items;
	}

	@NonNull
	public static ZoomLevel getGeoTileZoom() {
		return WeatherTileResourceProvider.getGeoTileZoom();
	}

	private void notifyOnComplete(@Nullable OnCompleteCallback callback) {
		if (callback != null) {
			callback.onComplete();
		}
	}

	@Nullable
	private OfflineForecastInfo getCachedInfo(@NonNull String regionId) {
		return offlineForecastInfo.get(regionId);
	}

	@NonNull
	private OfflineForecastInfo getOrCreateCachedInfo(@NonNull String regionId) {
		OfflineForecastInfo info = getCachedInfo(regionId);
		if (info == null) {
			info = new OfflineForecastInfo();
			offlineForecastInfo.put(regionId, info);
		}
		return info;
	}

	private void runAsync(@NonNull Runnable runnable) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				runnable.run();
				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void runInUiThread(@NonNull Runnable runnable) {
		app.runInUIThread(runnable);
	}
}
