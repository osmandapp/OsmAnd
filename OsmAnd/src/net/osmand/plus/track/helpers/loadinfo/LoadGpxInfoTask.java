package net.osmand.plus.track.helpers.loadinfo;

import static net.osmand.plus.track.helpers.GPXFolderUtils.getSubfolderTitle;
import static net.osmand.plus.track.helpers.GPXFolderUtils.listFilesSorted;
import static net.osmand.plus.track.helpers.GpxUiHelper.isGpxFile;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.data.GPXFolderInfo;
import net.osmand.plus.track.data.GPXInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LoadGpxInfoTask extends AsyncTask<Void, GPXInfo, GPXFolderInfo> {

	private static final int PROGRESS_LIMIT = 7;

	private final File rootDir;
	private final TracksSortByMode sortingMode;
	private final LoadGpxInfoListener listener;
	private final List<GPXInfo> progress = new ArrayList<>();


	public LoadGpxInfoTask(@NonNull OsmandApplication app, @NonNull File rootDir,
	                       @NonNull LoadGpxInfoListener listener) {
		this.rootDir = rootDir;
		this.listener = listener;
		OsmandSettings settings = app.getSettings();
		this.sortingMode = settings.TRACKS_SORT_BY_MODE.get();
	}

	@Override
	protected GPXFolderInfo doInBackground(Void... voids) {
		return loadGPXData(rootDir);
	}

	private GPXFolderInfo loadGPXData(@NonNull File rootDir) {
		GPXFolderInfo result = new GPXFolderInfo(rootDir);
		if (rootDir.canRead()) {
			loadGPXFolder(result, "");
			if (progress.size() > 0) {
				notifyUpdateProgress();
			}
		}
		return result;
	}

	private void loadGPXFolder(@NonNull GPXFolderInfo folder, String subfolderTitle) {
		File path = folder.getPath();
		File[] listFiles = listFilesSorted(sortingMode, path);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				GPXFolderInfo subfolder = new GPXFolderInfo(file);
				folder.addFolder(subfolder);
				loadGPXFolder(subfolder, getSubfolderTitle(file, subfolderTitle));
			} else if (isGpxFile(file)) {
				GPXInfo info = new GPXInfo(file.getName(), file);
				info.subfolder = subfolderTitle;
				folder.addFile(info);
				progress.add(info);
				if (progress.size() > PROGRESS_LIMIT) {
					notifyUpdateProgress();
					progress.clear();
				}
			}
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onGpxInfoLoadStarted();
		}
	}

	public void notifyUpdateProgress() {
		publishProgress(progress.toArray(new GPXInfo[0]));
	}

	@Override
	protected void onProgressUpdate(GPXInfo ... values) {
		if (listener != null) {
			listener.onUpdateGpxInfoLoadProgress(values);
		}
	}

	@Override
	protected void onPostExecute(GPXFolderInfo result) {
		if (listener != null) {
			listener.onGpxInfoLoadFinished(result);
		}
	}

}
