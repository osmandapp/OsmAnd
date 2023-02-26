package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TracksSortMode;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GPXInfoLoaderTask extends AsyncTask<Void, GPXInfo, Void> {

	private final OsmandApplication app;
	private final GpxDbHelper gpxDbHelper;
	private final GpxSelectionHelper gpxSelectionHelper;

	private final List<GPXInfo> gpxInfos = new ArrayList<>();
	private final TracksSortMode sortByMode;
	private final LoadTracksListener listener;

	public GPXInfoLoaderTask(@NonNull OsmandApplication app, @NonNull LoadTracksListener listener) {
		this.app = app;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.gpxSelectionHelper = app.getSelectedGpxHelper();
		this.sortByMode = app.getSettings().TRACKS_SORT_MODE.get();
		this.listener = listener;
	}

	@Nullable
	public List<GPXInfo> getGpxInfos() {
		return gpxInfos;
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
			List<GPXInfo> progress = new ArrayList<>();
			loadGPXFolder(dir, progress, "");

			if (!progress.isEmpty()) {
				publishProgress(progress.toArray(new GPXInfo[0]));
			}
		}
		return null;
	}

	private void loadGPXFolder(@NonNull File dir, @NonNull List<GPXInfo> progress, @NonNull String subfolder) {
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
				GPXInfo gpxInfo = new GPXInfo(name, file);
				gpxInfo.subfolder = subfolder;

				SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(file.getAbsolutePath());
				if (selectedGpxFile != null) {
					gpxInfo.setGpxFile(selectedGpxFile.getGpxFile());
				} else {
					gpxInfo.setGpxFile(GPXUtilities.loadGPXFile(file));
				}
				gpxInfo.setDataItem(getDataItem(gpxInfo));

				gpxInfos.add(gpxInfo);

				progress.add(gpxInfo);
				if (progress.size() > 7) {
					publishProgress(progress.toArray(new GPXInfo[0]));
					progress.clear();
				}
			}
		}
	}

	@Nullable
	private GpxDataItem getDataItem(@NonNull GPXInfo gpxInfo) {
		GpxDataItemCallback callback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				if (item != null && item.getAnalysis() != null) {
					gpxInfo.setDataItem(item);
				}
			}
		};
		return gpxDbHelper.getItem(gpxInfo.getFile(), callback);
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