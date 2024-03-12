package net.osmand.plus.configmap.tracks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GPXFolderUtils;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxUiHelper;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackFolderLoaderTask extends AsyncTask<Void, TrackItem, Void> {

	private static final Log LOG = PlatformUtil.getLog(TrackFolderLoaderTask.class);

	private final GpxDbHelper gpxDbHelper;
	private final SmartFolderHelper smartFolderHelper;

	private final TrackFolder folder;
	private final TracksSortByMode sortByMode;
	private final LoadTracksListener listener;

	public TrackFolderLoaderTask(@NonNull OsmandApplication app, @NonNull TrackFolder folder,
	                             @NonNull LoadTracksListener listener) {
		this.folder = folder;
		this.listener = listener;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.smartFolderHelper = app.getSmartFolderHelper();
		this.sortByMode = app.getSettings().TRACKS_SORT_BY_MODE.get();
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadTracksStarted();
		}
	}

	@Override
	protected void onProgressUpdate(TrackItem... values) {
		if (listener != null) {
			listener.loadTracksProgress(values);
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		long time = System.currentTimeMillis();
		LOG.info("Start loading tracks in " + folder.getDirName());

		folder.clearData();
		List<TrackItem> progress = new ArrayList<>();
		loadGPXFolder(folder, null, progress, true);
		if (!progress.isEmpty()) {
			publishProgress(progress.toArray(new TrackItem[0]));
		}
		if (listener != null) {
			listener.tracksLoaded(folder);
		}
		LOG.info("Finished loading tracks. took " + (System.currentTimeMillis() - time) + "ms");
		return null;
	}

	private void loadGPXFolder(@NonNull TrackFolder trackFolder, @Nullable String subfolder,
	                           @NonNull List<TrackItem> progress, boolean updateSmartFolder) {
		File folderFile = trackFolder.getDirFile();
		File[] files = GPXFolderUtils.listFilesSorted(sortByMode, folderFile);
		List<TrackFolder> subFolders = new ArrayList<>();
		List<TrackItem> trackItems = new ArrayList<>();
		for (File file : files) {
			if (file.isDirectory()) {
				TrackFolder subFold = new TrackFolder(file, trackFolder);
				subFolders.add(subFold);
				loadGPXFolder(subFold, GPXFolderUtils.getSubfolderTitle(file, subfolder), progress, updateSmartFolder);
			} else if (GpxUiHelper.isGpxFile(file)) {
				TrackItem item = new TrackItem(file);
				item.setDataItem(getDataItem(item, file));
				trackItems.add(item);
				progress.add(item);
				if (progress.size() > 7) {
					publishProgress(progress.toArray(new TrackItem[0]));
					progress.clear();
				}
			}
		}
		trackFolder.setSubFolders(subFolders);
		trackFolder.setTrackItems(trackItems);
		trackFolder.resetCashedData();
		if (updateSmartFolder) {
			smartFolderHelper.addTrackItemsToSmartFolder(trackItems);
		}
	}

	@Nullable
	private GpxDataItem getDataItem(@NonNull TrackItem trackItem, File file) {
//		File file = trackItem.getFile();
		if (file != null) {
			GpxDataItemCallback callback = new GpxDataItemCallback() {
				@Override
				public boolean isCancelled() {
					return TrackFolderLoaderTask.this.isCancelled();
				}

				@Override
				public void onGpxDataItemReady(@NonNull GpxDataItem item) {
					trackItem.setDataItem(item);
				}
			};
			return gpxDbHelper.getItem(file, callback);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.loadTracksFinished(folder);
		}
		smartFolderHelper.notifyUpdateListeners();
	}

	public interface LoadTracksListener {

		@UiThread
		default void loadTracksStarted() {
		}

		@UiThread
		default void loadTracksProgress(@NonNull TrackItem[] items) {
		}

		@WorkerThread
		default void tracksLoaded(@NonNull TrackFolder folder) {
		}

		@UiThread
		void loadTracksFinished(@NonNull TrackFolder folder);
	}
}
