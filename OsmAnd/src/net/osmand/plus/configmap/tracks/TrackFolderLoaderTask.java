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
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GPXFolderUtils;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxUiHelper;

import org.apache.commons.logging.Log;

import java.io.File;

public class TrackFolderLoaderTask extends AsyncTask<Void, Void, TrackFolder> {
	public static final Log LOG = PlatformUtil.getLog(TrackFolderLoaderTask.class);

	private final GpxDbHelper gpxDbHelper;
	private final File dir;
	private final TracksSortByMode sortByMode;
	private final LoadTracksListener listener;
	private final SmartFolderHelper smartFolderHelper;

	public TrackFolderLoaderTask(@NonNull OsmandApplication app, @NonNull File dir, @NonNull LoadTracksListener listener) {
		this.dir = dir;
		this.listener = listener;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.sortByMode = app.getSettings().TRACKS_SORT_BY_MODE.get();
		smartFolderHelper = app.getSmartFolderHelper();
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadTracksStarted();
		}
	}

	@Override
	protected TrackFolder doInBackground(Void... voids) {
		long startLoadingTime = System.currentTimeMillis();
		LOG.info("Start loading tracks in " + dir.getName());
		TrackFolder tracksFolder = new TrackFolder(dir, null);
		loadGPXFolder(tracksFolder, "", true);
		if (listener != null) {
			listener.tracksLoaded(tracksFolder);
		}
		LOG.info("Finished loading tracks. took " + (System.currentTimeMillis() - startLoadingTime) + "ms");
		return tracksFolder;
	}

	private void loadGPXFolder(@NonNull TrackFolder trackFolder, @NonNull String subfolder, boolean updateSmartFolder) {
		File folderFile = trackFolder.getDirFile();
		File[] files = GPXFolderUtils.listFilesSorted(sortByMode, folderFile);
		for (File file : files) {
			if (file.isDirectory()) {
				TrackFolder folder = new TrackFolder(file, trackFolder);
				trackFolder.addSubFolder(folder);
				loadGPXFolder(folder, GPXFolderUtils.getSubfolderTitle(file, subfolder), updateSmartFolder);
			} else if (GpxUiHelper.isGpxFile(file)) {
				TrackItem trackItem = new TrackItem(file);
				trackItem.setDataItem(getDataItem(trackItem));
				trackFolder.addTrackItem(trackItem);
				if (updateSmartFolder) {
					smartFolderHelper.addTrackItemToSmartFolder(trackItem);
				}
			}
		}
	}

	@Nullable
	private GpxDataItem getDataItem(@NonNull TrackItem trackItem) {
		File file = trackItem.getFile();
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
	protected void onPostExecute(@NonNull TrackFolder folder) {
		if (listener != null) {
			listener.loadTracksFinished(folder);
		}
		smartFolderHelper.notifyUpdateListeners();
	}

	public interface LoadTracksListener {

		@UiThread
		default void loadTracksStarted() {
		}

		@WorkerThread
		default void tracksLoaded(@NonNull TrackFolder folder) {
		}

		@UiThread
		void loadTracksFinished(@NonNull TrackFolder folder);
	}
}