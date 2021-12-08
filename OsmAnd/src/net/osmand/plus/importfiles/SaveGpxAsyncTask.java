package net.osmand.plus.importfiles;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.ZIP_EXT;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper.OnGpxImportCompleteListener;
import net.osmand.plus.importfiles.ImportHelper.OnSuccessfulGpxImport;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class SaveGpxAsyncTask extends AsyncTask<Void, Void, String> {

	private final OsmandApplication app;
	private final ImportHelper importHelper;

	private final GPXFile gpxFile;
	private final String name;
	private final OnSuccessfulGpxImport onGpxImport;
	private final boolean useImportDir;

	SaveGpxAsyncTask(@NonNull OsmandApplication app,
					 @NonNull ImportHelper importHelper,
					 @NonNull GPXFile gpxFile,
					 @NonNull String name,
					 @Nullable OnSuccessfulGpxImport onGpxImport,
					 boolean useImportDir) {
		this.app = app;
		this.importHelper = importHelper;
		this.gpxFile = gpxFile;
		this.name = name;
		this.onGpxImport = onGpxImport;
		this.useImportDir = useImportDir;
	}

	@Override
	protected String doInBackground(Void... nothing) {
		return saveImport(gpxFile, name, useImportDir);
	}

	private String saveImport(GPXFile gpxFile, String fileName, boolean useImportDir) {
		final String warning;
		if (gpxFile.isEmpty() || fileName == null) {
			warning = app.getString(R.string.error_reading_gpx);
		} else {
			final File importDir;
			if (useImportDir) {
				importDir = app.getAppPath(GPX_IMPORT_DIR);
			} else {
				importDir = app.getAppPath(GPX_INDEX_DIR);
			}
			//noinspection ResultOfMethodCallIgnored
			importDir.mkdirs();
			if (importDir.exists() && importDir.isDirectory() && importDir.canWrite()) {
				final WptPt pt = gpxFile.findPointToShow();
				final File toWrite = getFileToSave(fileName, importDir, pt);
				boolean destinationExists = toWrite.exists();
				Exception e = GPXUtilities.writeGpxFile(toWrite, gpxFile);
				if (e == null) {
					gpxFile.path = toWrite.getAbsolutePath();
					File file = new File(gpxFile.path);
					if (!destinationExists) {
						GpxDataItem item = new GpxDataItem(file, gpxFile);
						app.getGpxDbHelper().add(item);
					} else {
						GpxDataItem item = app.getGpxDbHelper().getItem(file);
						if (item != null) {
							app.getGpxDbHelper().clearAnalysis(item);
						}
					}

					warning = null;
				} else {
					warning = app.getString(R.string.error_reading_gpx);
				}
			} else {
				warning = app.getString(R.string.sd_dir_not_accessible);
			}
		}
		return warning;
	}

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
		return new File(importDir, fileName);
	}

	@Override
	protected void onPostExecute(final String warning) {
		boolean success = Algorithms.isEmpty(warning);

		if (success) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
			if (selectedGpxFile != null) {
				selectedGpxFile.setGpxFile(gpxFile, app);
			}
			importHelper.showNeededScreen(onGpxImport, gpxFile);
		} else {
			app.showToastMessage(warning);
		}
		OnGpxImportCompleteListener listener = importHelper.getGpxImportCompleteListener();
		if (listener != null) {
			listener.onSaveComplete(success, gpxFile);
		}
	}
}
