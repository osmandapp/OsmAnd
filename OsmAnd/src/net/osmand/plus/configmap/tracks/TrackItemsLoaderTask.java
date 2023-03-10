package net.osmand.plus.configmap.tracks;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackItemsLoaderTask extends AsyncTask<Void, TrackItem, Void> {

	private final OsmandApplication app;
	private final GpxDbHelper gpxDbHelper;
	private final GpxSelectionHelper gpxSelectionHelper;

	private final List<TrackItem> trackItems = new ArrayList<>();
	private final LoadTracksListener listener;

	public TrackItemsLoaderTask(@NonNull OsmandApplication app, @NonNull LoadTracksListener listener) {
		this.app = app;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.gpxSelectionHelper = app.getSelectedGpxHelper();
		this.listener = listener;
	}

	@Nullable
	public List<TrackItem> getTrackItems() {
		return trackItems;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadTracksStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		File dir = app.getAppPath(GPX_INDEX_DIR);
		if (dir.exists() && dir.canRead()) {
			List<TrackItem> progress = new ArrayList<>();
			loadGPXFolder(dir, progress, "");

			if (!progress.isEmpty()) {
				publishProgress(progress.toArray(new TrackItem[0]));
			}
		}
		return null;
	}

	private void loadGPXFolder(@NonNull File dir, @NonNull List<TrackItem> progress, @NonNull String subfolder) {
		File[] listFiles = dir.listFiles();
		if (Algorithms.isEmpty(listFiles)) {
			return;
		}
		for (File file : listFiles) {
			String name = file.getName();
			if (file.isDirectory()) {
				String sub = subfolder.length() == 0 ? name : subfolder + "/" + name;
				loadGPXFolder(file, progress, sub);
			} else if (file.isFile() && name.toLowerCase().endsWith(GPX_FILE_EXT)) {
				TrackItem trackItem = new TrackItem(file);
				trackItem.setDataItem(getDataItem(trackItem));
				trackItems.add(trackItem);

				progress.add(trackItem);
				if (progress.size() > 7) {
					publishProgress(progress.toArray(new TrackItem[0]));
					progress.clear();
				}
			}
		}
	}

	@Nullable
	private GpxDataItem getDataItem(@NonNull TrackItem trackItem) {
		GpxDataItemCallback callback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return TrackItemsLoaderTask.this.isCancelled();
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				if (item != null && item.getAnalysis() != null) {
					trackItem.setDataItem(item);
				}
			}
		};
		return gpxDbHelper.getItem(trackItem.getFile(), callback);
	}

	@Override
	protected void onPostExecute(Void result) {
		if (listener != null) {
			listener.loadTracksFinished();
		}
	}


	public interface LoadTracksListener {

		void loadTracksStarted();

		void loadTracksFinished();
	}
}