package net.osmand.plus.plugins.weather;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static net.osmand.map.WorldRegion.RUSSIA_REGION_ID;
import static net.osmand.plus.helpers.FileNameTranslationHelper.getWeatherName;
import static net.osmand.plus.plugins.weather.OfflineForecastInfo.InfoType.LOCAL_SIZE;
import static net.osmand.plus.plugins.weather.OfflineForecastInfo.InfoType.PROGRESS_DOWNLOAD;
import static net.osmand.plus.plugins.weather.OfflineForecastInfo.InfoType.SIZE_CALCULATED;
import static net.osmand.plus.plugins.weather.OfflineForecastInfo.InfoType.UPDATES_SIZE;
import static net.osmand.plus.plugins.weather.WeatherForecastDownloadState.FINISHED;
import static net.osmand.plus.plugins.weather.WeatherForecastDownloadState.IN_PROGRESS;
import static net.osmand.plus.plugins.weather.WeatherForecastDownloadState.UNDEFINED;
import static net.osmand.plus.plugins.weather.WeatherUtils.checkAndGetRegionId;
import static net.osmand.plus.plugins.weather.WeatherUtils.getRectanglePoints;
import static net.osmand.plus.plugins.weather.WeatherUtils.isEntireWorld;
import static net.osmand.plus.utils.OsmAndFormatter.getTimeForTimeZone;

import android.text.format.DateUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
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
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.indexitem.WeatherIndexItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineForecastHelper {

	private static final Log log = PlatformUtil.getLog(OfflineForecastHelper.class);

	private static final String PREF_FORECAST_DOWNLOAD_STATE_PREFIX = "forecast_download_state_";
	private static final String PREF_FORECAST_LAST_UPDATE_PREFIX = "forecast_last_update_";
	private static final String PREF_FORECAST_FREQUENCY_PREFIX = "forecast_frequency_";
	private static final String PREF_FORECAST_TILE_IDS_PREFIX = "forecast_tile_ids_";
	private static final String PREF_FORECAST_WIFI_PREFIX = "forecast_download_via_wifi_";

	public static final int TILE_SIZE = 40_000;
	public static final int FORECAST_DATES_COUNT = 24 + (6 * 8) + 1;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final WeatherCacheSize weatherCacheSize;
	private WeatherTileResourcesManager weatherResourcesManager;
	private final Map<String, OfflineForecastInfo> offlineForecastInfo;

	public OfflineForecastHelper(@NonNull OsmandApplication app, @NonNull WeatherCacheSize weatherCacheSize) {
		this.app = app;
		settings = app.getSettings();
		offlineForecastInfo = new HashMap<>();
		this.weatherCacheSize = weatherCacheSize;
	}

	public void setWeatherResourcesManager(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		this.weatherResourcesManager = weatherResourcesManager;
	}

	public boolean shouldHaveWeatherForecast(@NonNull WorldRegion region) {
		String regionId = checkAndGetRegionId(region);
		int level = region.getLevel();

		boolean russia = RUSSIA_REGION_ID.equals(regionId);
		boolean russiaPrefix = regionId.startsWith(RUSSIA_REGION_ID);
		boolean unitedKingdom = regionId.equals(WorldRegion.UNITED_KINGDOM_REGION_ID);

		return isEntireWorld(regionId) ||
				(level == 1 && russia) ||
				(level > 1 && !russiaPrefix && ((level == 2 && !unitedKingdom) || (level == 3 && unitedKingdom)));
	}

	public void checkAndDownloadForecastsByRegionIds(List<String> regionIds) {
		if (!settings.isInternetConnectionAvailable()) {
			return;
		}
		int forecastDownloading = 0;
		for (WorldRegion region : app.getRegions().getFlattenedWorldRegions()) {
			String regionId = checkAndGetRegionId(region);
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
			String regionId = checkAndGetRegionId(region);
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
		String regionId = checkAndGetRegionId(region);
		if (!settings.isInternetConnectionAvailable()) {
			return false;
		}
		if (!settings.isWifiConnected() && getPreferenceWifi(regionId)) {
			return false;
		}

		Pair<LatLon, LatLon> rectanglePoints = getRectanglePoints(region);
		LatLon topLeft = rectanglePoints.first;
		LatLon bottomRight = rectanglePoints.second;

		updatePreferenceTileIdsIfNeeded(region);

		setOfflineForecastProgressInfo(regionId, 0);
		setPreferenceDownloadState(regionId, WeatherForecastDownloadState.IN_PROGRESS);

		onDownloadStarted(region, progress);

		// todo -- create and add FunctorQueryController
//		std::shared_ptr<const OsmAnd::IQueryController> queryController;
//		queryController.reset(new OsmAnd::FunctorQueryController(
//				[self, regionId]
//		(const OsmAnd::IQueryController *const controller) -> bool
//		{
//			return [self.class getPreferenceDownloadState:regionId] != EOAWeatherForecastDownloadStateInProgress;
//		}
//		));

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
//		    request.setQueryController(queryController); todo QueryController
			IDownloadGeoTilesAsyncCallback callback = new IDownloadGeoTilesAsyncCallback() {
				@Override
				public void method(boolean succeeded,
				                   BigInteger downloadedTiles,
				                   BigInteger totalTiles,
				                   SWIGTYPE_p_std__shared_ptrT_Metric_t metric) {
					onUpdateDownloadProgress(region, progress, succeeded);
				}
			};
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
			removeLocalForecast(RemoveLocalForecastParams.newInstance().setRegionIds(regionId));
		} else if (isDownloadStateFinished(regionId)) {
			calculateCacheSize(weatherIndexItem.getRegion(), null);
		}
	}

	public void prepareToStopDownloading(@NonNull String regionId) {
		weatherCacheSize.reset(true);
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
		String regionId = checkAndGetRegionId(region);
		setOfflineForecastSizeInfo(regionId, 0, true);
		setOfflineForecastSizeInfo(regionId, 0, false);
		setOfflineForecastSizesInfoCalculated(regionId, false);
		updatePreferenceTileIdsIfNeeded(region);
		runAsync(() -> {
			List<List<Integer>> tileIds = getPreferenceTileIds(regionId);
			TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
			ZoomLevel zoom = getGeoTileZoom();
			if (!qTileIds.isEmpty()) {
				long localSize = weatherResourcesManager.calculateDbCacheSize(qTileIds, new TileIdList(), zoom).longValue();
				setOfflineForecastSizeInfo(regionId, localSize, true);
				setOfflineForecastSizeInfo(regionId, calculateUpdatesSize(tileIds), false);
				setOfflineForecastSizesInfoCalculated(regionId, true);
			}
			runInUiThread(() -> {
				if (callback != null) {
					callback.onComplete();
				}
			});
		});
	}

	public void calculateFullCacheSize(boolean localData, CallbackWithObject<Long> callback) {
		runAsync(() -> {
			List<List<Integer>> tileIds = getOfflineTileIds(null);
			TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
			ZoomLevel zoom = getGeoTileZoom();
			long size = weatherCacheSize.get(localData);
			if (size == 0 && (!localData || !qTileIds.isEmpty())) {
				size = weatherResourcesManager.calculateDbCacheSize(
						localData ? qTileIds : new TileIdList(),
						localData ? new TileIdList() : qTileIds,
						zoom
				).longValue();
				weatherCacheSize.set(size, localData);
			}
			if (callback != null) {
				callback.processResult(size);
			}
		});
	}

	private int calculateUpdatesSize(@NonNull List<List<Integer>> tileIds) {
		int tilesCount = tileIds.size();
		return tilesCount * FORECAST_DATES_COUNT * TILE_SIZE;
	}

	public void clearCache(boolean localData, @NonNull List<String> regionIds) {
		if (localData) {
			if (Algorithms.isEmpty(regionIds)) {
				regionIds = getTempForecastsWithDownloadStates(IN_PROGRESS, FINISHED);
			}
			for (String regionId : regionIds) {
				setOfflineForecastSizeInfo(regionId, 0, true);
				prepareToStopDownloading(regionId);
			}
		}
		weatherCacheSize.reset(false);

		List<List<Integer>> tileIds = getOfflineTileIds(regionIds);
		TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
		ZoomLevel zoom = getGeoTileZoom();
		weatherResourcesManager.clearDbCache(
				localData ? qTileIds : new TileIdList(),
				localData ? new TileIdList() : qTileIds,
				zoom
		);
		runInUiThread(this::updateWeatherLayers);
	}

	public void removeLocalForecast(@NonNull RemoveLocalForecastParams params) {
		weatherCacheSize.reset();
		List<List<Integer>> tileIds = new ArrayList<>();
		for (String regionId : params.getRegionIds()) {
			List<List<Integer>> regionTileIds = getPreferenceTileIds(regionId);
			for (List<Integer> tileId : regionTileIds) {
				if (!tileIds.contains(tileId) && !isContainsInOfflineRegions(tileId, regionId)) {
					tileIds.add(tileId);
				}
			}
			removePreferences(regionId);
			removeOfflineForecastInfo(regionId);
			setOfflineForecastSizeInfo(regionId, calculateUpdatesSize(regionTileIds), false);
			notifyOnComplete(params.getOnSettingsRemovedCallback());
		}
		runAsync(() -> {
			TileIdList qTileIds = NativeUtilities.convertToQListTileIds(tileIds);
			ZoomLevel zoom = getGeoTileZoom();
			if (!qTileIds.isEmpty()) {
				weatherResourcesManager.clearDbCache(qTileIds, new TileIdList(), zoom);
			}
			if (params.shouldRefreshMap()) {
				runInUiThread(() -> {
					updateWeatherLayers();
					notifyOnComplete(params.getOnDataRemovedCallback());
				});
			}
		});
	}

	@NonNull
	public List<List<Integer>> getOfflineTileIds(List<String> regionIds) {
		if (regionIds == null) {
			regionIds = getTempForecastsWithDownloadStates(IN_PROGRESS, FINISHED);
		}
		List<List<Integer>> offlineTileIds = new ArrayList<>();
		for (String regionId : regionIds) {
			List<List<Integer>> regionTileIds = getPreferenceTileIds(regionId);
			for (List<Integer> tileId : regionTileIds) {
				if (!offlineTileIds.contains(tileId)) {
					offlineTileIds.add(tileId);
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
				removeLocalForecast(RemoveLocalForecastParams.newInstance().setRegionIds(regionId));
			}
		} else if (isDownloadStateFinished(regionId)) {
			int destination = getProgressDestination(regionId);
			setOfflineForecastProgressInfo(regionId, destination);
		}
	}

	public boolean isContainsInOfflineRegions(@NonNull List<Integer> tileId, @NonNull String excludeRegionId) {
		List<String> regionIds = getTempForecastsWithDownloadStates(IN_PROGRESS, FINISHED);
		for (String regionId : regionIds) {
			if (!regionId.equals(excludeRegionId)) {
				List<List<Integer>> regionTileIds = getPreferenceTileIds(regionId);
				for (List<Integer> offlineTileId : regionTileIds) {
					if (offlineTileId.equals(tileId)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public List<String> getTempForecastsWithDownloadStates(@NonNull WeatherForecastDownloadState... states) {
		List<String> forecasts = new ArrayList<>();
		List<WeatherForecastDownloadState> statesList = Arrays.asList(states);
		for (String regionId : offlineForecastInfo.keySet()) {
			if (statesList.contains(getPreferenceDownloadState(regionId))) {
				forecasts.add(regionId);
			}
		}
		return forecasts;
	}

	public void removeOfflineForecastInfo(@NonNull String regionId) {
		offlineForecastInfo.remove(regionId);
	}

	public void setOfflineForecastSizeInfo(@NonNull String regionId, long size, boolean local) {
		OfflineForecastInfo info = requireCachedInfo(regionId);
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
		OfflineForecastInfo info = requireCachedInfo(regionId);
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
		OfflineForecastInfo info = requireCachedInfo(regionId);
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
		return getPreferenceTileIds(regionId).size() * FORECAST_DATES_COUNT;
	}

	public void onDownloadStarted(@NonNull WorldRegion region, @Nullable IProgress progress) {
		if (progress != null) {
			String regionId = checkAndGetRegionId(region);
			StringBuilder taskName = new StringBuilder()
					.append(getWeatherName(app, app.getRegions(), regionId))
					.append(" ").append(DownloadActivityType.WEATHER_FORECAST.getString(app));
			String message = app.getString(R.string.shared_string_downloading_formatted, taskName);
			int totalWork = getProgressDestination(regionId);
			progress.startTask(message, totalWork);
		}
	}

	public void onUpdateDownloadProgress(@NonNull WorldRegion region, @Nullable IProgress progress, boolean success) {
		String regionId = checkAndGetRegionId(region);
		if (!isDownloadStateInProgress(regionId)) {
			log.debug("Weather offline forecast download " + regionId + " : cancel");
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
		log.debug("Weather offline forecast download " + regionId + " : " + currentProgress + "% " + status);

		if (currentProgress >= 1.f) {
			setPreferenceDownloadState(regionId, FINISHED);
			long lastUpdateTime = getTimeForTimeZone(System.currentTimeMillis(), "GMT").getTime();
			setPreferenceLastUpdate(regionId, lastUpdateTime);
			weatherCacheSize.reset();

			runInUiThread(() -> {
				updateWeatherLayers();
				calculateCacheSize(region, null);
			});
		}
	}

	@NonNull
	public IndexItem createIndexItem(@NonNull WorldRegion region) {
		String regionId = checkAndGetRegionId(region);
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

		return new WeatherIndexItem(
				region, timestamp, size, contentSize, containerSize
		);
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

	// todo implement / not implemented yet
	@NonNull
	public String getStatusInfoDescription(@NonNull String regionId) {
		return "";
	}

	// todo implement / not implemented yet
	@NonNull
	public String getAccuracyDescription(@NonNull String regionId) {
		return "";
	}

	// todo implement / not implemented yet
	@NonNull
	public String getUpdatesDateFormat(@NonNull String regionId, boolean next) {
		return "";
	}

	// todo implement / not implemented yet
	@NonNull
	public String getFrequencyFormat(@NonNull String regionId, boolean next) {
		return "";
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

	// todo check
	@NonNull
	public List<List<Integer>> getPreferenceTileIds(@NonNull String regionId) {
		List<List<Integer>> res = new ArrayList<>();
		List<String> matrixInStrings = getTileIdsPreference(regionId).getStringsList();
		if (matrixInStrings != null) {
			for (String idsRowAsString : matrixInStrings) {
				List<Integer> idsRow = new ArrayList<>();
				for (String idAsString : idsRowAsString.split(" ")) {
					idsRow.add(Integer.parseInt(idAsString));
				}
				res.add(idsRow);
			}
		}
		return res;
	}

	// todo check
	public void setPreferenceTileIds(@NonNull String regionId, @NonNull List<List<Integer>> value) {
		List<String> matrixInStrings = new ArrayList<>();
		for (List<Integer> idsRow : value) {
			StringBuilder stringBuilder = new StringBuilder();
			for (Integer id : idsRow) {
				stringBuilder.append(id).append(" ");
			}
			String idsRowAsString = stringBuilder.toString().trim();
			matrixInStrings.add(idsRowAsString);
		}
		getTileIdsPreference(regionId).setStringsList(matrixInStrings);
	}

	@NonNull
	private ListStringPreference getTileIdsPreference(@NonNull String regionId) {
		String prefId = PREF_FORECAST_TILE_IDS_PREFIX + regionId;
		return settings.registerStringListPreference(prefId, null, ",,");
	}

	public void updatePreferenceTileIdsIfNeeded(@NonNull WorldRegion region) {
		String regionId = checkAndGetRegionId(region);
		if (Algorithms.isEmpty(getPreferenceTileIds(regionId))) {
			Pair<LatLon, LatLon> boundPoints = getRectanglePoints(region);
			LatLon topLeft = boundPoints.first;
			LatLon bottomRight = boundPoints.second;
			ZoomLevel zoomLevel = getGeoTileZoom();

			TileIdVector tileIdVector = WeatherTileResourcesManager.generateGeoTileIds(topLeft, bottomRight, zoomLevel);
			List<List<Integer>> tileIds = new ArrayList<>();
			for (int i = 0; i < tileIdVector.size(); i++) {
				List<Integer> row = new ArrayList<>();
				TileId tileId = tileIdVector.get(i);
				row.add(tileId.getX());
				row.add(tileId.getY());
				tileIds.add(row);
			}
			setPreferenceTileIds(regionId, tileIds);
		}
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
				PREF_FORECAST_TILE_IDS_PREFIX + regionId,
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
	public ZoomLevel getGeoTileZoom() {
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
	private OfflineForecastInfo requireCachedInfo(@NonNull String regionId) {
		OfflineForecastInfo info = getCachedInfo(regionId);
		if (info == null) {
			info = new OfflineForecastInfo();
			offlineForecastInfo.put(regionId, info);
		}
		return info;
	}

	private void runAsync(@NonNull Runnable runnable) {
		new Thread(runnable).start();
	}

	private void runInUiThread(@NonNull Runnable runnable) {
		app.runInUIThread(runnable);
	}
}
