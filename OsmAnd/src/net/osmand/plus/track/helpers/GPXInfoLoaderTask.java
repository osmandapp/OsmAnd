package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.base.BaseLoadAsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GPXInfoLoaderTask extends BaseLoadAsyncTask<Void, Void, List<GPXFile>> {

	private final List<GPXInfo> gpxInfos;
	private final CallbackWithObject<List<GPXFile>> callback;

	public GPXInfoLoaderTask(@NonNull FragmentActivity activity, @NonNull List<GPXInfo> gpxInfos,
	                         @NonNull CallbackWithObject<List<GPXFile>> callback) {
		super(activity);
		this.gpxInfos = gpxInfos;
		this.callback = callback;
	}

	@Override
	protected List<GPXFile> doInBackground(Void... voids) {
		List<GPXFile> gpxFiles = new ArrayList<>();
		for (GPXInfo gpxInfo : gpxInfos) {
			GPXFile gpxFile = gpxInfo.getGpxFile();
			if (gpxFile == null) {
				File file = gpxInfo.getFile();
				if (file != null) {
					gpxInfo.setGpxFile(GPXUtilities.loadGPXFile(file));
				}
			}
			gpxFile = gpxInfo.getGpxFile();
			if (gpxFile != null) {
				gpxFiles.add(gpxFile);
			}
		}
		return gpxFiles;
	}

	@Override
	protected void onPostExecute(List<GPXFile> gpxFiles) {
		hideProgress();
		callback.processResult(gpxFiles);
	}
}