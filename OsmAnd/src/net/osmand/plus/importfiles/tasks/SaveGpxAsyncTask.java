package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class SaveGpxAsyncTask extends AsyncTask<Void, Void, Pair<String, File>> {

	private final OsmandApplication app;

	private final GPXFile gpxFile;
	private final String fileName;
	private final File destinationDir;
	private final SaveImportedGpxListener listener;
	private final boolean overwrite;
	private boolean isTrackSelected = false;

	public SaveGpxAsyncTask(@NonNull OsmandApplication app,
	                        @NonNull GPXFile gpxFile,
	                        @NonNull File destinationDir,
	                        @NonNull String fileName,
	                        @Nullable SaveImportedGpxListener listener,
	                        boolean overwrite) {
		this.app = app;
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.destinationDir = destinationDir;
		this.listener = listener;
		this.overwrite = overwrite;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onGpxSavingStarted();
		}
	}

	@Override
	protected Pair<String, File> doInBackground(Void... nothing) {
		if (gpxFile.isEmpty()) {
			return new Pair<>(app.getString(R.string.error_reading_gpx), null);
		}

		String warning;
		File resultFile = null;
		//noinspection ResultOfMethodCallIgnored
		destinationDir.mkdirs();
		if (destinationDir.exists() && destinationDir.isDirectory() && destinationDir.canWrite()) {
			WptPt pt = gpxFile.findPointToShow();
			File toWrite = getFileToSave(fileName, destinationDir, pt);
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(toWrite.getAbsolutePath());
			Exception exception = GPXUtilities.writeGpxFile(toWrite, gpxFile);

			if (listener != null) {
				listener.onGpxSaved(exception != null ? exception.getMessage() : null, gpxFile);
			}
			if (exception == null) {
				gpxFile.path = toWrite.getAbsolutePath();
				resultFile = new File(gpxFile.path);
				if (overwrite) {
					app.getGpxDbHelper().remove(toWrite);
					if (selected != null) {
						isTrackSelected = true;
						GpxSelectionParams params = GpxSelectionParams.newInstance()
								.hideFromMap().syncGroup().saveSelection();
						helper.selectGpxFile(selected.getGpxFile(), params);
					}
				}
				GpxDataItem item = new GpxDataItem(resultFile, gpxFile);
				app.getGpxDbHelper().add(item);

				warning = null;
			} else {
				warning = app.getString(R.string.error_reading_gpx);
			}
		} else {
			warning = app.getString(R.string.sd_dir_not_accessible);
		}

		return new Pair<>(warning, resultFile);
	}

	@NonNull
	private File getFileToSave(String fileName, File importDir, WptPt pt) {
		if (Algorithms.isEmpty(fileName)) {
			fileName = "import_" + new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time));
		} else if (fileName.endsWith(ImportHelper.KML_SUFFIX)) {
			fileName = fileName.replace(ImportHelper.KML_SUFFIX, "");
		} else if (fileName.endsWith(ImportHelper.KMZ_SUFFIX)) {
			fileName = fileName.replace(ImportHelper.KMZ_SUFFIX, "");
		} else if (fileName.endsWith(ZIP_EXT)) {
			fileName = fileName.replace(ZIP_EXT, "");
		}
		if (!fileName.endsWith(GPX_FILE_EXT)) {
			fileName = fileName + GPX_FILE_EXT;
		}
		File destFile = new File(importDir, fileName);
		while (destFile.exists() && !overwrite) {
			fileName = AndroidUtils.createNewFileName(fileName);
			destFile = new File(importDir, fileName);
		}
		return destFile;
	}

	@Override
	protected void onPostExecute(Pair<String, File> result) {
		String warning = result.first;
		File file = result.second;
		if (listener != null) {
			List<String> warnings = Algorithms.isEmpty(warning)
					? Collections.emptyList()
					: Collections.singletonList(warning);
			listener.onGpxSavingFinished(warnings);
		}
		if (isTrackSelected && file != null) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			GpxFileLoaderTask.loadGpxFile(file, app.getOsmandMap().getMapView().getMapActivity(), gpxFile -> {
				GpxSelectionParams selectionParams = GpxSelectionParams.newInstance()
						.showOnMap().selectedAutomatically().saveSelection();
				helper.selectGpxFile(gpxFile, selectionParams);
				return true;
			});
		}
	}
}
