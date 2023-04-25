package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.track.helpers.GPXFolderUtils.getSubfolderTitle;
import static net.osmand.plus.track.helpers.GPXFolderUtils.listFilesSorted;
import static net.osmand.plus.track.helpers.GpxUiHelper.isGpxFile;
import static net.osmand.util.Algorithms.objectEquals;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.data.GPXInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoadGpxInfosTask extends AsyncTask<Void, GPXInfo, List<GPXInfo>> {

	private final OsmandApplication app;
	private final TracksSortByMode sortByMode;
	private final LoadTracksListener listener;

	private List<GPXInfo> gpxInfos;

	public LoadGpxInfosTask(@NonNull OsmandApplication app, @NonNull LoadTracksListener listener) {
		this.app = app;
		this.listener = listener;
		this.sortByMode = app.getSettings().TRACKS_SORT_BY_MODE.get();
	}

	public List<GPXInfo> getGpxInfos() {
		return gpxInfos;
	}

	@Override
	protected List<GPXInfo> doInBackground(Void... voids) {
		List<GPXInfo> result = new ArrayList<>();
		loadGPXData(app.getAppPath(GPX_INDEX_DIR), result);
		return result;
	}

	public void loadFile(GPXInfo... loaded) {
		publishProgress(loaded);
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadTracksStarted();
		}
	}

	@Override
	protected void onProgressUpdate(GPXInfo... values) {
		if (listener != null) {
			listener.loadTracksProgress(values);
		}
	}

	@Override
	protected void onPostExecute(List<GPXInfo> result) {
		this.gpxInfos = result;
		if (listener != null) {
			listener.loadTracksFinished();
		}
	}

	private void loadGPXData(@NonNull File mapPath, @NonNull List<GPXInfo> result) {
		if (mapPath.canRead()) {
			List<GPXInfo> progress = new ArrayList<>();
			loadGPXFolder(mapPath, result, progress, "");
			if (!progress.isEmpty()) {
				loadFile(progress.toArray(new GPXInfo[0]));
			}
		}
	}

	private void loadGPXFolder(File mapPath, List<GPXInfo> result,
	                           List<GPXInfo> progress, String gpxSubfolder) {
		File[] listFiles = listFilesSorted(sortByMode, mapPath);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				loadGPXFolder(file, result, progress, getSubfolderTitle(file, gpxSubfolder));
			} else if (isGpxFile(file)) {
				GPXInfo info = new GPXInfo(file.getName(), file);
				info.subfolder = gpxSubfolder;
				result.add(info);
				progress.add(info);
				if (progress.size() > 7) {
					loadFile(progress.toArray(new GPXInfo[0]));
					progress.clear();
				}
			}
		}
	}

	public static void addLocalIndexInfo(@NonNull GPXInfo info,
	                                     @NonNull List<String> category,
	                                     @NonNull Map<String, List<GPXInfo>> data) {
		String categoryName;
		if (info.getGpxFile() != null && info.isCurrentRecordingTrack()) {
			categoryName = info.getName();
		} else {
			// local_indexes_cat_gpx now obsolete in new UI screen which shows only GPX data
			// categoryName = app.getString(R.string.local_indexes_cat_gpx) + " " + info.subfolder;
			categoryName = "" + info.subfolder;
		}
		int found = -1;
		// search from end
		for (int i = category.size() - 1; i >= 0; i--) {
			String cat = category.get(i);
			if (objectEquals(categoryName, cat)) {
				found = i;
				break;
			}
		}
		if (found == -1) {
			found = category.size();
			category.add(categoryName);
		}
		if (!data.containsKey(category.get(found))) {
			data.put(category.get(found), new ArrayList<GPXInfo>());
		}
		data.get(category.get(found)).add(info);
	}

	public interface LoadTracksListener {

		void loadTracksStarted();

		void loadTracksProgress(GPXInfo[] gpxInfos);

		void loadTracksFinished();
	}
}
