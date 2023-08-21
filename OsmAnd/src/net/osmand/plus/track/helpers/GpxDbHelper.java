package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxReaderTask.GpxDbReaderCallback;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GpxDbHelper implements GpxDbReaderCallback {

	private static final int MAX_ITEMS_CACHE_SIZE = 5000;

	private final OsmandApplication app;
	private final GPXDatabase database;

	private final Map<File, GpxDataItem> itemsCache = new ConcurrentHashMap<>();

	private final ConcurrentLinkedQueue<File> readingItems = new ConcurrentLinkedQueue<>();
	private final Map<File, GpxDataItem> readingItemsMap = new ConcurrentHashMap<>();
	private final Map<File, GpxDataItemCallback> readingItemsCallbacks = new ConcurrentHashMap<>();

	private GpxReaderTask readerTask;

	public interface GpxDataItemCallback {

		default boolean isCancelled() {
			return false;
		}

		void onGpxDataItemReady(@NonNull GpxDataItem item);
	}

	public GpxDbHelper(@NonNull OsmandApplication app) {
		this.app = app;
		database = new GPXDatabase(app);
	}

	public void loadGpxItems() {
		List<GpxDataItem> items = getItems();
		for (GpxDataItem item : items) {
			putToCache(item);
		}
		loadNewGpxItems();
	}

	private void loadNewGpxItems() {
		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		List<GPXInfo> gpxInfos = GpxUiHelper.getGPXFiles(gpxDir, true);
		for (GPXInfo gpxInfo : gpxInfos) {
			File file = new File(gpxInfo.getFileName());
			if (file.exists() && !file.isDirectory() && !hasItem(file)) {
				add(new GpxDataItem(file));
			}
		}
	}

	private void updateItemsCacheSize() {
		if (itemsCache.size() > MAX_ITEMS_CACHE_SIZE) {
			itemsCache.clear();
		}
	}

	private GpxDataItem putToCache(@NonNull GpxDataItem item) {
		updateItemsCacheSize();
		return itemsCache.put(item.getFile(), item);
	}

	private void removeFromCache(@NonNull File file) {
		itemsCache.remove(file);
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		GpxDataItem item = itemsCache.get(currentFile);
		boolean res = database.rename(item, currentFile, newFile);
		if (item != null) {
			putToCache(item);
			removeFromCache(currentFile);
		}
		return res;
	}

	public boolean updateColor(@NonNull GpxDataItem item, int color) {
		boolean res = database.updateColor(item, color);
		putToCache(item);
		return res;
	}

	public boolean updateLastUploadedTime(@NonNull GpxDataItem item, long fileLastUploadedTime) {
		boolean res = database.updateLastUploadedTime(item, fileLastUploadedTime);
		putToCache(item);
		return res;
	}

	public boolean updateColoringType(@NonNull GpxDataItem item, @Nullable String coloringType) {
		boolean res = database.updateColoringType(item, coloringType);
		putToCache(item);
		return res;
	}

	public boolean updateNearestCityName(@NonNull GpxDataItem item, @Nullable String nearestCityName) {
		boolean res = database.updateNearestCityName(item, nearestCityName);
		putToCache(item);
		return res;
	}

	public boolean updateShowAsMarkers(@NonNull GpxDataItem item, boolean showAsMarkers) {
		boolean res = database.updateShowAsMarkers(item, showAsMarkers);
		putToCache(item);
		return res;
	}

	public boolean updateImportedByApi(@NonNull GpxDataItem item, boolean importedByApi) {
		boolean res = database.updateImportedByApi(item, importedByApi);
		putToCache(item);
		return res;
	}

	public boolean updateShowArrows(@NonNull GpxDataItem item, boolean showArrows) {
		boolean res = database.updateShowArrows(item, showArrows);
		putToCache(item);
		return res;
	}

	public boolean updateShowStartFinish(@NonNull GpxDataItem item, boolean showStartFinish) {
		boolean res = database.updateShowStartFinish(item, showStartFinish);
		putToCache(item);
		return res;
	}

	public boolean updateWidth(@NonNull GpxDataItem item, @NonNull String width) {
		boolean res = database.updateWidth(item, width);
		putToCache(item);
		return res;
	}

	public boolean updateSplit(@NonNull GpxDataItem item, @NonNull GpxSplitType splitType, double splitInterval) {
		boolean res = database.updateSplit(item, splitType.getType(), splitInterval);
		putToCache(item);
		return res;
	}

	public boolean updateJoinSegments(@NonNull GpxDataItem item, boolean joinSegments) {
		boolean res = database.updateJoinSegments(item, joinSegments);
		putToCache(item);
		return res;
	}

	public boolean updateGpsFilters(@NonNull GpxDataItem item, @NonNull FilteredSelectedGpxFile selectedGpxFile) {
		boolean res = database.updateGpsFiltersConfig(item, selectedGpxFile);
		putToCache(item);
		return res;
	}

	public void resetGpsFilters(@NonNull GpxDataItem item) {
		database.resetGpsFilters(item);
		putToCache(item);
	}

	public boolean updateAppearance(@NonNull GpxDataItem item, int color, @NonNull String width,
	                                boolean showArrows, boolean showStartFinish, int splitType,
	                                double splitInterval, @Nullable String coloringType) {
		boolean res = database.updateAppearance(item, color, width, showArrows, showStartFinish, splitType, splitInterval, coloringType);
		putToCache(item);
		return res;
	}

	public boolean remove(@NonNull File file) {
		boolean res = database.remove(file);
		itemsCache.remove(file);
		return res;
	}

	public boolean remove(@NonNull GpxDataItem item) {
		boolean res = database.remove(item);
		itemsCache.remove(item.getFile());
		return res;
	}

	public boolean add(@NonNull GpxDataItem item) {
		boolean res = database.add(item);
		putToCache(item);
		return res;
	}

	public boolean updateAnalysis(@NonNull GpxDataItem item, @Nullable GPXTrackAnalysis analysis) {
		boolean res = database.updateAnalysis(item, analysis);
		putToCache(item);
		return res;
	}

	public boolean clearAnalysis(@NonNull GpxDataItem item) {
		boolean res = database.clearAnalysis(item);
		itemsCache.remove(item.getFile());
		return res;
	}

	@NonNull
	public List<GpxDataItem> getItems() {
		return database.getItems();
	}

	@Nullable
	public GpxDataItem getItem(@NonNull File file) {
		return getItem(file, null);
	}

	@Nullable
	public GpxDataItem getItem(@NonNull File file, @Nullable GpxDataItemCallback callback) {
		GpxDataItem item = itemsCache.get(file);
		if ((isAnalyseNeeded(file, item) || GpxDbHelper.isCitySearchNeeded(item)) && !isGpxReading(file)) {
			readGpxItem(file, item, callback);
		}
		return item;
	}

	public boolean hasItem(@NonNull File file) {
		return itemsCache.containsKey(file);
	}

	@NonNull
	public List<GpxDataItem> getSplitItems() {
		return database.getSplitItems();
	}

	public boolean isRead() {
		GpxReaderTask readerTask = this.readerTask;
		return readerTask == null || !readerTask.isReading();
	}

	private boolean isGpxReading(@NonNull File file) {
		GpxReaderTask analyser = this.readerTask;
		return readingItems.contains(file) || (analyser != null && file.equals(analyser.getFile()));
	}

	private void readGpxItem(@NonNull File gpxFile, @Nullable GpxDataItem item, @Nullable GpxDataItemCallback callback) {
		readingItemsMap.put(gpxFile, item != null ? item : new GpxDataItem(null, (GPXTrackAnalysis) null));
		if (callback != null) {
			readingItemsCallbacks.put(gpxFile, callback);
		}
		readingItems.add(gpxFile);
		if (readerTask == null) {
			startReading();
		}
	}

	private void startReading() {
		readerTask = new GpxReaderTask(app, readingItems, readingItemsMap, this);
		readerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopReading() {
		if (readerTask != null) {
			readerTask.cancel(false);
			readerTask = null;
		}
	}

	@NonNull
	@Override
	public GPXDatabase getGPXDatabase() {
		return database;
	}

	@Override
	public void onGpxDataItemRead(@NonNull GpxDataItem item) {
		putToCache(item);
	}

	@Override
	public void onProgressUpdate(@NonNull GpxDataItem... dataItems) {
		for (GpxDataItem item : dataItems) {
			GpxDataItemCallback callback = readingItemsCallbacks.remove(item.getFile());
			if (callback != null) {
				if (callback.isCancelled()) {
					stopReading();
				} else {
					callback.onGpxDataItemReady(item);
				}
			}
		}
	}

	@Override
	public void onReadingCancelled() {
		readingItems.clear();
		readingItemsMap.clear();
		readingItemsCallbacks.clear();
	}

	@Override
	public void onReadingFinished(boolean cancelled) {
		if (!Algorithms.isEmpty(readingItems) && !cancelled) {
			startReading();
		} else {
			readerTask = null;
		}
	}

	public static boolean isAnalyseNeeded(@NonNull File gpxFile, @Nullable GpxDataItem item) {
		return item == null
				|| item.getFileLastModifiedTime() != gpxFile.lastModified()
				|| item.getAnalysis() == null
				|| item.getAnalysis().wptCategoryNames == null
				|| item.getAnalysis().latLonStart == null && item.getAnalysis().points > 0;
	}

	public static boolean isCitySearchNeeded(@Nullable GpxDataItem item) {
		return item != null && item.getNearestCityName() == null && item.getAnalysis() != null && item.getAnalysis().latLonStart != null;
	}
}