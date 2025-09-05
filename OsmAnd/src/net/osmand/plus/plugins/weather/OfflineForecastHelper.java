package net.osmand.plus.plugins.weather;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static net.osmand.map.WorldRegion.RUSSIA_REGION_ID;
import static net.osmand.map.WorldRegion.UNITED_KINGDOM_REGION_ID;
import static net.osmand.map.WorldRegion.WORLD;
import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;
import static net.osmand.plus.helpers.FileNameTranslationHelper.getWeatherName;
import static net.osmand.plus.plugins.weather.WeatherUtils.isWeatherSupported;
import static net.osmand.plus.plugins.weather.WeatherUtils.getRegionBounds;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.LOCAL_SIZE;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.PROGRESS_DOWNLOAD;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.SIZE_CALCULATED;
import static net.osmand.plus.plugins.weather.containers.OfflineForecastInfo.InfoType.UPDATES_SIZE;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.FINISHED;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.IN_PROGRESS;
import static net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState.UNDEFINED;
import static net.osmand.plus.utils.OsmAndFormatter.getTimeForTimeZone;

import android.os.AsyncTask;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.OnCompleteCallback;
import net.osmand.PlatformUtil;
import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.Metric;
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
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.containers.OfflineForecastInfo;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize;
import net.osmand.plus.plugins.weather.containers.WeatherTotalCacheSize.ResetTotalWeatherCacheSizeListener;
import net.osmand.plus.plugins.weather.enums.WeatherForecastDownloadState;
import net.osmand.plus.plugins.weather.enums.WeatherForecastUpdatesFrequency;
import net.osmand.plus.plugins.weather.listener.RemoveLocalForecastListener;
import net.osmand.plus.plugins.weather.listener.WeatherCacheSizeChangeListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
	private final WeatherTotalCacheSize totalCacheSize;
	private List<WeatherCacheSizeChangeListener> weatherCacheSizeChangeListeners = new ArrayList<>();
	private boolean clearOnlineCacheInProgress;
	private boolean totalCacheSizeCalculationInProgress;
	private List<RemoveLocalForecastListener> removeLocalForecastListeners = new ArrayList<>();
	private List<String> regionsRemoveInProgress = new ArrayList<>();

	public OfflineForecastHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		offlineForecastInfo = new HashMap<>();
		totalCacheSize = new WeatherTotalCacheSize(this);
	}

	public void setWeatherResourcesManager(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		this.weatherResourcesManager = weatherResourcesManager;
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

	public void downloadForecastByRegion(@NonNull WorldRegion region) {
		downloadForecastByRegion(region, null);
	}

	public boolean downloadForecastByRegion(@NonNull WorldRegion region, @Nullable IProgress progress) {
		if (!isWeatherSupported(app)) {
			LOG.error("[Download] [" + region.getRegionId() + "] Failed. Weather isn't allowed with current configuration.");
			return false;
		}
		if (weatherResourcesManager == null) {
			LOG.error("[Download] [" + region.getRegionId() + "] Failed. weatherResourcesManager  isn't available.");
			return false;
		}
		String regionId = region.getRegionId();
		if (!settings.isInternetConnectionAvailable()) {
			LOG.error("[Download] [" + regionId + "] Failed. Internet connection isn't available.");
			return false;
		}
		if (!settings.isWifiConnected() && getPreferenceWifi(regionId)) {
			LOG.error("[Download] [" + regionId + "] Failed. Wi-Fi isn't connected.");
			return false;
		}

		setOfflineForecastProgressInfo(regionId, 0);
		setPreferenceDownloadState(regionId, IN_PROGRESS);

		onDownloadStarted(region, progress);

		long[] errorsCount = {0};
		for (QuadRect bounds : getRegionBounds(region)) {
			downloadForecastByRegion(region, bounds, progress, errorsCount);
		}

		return isDownloadStateFinished(regionId);
	}

	private void downloadForecastByRegion(@NonNull WorldRegion region, @NonNull QuadRect regionBounds,
	                                      @Nullable IProgress progress, long[] errorsCounter) {
		String regionId = region.getRegionId();
		LatLon topLeft = new LatLon(regionBounds.top, regionBounds.left);
		LatLon bottomRight = new LatLon(regionBounds.bottom, regionBounds.right);

		interface_IQueryController queryController = new interface_IQueryController() {
			@Override
			public boolean isAborted() {
				return getPreferenceDownloadState(regionId) != IN_PROGRESS;
			}
		};

		long dateTime = OsmAndFormatter.getStartOfToday();
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
				                   Metric metric) {
					if (!succeeded) {
						errorsCounter[0]++;
					}
					onUpdateDownloadProgress(region, progress, errorsCounter[0]);
				}
			};
			queryController.swigReleaseOwnership();
			callback.swigReleaseOwnership();

			weatherResourcesManager.downloadGeoTiles(request, callback.getBinding());
			dateTime += HOUR_IN_MILLIS * (i < 24 ? 1 : 3);
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

	public void calculateCacheSizeIfNeeded(@NonNull IndexItem indexItem,
	                                       @NonNull WorldRegion region,
	                                       @Nullable OnCompleteCallback callback) {
		String regionId = region.getRegionId();
		if (!isWeatherSupported(app)) {
			LOG.error("[Calculate size] [" + regionId + "] Can't calculate cache size. Weather isn't allowed with this configuration.");
			notifyOnComplete(callback);
			return;
		}
		if (weatherResourcesManager == null) {
			LOG.error("[Calculate size] [" + regionId + "] Can't calculate cache size. WeatherResourcesManager isn't available.");
			return;
		}
		if (!isOfflineForecastSizesInfoCalculated(regionId)) {
			calculateCacheSize(region, () -> {
				NumberFormat decimalFormat = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
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
		if (!isWeatherSupported(app)) {
			LOG.error("[Calculate size] [" + regionId + "] Can't calculate cache size. Weather isn't allowed with current configuration.");
			notifyOnComplete(callback);
			return;
		}
		if (weatherResourcesManager == null) {
			LOG.error("[Calculate size] [" + regionId + "] Can't calculate cache size. WeatherResourcesManager isn't available.");
			return;
		}
		setOfflineForecastSizeInfo(regionId, 0, true);
		setOfflineForecastSizeInfo(regionId, 0, false);
		setOfflineForecastSizesInfoCalculated(regionId, false);
		runAsync(() -> {
			List<Long> tileIds = getTileIds(region);
			TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
			ZoomLevel zoom = getGeoTileZoom();
			if (!qTileIds.isEmpty()) {
				BigInteger calculatedSize = weatherResourcesManager.calculateDbCacheSize(qTileIds, new TileIdList(), zoom);
				long localSize = calculatedSize != null ? calculatedSize.longValue() : 0;
				long updatesSize = calculateApproxUpdatesSize(tileIds);
				setOfflineForecastSizeInfo(regionId, localSize, true);
				setOfflineForecastSizeInfo(regionId, updatesSize, false);
				setOfflineForecastSizesInfoCalculated(regionId, true);
			}
			runInUiThread(() -> notifyOnComplete(callback));
		});
	}

	private int calculateApproxUpdatesSize(@NonNull List<Long> tileIds) {
		return tileIds.size() * FORECAST_DATES_COUNT * TILE_SIZE;
	}

	public void calculateTotalCacheSizeAsync(boolean forceCalculation) {
		if (!isWeatherSupported(app)) {
			LOG.error("[Calculate size] [Total] Can't calculate. Weather isn't allowed with current configuration.");
			return;
		}
		if (weatherResourcesManager == null) {
			LOG.error("[Calculate size] [Total] Can't calculate. WeatherResourcesManager isn't available.");
			return;
		}
		if ((totalCacheSize.isCalculated() && !forceCalculation)) {
			// calculation is not required
			LOG.info("[Calculate size] [Total] Calculation is not required. Already calculated and don't need force calculation");
			return;
		}
		if (isTotalCacheSizeCalculationInProgress() || isClearOnlineCacheInProgress()) {
			LOG.info("[Calculate size] [Total] Calculation is not required. Calculation in progress or clear online cache in progress");
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
				BigInteger calculatedSize = weatherResourcesManager.calculateDbCacheSize(tileIds, excludeIds, zoom);
				long size = calculatedSize != null ? calculatedSize.longValue() : 0;
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
		if (isWeatherSupported(app)) {
			runAsync(this::clearOnlineCache);
		} else {
			LOG.error("[Clear] [All online] Can't clear online cache. Weather isn't allowed with current configuration.");
		}
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
		if (isWeatherSupported(app)) {
			runAsync(() -> clearOfflineCache(regionIds));
		} else {
			LOG.error("[Clear] [All offline] Can't clear offline cache. Weather isn't allowed with current configuration.");
		}
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
			return totalCacheSize.isCalculated(false) && totalCacheSize.get(false) > 0;
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
		weatherCacheSizeChangeListeners = CollectionUtils.addToList(weatherCacheSizeChangeListeners, listener);
	}

	public void unregisterWeatherCacheSizeChangeListener(@NonNull WeatherCacheSizeChangeListener listener) {
		weatherCacheSizeChangeListeners = CollectionUtils.removeFromList(weatherCacheSizeChangeListeners, listener);
	}

	public void removeLocalForecastAsync(@NonNull String regionId, boolean refreshMap, boolean notifyUserOnFinish) {
		if (isWeatherSupported(app)) {
			runAsync(() -> removeLocalForecast(new String[] {regionId}, refreshMap, notifyUserOnFinish));
		} else {
			LOG.error("[Clear] [" + regionId + "] Can't remove local forecast. Weather isn't allowed with current configuration.");
		}
	}

	private void removeLocalForecast(@NonNull String[] regionIds, boolean refreshMap, boolean notifyUserOnFinish) {
		List<String> regionIdsList = Arrays.asList(regionIds);
		regionsRemoveInProgress = CollectionUtils.addAllToList(regionsRemoveInProgress, regionIdsList);

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
						.append(WEATHER_FORECAST.getString(app));
				app.showToastMessage(R.string.item_deleted, fileName);
			}
		}
		regionsRemoveInProgress = CollectionUtils.removeAllFromList(regionsRemoveInProgress, regionIdsList);
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
		removeLocalForecastListeners = CollectionUtils.addToList(removeLocalForecastListeners, listener);
	}

	public void unregisterRemoveLocalForecastListener(@NonNull RemoveLocalForecastListener listener) {
		removeLocalForecastListeners = CollectionUtils.removeFromList(removeLocalForecastListeners, listener);
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
		boolean outdated = false;
		if (isDownloadStateFinished(regionId)) {
			int daysGone = 0;
			long lastUpdate = getPreferenceLastUpdate(regionId);
			if (lastUpdate != -1) {
				long passedTime = OsmAndFormatter.getStartOfToday() - lastUpdate;
				daysGone = (int) (passedTime / DateUtils.DAY_IN_MILLIS);
			}
			outdated = daysGone >= 7;
		}
		return outdated;
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

	public void setOfflineForecastSizeInfo(@NonNull String regionId, long size, boolean forLocal) {
		OfflineForecastInfo info = getOrCreateCachedInfo(regionId);
		info.put(forLocal ? LOCAL_SIZE : UPDATES_SIZE, size);
	}

	public long getOfflineForecastSizeInfo(@NonNull String regionId, boolean forLocal) {
		OfflineForecastInfo info = getCachedInfo(regionId);
		if (info != null) {
			Object size = info.get(forLocal ? LOCAL_SIZE : UPDATES_SIZE);
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
					.append(" ").append(WEATHER_FORECAST.getString(app));
			String message = app.getString(R.string.shared_string_downloading_formatted, taskName);
			int totalWork = getProgressDestination(regionId);
			progress.startTask(message, totalWork);
		}
	}

	public void onUpdateDownloadProgress(@NonNull WorldRegion region, @Nullable IProgress progress, long errorsCount) {
		String regionId = region.getRegionId();
		if (!isDownloadStateInProgress(regionId)) {
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

		if (currentProgress >= 1.0f) {
			setPreferenceDownloadState(regionId, FINISHED);
			long lastUpdateTime = getTimeForTimeZone(System.currentTimeMillis(), "GMT").getTime();
			setPreferenceLastUpdate(regionId, lastUpdateTime);
			totalCacheSize.reset();

			if (errorsCount > 0 && destinationTilesCount > 0) {
				int percentage = ProgressHelper.normalizeProgressPercent((int) (errorsCount * 100 / destinationTilesCount));
				app.showToastMessage(R.string.weather_download_error, percentage + "%");
			}
			runInUiThread(() -> {
				updateWeatherLayers();
				calculateCacheSize(region, null);
			});
		}
	}

	private boolean shouldHaveWeatherForecast(@NonNull WorldRegion region) {
		int level = region.getLevel();
		String regionId = region.getRegionId();
		return WORLD.equals(regionId) || (level > 2 && regionId.startsWith(RUSSIA_REGION_ID))
				|| (level == 2 && !regionId.startsWith(UNITED_KINGDOM_REGION_ID))
				|| (level == 3 && regionId.startsWith(UNITED_KINGDOM_REGION_ID));
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
						UNDEFINED,
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

	@NonNull
	public static List<Long> getTileIds(@NonNull WorldRegion region) {
		Set<Long> tileIds = new HashSet<>();
		ZoomLevel zoomLevel = getGeoTileZoom();

		for (QuadRect bounds : getRegionBounds(region)) {
			LatLon topLeft = new LatLon(bounds.top, bounds.left);
			LatLon bottomRight = new LatLon(bounds.bottom, bounds.right);

			TileIdVector tileIdVector = WeatherTileResourcesManager.generateGeoTileIds(topLeft, bottomRight, zoomLevel);
			for (int i = 0; i < tileIdVector.size(); i++) {
				TileId tileId = tileIdVector.get(i);
				tileIds.add(getTileId(tileId.getX(), tileId.getY()));
			}
		}
		return new ArrayList<>(tileIds);
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
		return new String[] {
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
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				runnable.run();
				return null;
			}
		});
	}

	private void runInUiThread(@NonNull Runnable runnable) {
		app.runInUIThread(runnable);
	}
}
