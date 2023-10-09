package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.track.helpers.GPXFolderUtils.getSubfolderTitle;
import static net.osmand.plus.track.helpers.GPXFolderUtils.listFilesSorted;
import static net.osmand.plus.track.helpers.GpxUiHelper.isGpxFile;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;

import java.io.File;

public class TrackFolderLoaderTask extends AsyncTask<Void, Void, TrackFolder> {

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
		smartFolderHelper.resetSmartFolders();
	}

	@Override
	protected TrackFolder doInBackground(Void... voids) {
		TrackFolder tracksFolder = new TrackFolder(dir, null);
		loadGPXFolder(tracksFolder, "", true);
		if (listener != null) {
			listener.tracksLoaded(tracksFolder);
		}
		return tracksFolder;
	}

	private void loadGPXFolder(@NonNull TrackFolder trackFolder, @NonNull String subfolder, boolean updateSmartFolder) {
		File folderFile = trackFolder.getDirFile();
		File[] files = listFilesSorted(sortByMode, folderFile);
		for (File file : files) {
			if (file.isDirectory()) {
				TrackFolder folder = new TrackFolder(file, trackFolder);
				trackFolder.addSubFolder(folder);
				loadGPXFolder(folder, getSubfolderTitle(file, subfolder), updateSmartFolder);
			} else if (isGpxFile(file)) {
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