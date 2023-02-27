package net.osmand.plus.track.helpers;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GpxDbHelper {

	private static final int MAX_ITEMS_CACHE_SIZE = 5000;

	private final OsmandApplication app;
	private final GPXDatabase db;
	private final Map<File, GpxDataItem> itemsCache = new ConcurrentHashMap<>();

	private final ConcurrentLinkedQueue<File> readingItems = new ConcurrentLinkedQueue<>();
	private final Map<File, GpxDataItem> readingItemsMap = new ConcurrentHashMap<>();
	private final Map<File, GpxDataItemCallback> readingItemsCallbacks = new ConcurrentHashMap<>();
	private GpxReaderTask readerTask;

	public interface GpxDataItemCallback {

		boolean isCancelled();

		void onGpxDataItemReady(GpxDataItem item);
	}

	public GpxDbHelper(@NonNull OsmandApplication app) {
		this.app = app;
		db = new GPXDatabase(app);
	}

	public void loadGpxItems() {
		List<GpxDataItem> items = getItems();
		for (GpxDataItem item : items) {
			putToCache(item);
		}
		loadNewGpxItems();
	}

	private void loadNewGpxItems() {
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
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

	private GpxDataItem putToCache(GpxDataItem item) {
		updateItemsCacheSize();
		return itemsCache.put(item.getFile(), item);
	}

	private void removeFromCache(GpxDataItem item) {
		itemsCache.remove(item.getFile());
	}

	public boolean rename(File currentFile, File newFile) {
		GpxDataItem item = itemsCache.get(currentFile);
		return db.rename(item, currentFile, newFile);
	}

	public boolean updateColor(GpxDataItem item, int color) {
		boolean res = db.updateColor(item, color);
		putToCache(item);
		return res;
	}

	public boolean updateLastUploadedTime(GpxDataItem item, long fileLastUploadedTime) {
		boolean res = db.updateLastUploadedTime(item, fileLastUploadedTime);
		putToCache(item);
		return res;
	}

	public boolean updateColoringType(@NonNull GpxDataItem item, @Nullable String coloringType) {
		boolean res = db.updateColoringType(item, coloringType);
		putToCache(item);
		return res;
	}

	public boolean updateShowAsMarkers(GpxDataItem item, boolean showAsMarkers) {
		boolean res = db.updateShowAsMarkers(item, showAsMarkers);
		putToCache(item);
		return res;
	}

	public boolean updateShowArrows(GpxDataItem item, boolean showArrows) {
		boolean res = db.updateShowArrows(item, showArrows);
		putToCache(item);
		return res;
	}

	public boolean updateShowStartFinish(GpxDataItem item, boolean showStartFinish) {
		boolean res = db.updateShowStartFinish(item, showStartFinish);
		putToCache(item);
		return res;
	}

	public boolean updateWidth(GpxDataItem item, String width) {
		boolean res = db.updateWidth(item, width);
		putToCache(item);
		return res;
	}

	public boolean updateSplit(@NonNull GpxDataItem item, @NonNull GpxSplitType splitType, double splitInterval) {
		boolean res = db.updateSplit(item, splitType.getType(), splitInterval);
		putToCache(item);
		return res;
	}

	public boolean updateJoinSegments(@NonNull GpxDataItem item, boolean joinSegments) {
		boolean res = db.updateJoinSegments(item, joinSegments);
		putToCache(item);
		return res;
	}

	public boolean updateGpsFilters(@NonNull GpxDataItem item, @NonNull FilteredSelectedGpxFile selectedGpxFile) {
		boolean res = db.updateGpsFiltersConfig(item, selectedGpxFile);
		putToCache(item);
		return res;
	}

	public void resetGpsFilters(@NonNull GpxDataItem item) {
		db.resetGpsFilters(item);
		putToCache(item);
	}

	public boolean remove(File file) {
		boolean res = db.remove(file);
		itemsCache.remove(file);
		return res;
	}

	public boolean remove(GpxDataItem item) {
		boolean res = db.remove(item);
		itemsCache.remove(item.getFile());
		return res;
	}

	public boolean add(GpxDataItem item) {
		boolean res = db.add(item);
		putToCache(item);
		return res;
	}

	public boolean updateAnalysis(GpxDataItem item, GPXTrackAnalysis a) {
		boolean res = db.updateAnalysis(item, a);
		putToCache(item);
		return res;
	}

	public boolean clearAnalysis(GpxDataItem item) {
		boolean res = db.clearAnalysis(item);
		itemsCache.remove(item.getFile());
		return res;
	}

	public List<GpxDataItem> getItems() {
		return db.getItems();
	}

	public GpxDataItem getItem(@NonNull File file) {
		return getItem(file, null);
	}

	public GpxDataItem getItem(@NonNull File file, @Nullable GpxDataItemCallback callback) {
		GpxDataItem item = itemsCache.get(file);
		if (isAnalyseNeeded(file, item) && !isGpxReading(file)) {
			readGpxItem(file, item, callback);
		}
		return item;
	}

	public boolean hasItem(@NonNull File file) {
		return itemsCache.containsKey(file);
	}

	public List<GpxDataItem> getSplitItems() {
		return db.getSplitItems();
	}

	public boolean isRead() {
		GpxReaderTask readerTask = this.readerTask;
		return readerTask == null || !readerTask.isReading();
	}

	private boolean isGpxReading(@NonNull File gpxFile) {
		GpxReaderTask analyser = this.readerTask;
		return readingItems.contains(gpxFile)
				|| (analyser != null && gpxFile.equals(analyser.getGpxFile()));
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
		readerTask = new GpxReaderTask();
		readerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopReading() {
		if (readerTask != null) {
			readerTask.cancel(false);
			readerTask = null;
		}
	}

	private boolean isAnalyseNeeded(@NonNull File gpxFile, @Nullable GpxDataItem item) {
		return item == null
				|| item.getFileLastModifiedTime() != gpxFile.lastModified()
				|| item.getAnalysis() == null
				|| item.getAnalysis().wptCategoryNames == null;
	}

	@SuppressLint("StaticFieldLeak")
	private class GpxReaderTask extends AsyncTask<Void, GpxDataItem, Void> {

		private File gpxFile;

		public File getGpxFile() {
			return gpxFile;
		}

		public boolean isReading() {
			return readingItems.size() > 0 || gpxFile != null;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			SQLiteConnection conn = db.openConnection(false);
			if (conn != null) {
				try {
					gpxFile = readingItems.poll();
					while (gpxFile != null && !isCancelled()) {
						GpxDataItem item = readingItemsMap.remove(gpxFile);
						if (item != null && item.getFile() == null) {
							item = db.getItem(gpxFile, conn);
						}
						if (isAnalyseNeeded(gpxFile, item)) {
							GPXFile f = GPXUtilities.loadGPXFile(gpxFile);
							GPXTrackAnalysis analysis = f.getAnalysis(gpxFile.lastModified());
							if (item == null || item.getFile() == null) {
								item = new GpxDataItem(gpxFile, analysis);
								db.insert(item, conn);
							} else {
								db.updateAnalysis(item, analysis, conn);
							}
							putToCache(item);
						} else {
							putToCache(item);
						}

						if (!isCancelled()) {
							publishProgress(item);
						}
						gpxFile = readingItems.poll();
					}
				} finally {
					conn.close();
				}
			} else {
				cancel(false);
			}
			return null;
		}

		@Override
		protected void onCancelled(Void aVoid) {
			readingItems.clear();
			readingItemsMap.clear();
			readingItemsCallbacks.clear();
		}

		@Override
		protected void onProgressUpdate(GpxDataItem... values) {
			for (GpxDataItem item : values) {
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
		protected void onPostExecute(Void aVoid) {
			if (readingItems.size() > 0 && !isCancelled()) {
				startReading();
			} else {
				readerTask = null;
			}
		}
	}
}
