package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GpxParameter.SPLIT_TYPE;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.helpers.GpxReaderTask.GpxDbReaderCallback;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class GpxDbHelper implements GpxDbReaderCallback {
	private static final Log LOG = PlatformUtil.getLog(GpxDbHelper.class);
	private final OsmandApplication app;
	private final GPXDatabase database;

	private final Map<File, GpxDirItem> dirItems = new ConcurrentHashMap<>();
	private final Map<File, GpxDataItem> dataItems = new ConcurrentHashMap<>();

	private final ConcurrentLinkedQueue<File> readingItems = new ConcurrentLinkedQueue<>();
	private final Map<File, GpxDataItem> readingItemsMap = new ConcurrentHashMap<>();
	private final Map<File, GpxDataItemCallback> readingItemsCallbacks = new ConcurrentHashMap<>();

	private GpxReaderTask readerTask;
	public static long readTrackItemCount = 0;

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

	public void loadItems() {
		loadGpxItems();
		loadGpxDirItems();
	}

	public void loadGpxItems() {
		long start = System.currentTimeMillis();
		List<GpxDataItem> items = getItems();

		Map<File, Boolean> fileExistenceMap = items.stream().collect(Collectors.toMap(GpxDataItem::getFile, item -> item.getFile().exists()));

		items.forEach(item -> {
			File file = item.getFile();
			if (Boolean.TRUE.equals(fileExistenceMap.get(file))) {
				dataItems.put(file, item);
			} else {
				remove(file);
			}
		});
		LOG.info("Time to loadGpxItems " + (System.currentTimeMillis() - start) + " ms, " + items.size() + " items");
	}

	public void loadGpxDirItems() {
		long start = System.currentTimeMillis();
		List<GpxDirItem> items = getDirItems();
		for (GpxDirItem item : items) {
			File file = item.getFile();
			if (file.exists()) {
				putToCache(item);
			} else {
				remove(file);
			}
		}
		LOG.info("Time to loadGpxDirItems " + (System.currentTimeMillis() - start) + " ms items count " + dataItems.size());
	}

	private void putToCache(@NonNull DataItem item) {
		File file = item.getFile();
		if (item instanceof GpxDataItem) {
			dataItems.put(file, (GpxDataItem) item);
		} else if (item instanceof GpxDirItem) {
			dirItems.put(file, (GpxDirItem) item);
		}
	}

	private void removeFromCache(@NonNull File file) {
		if (GpxUiHelper.isGpxFile(file)) {
			dataItems.remove(file);
		} else {
			dirItems.remove(file);
		}
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		boolean success = database.rename(currentFile, newFile);
		if (success) {
			GpxDataItem newItem = new GpxDataItem(app, newFile);
			GpxDataItem oldItem = dataItems.get(currentFile);
			if (oldItem != null) {
				newItem.copyData(oldItem);
			}
			putToCache(newItem);
			removeFromCache(currentFile);
			updateDefaultAppearance(newItem, false);
		}
		return success;
	}

	public boolean updateDataItem(@NonNull DataItem item) {
		boolean res = database.updateDataItem(item);
		putToCache(item);
		return res;
	}

	public boolean remove(@NonNull File file) {
		boolean res = database.remove(file);
		removeFromCache(file);
		return res;
	}

	public boolean remove(@NonNull DataItem item) {
		File file = item.getFile();
		boolean res = database.remove(file);
		removeFromCache(file);
		return res;
	}

	public boolean add(@NonNull GpxDataItem item) {
		boolean res = database.add(item);
		putToCache(item);
		updateDefaultAppearance(item, true);
		return res;
	}

	public boolean add(@NonNull GpxDirItem item) {
		boolean res = database.add(item);
		putToCache(item);
		return res;
	}

	@NonNull
	public List<GpxDataItem> getItems() {
		return database.getGpxDataItems();
	}

	@NonNull
	public List<GpxDirItem> getDirItems() {
		return database.getGpxDirItems();
	}

	@NonNull
	public List<Pair<String, Integer>> getStringIntItemsCollection(@NonNull String columnName,
	                                                               boolean includeEmptyValues,
	                                                               boolean sortByName,
	                                                               boolean sortDescending) {

		return database.getStringIntItemsCollection(columnName,
				includeEmptyValues,
				sortByName,
				sortDescending);
	}

	public long getTracksMinCreateDate() {
		return database.getTracksMinCreateDate();
	}

	public String getMaxParameterValue(GpxParameter parameter) {
		return database.getColumnMaxValue(parameter);
	}

	@Nullable
	public GpxDataItem getItem(@NonNull File file) {
		return getItem(file, null);
	}

	@NonNull
	public GpxDirItem getGpxDirItem(@NonNull File file) {
		GpxDirItem item = dirItems.get(file);
		if (item == null) {
			item = database.getGpxDirItem(file);
		}
		if (item == null) {
			item = new GpxDirItem(app, file);
			add(item);
		}
		return item;
	}

	@Nullable
	public GpxDataItem getItem(@NonNull File file, @Nullable GpxDataItemCallback callback) {
		if (file.getPath().isEmpty()) {
			return null;
		}
		GpxDataItem item = dataItems.get(file);
		if (GpxDbUtils.isAnalyseNeeded(item) && !isGpxReading(file)) {
			readTrackItemCount++;
			readGpxItem(file, item, callback);
		}
		return item;
	}

	public boolean hasGpxDataItem(@NonNull File file) {
		return dataItems.containsKey(file);
	}

	public boolean hasGpxDirItem(@NonNull File file) {
		return dirItems.containsKey(file);
	}

	@NonNull
	public List<GpxDataItem> getSplitItems() {
		GpxAppearanceHelper appearanceHelper = new GpxAppearanceHelper(app);
		List<GpxDataItem> items = new ArrayList<>();
		for (GpxDataItem item : getItems()) {
			int splitType = appearanceHelper.getParameter(item, SPLIT_TYPE);
			if (splitType != 0) {
				items.add(item);
			}
		}
		return items;
	}

	private void updateDefaultAppearance(@NonNull GpxDataItem item, boolean updateExistingValues) {
		File file = item.getFile();
		File dir = file.getParentFile();
		if (dir != null) {
			GpxDirItem dirItem = getGpxDirItem(dir);

			for (GpxParameter parameter : GpxParameter.getAppearanceParameters()) {
				Object value = item.getParameter(parameter);
				Object defaultValue = dirItem.getParameter(parameter);
				if (defaultValue != null && (updateExistingValues || value == null)) {
					item.setParameter(parameter, defaultValue);
				}
			}
			updateDataItem(item);
		}
	}

	public boolean isRead() {
		GpxReaderTask readerTask = this.readerTask;
		return readerTask == null || !readerTask.isReading();
	}

	private boolean isGpxReading(@NonNull File file) {
		GpxReaderTask analyser = this.readerTask;
		return readingItems.contains(file) || (analyser != null && file.equals(analyser.getFile()));
	}

	private void readGpxItem(@NonNull File file, @Nullable GpxDataItem item, @Nullable GpxDataItemCallback callback) {
		readingItemsMap.put(file, item != null ? item : new GpxDataItem(app, file));
		if (callback != null) {
			readingItemsCallbacks.put(file, callback);
		}
		readingItems.add(file);
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
	protected GPXDatabase getGPXDatabase() {
		return database;
	}

	@Override
	public void onGpxDataItemRead(@NonNull GpxDataItem item) {
		putToCache(item);
		putGpxDataItemToSmartFolder(item);
	}

	private void putGpxDataItemToSmartFolder(@NonNull GpxDataItem item) {
		TrackItem trackItem = new TrackItem(item.getFile());
		trackItem.setDataItem(item);
		app.getSmartFolderHelper().addTrackItemToSmartFolder(trackItem);
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
}
