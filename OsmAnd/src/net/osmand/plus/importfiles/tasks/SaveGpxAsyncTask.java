package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class SaveGpxAsyncTask extends AsyncTask<Void, Void, String> {

	public static final DateFormat GPX_FILE_DATE_FORMAT = new SimpleDateFormat("HH-mm_EEE", Locale.US);

	private final OsmandApplication app;

	private final GPXFile gpxFile;
	private final String fileName;
	private final File destinationDir;
	private final SaveImportedGpxListener listener;
	private final boolean overwrite;

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
	protected String doInBackground(Void... nothing) {
		if (gpxFile.isEmpty()) {
			return app.getString(R.string.error_reading_gpx);
		}
		//noinspection ResultOfMethodCallIgnored
		destinationDir.mkdirs();
		if (destinationDir.exists() && destinationDir.isDirectory() && destinationDir.canWrite()) {
			File toSave = getFileToSave(fileName, destinationDir, gpxFile);
			String error = saveFile(toSave);

			if (listener != null) {
				listener.onGpxSaved(error, gpxFile);
			}
			if (error == null) {
				processSavedFile(toSave);
			} else {
				return app.getString(R.string.error_reading_gpx);
			}
		} else {
			return app.getString(R.string.sd_dir_not_accessible);
		}
		return null;
	}

	@Nullable
	private String saveFile(@NonNull File toSave) {
		File file = !Algorithms.isEmpty(gpxFile.path) ? new File(gpxFile.path) : null;
		if (isTmpFileToMove(file)) {
			if (!FileUtils.move(file, toSave)) {
				return app.getString(R.string.error_reading_gpx);
			}
		} else {
			Exception exception = GPXUtilities.writeGpxFile(toSave, gpxFile);
			return exception != null ? exception.getMessage() : null;
		}
		return null;
	}

	private boolean isTmpFileToMove(@Nullable File file) {
		return file != null && file.exists() && Algorithms.objectEquals(file.getParentFile(), FileUtils.getTempDir(app));
	}

	private void processSavedFile(@NonNull File file) {
		gpxFile.path = file.getAbsolutePath();
		if (overwrite) {
			app.getGpxDbHelper().remove(file);

			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(file.getAbsolutePath());
			if (selected != null) {
				selected.setGpxFile(gpxFile, app);
				DeletePointsTask.syncGpx(app, gpxFile);
			}
		}
		GpxDataItem item = new GpxDataItem(app, file);
		item.readGpxParams(app, gpxFile);
		app.getGpxDbHelper().add(item);
		app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(file));
	}

	@NonNull
	private File getFileToSave(@NonNull String fileName, @NonNull File importDir, @NonNull GPXFile gpxFile) {
		if (Algorithms.isEmpty(fileName)) {
			long time = gpxFile.findPointToShow().time;
			fileName = "import_" + GPX_FILE_DATE_FORMAT.format(new Date(time));
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
	protected void onPostExecute(String warning) {
		if (listener != null) {
			List<String> warnings = Algorithms.isEmpty(warning)
					? Collections.emptyList()
					: Collections.singletonList(warning);
			listener.onGpxSavingFinished(warnings);
		}
	}
}
