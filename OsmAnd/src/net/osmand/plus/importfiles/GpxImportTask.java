package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.util.Algorithms;
import net.osmand.plus.importfiles.ImportHelper.OnSuccessfulGpxImport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class GpxImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private final ImportHelper importHelper;
	private final Uri gpxFile;
	private final String fileName;
	private long fileSize;

	private final boolean save;
	private final boolean useImportDir;
	private final OnSuccessfulGpxImport onGpxImport;

	public GpxImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
	                     @NonNull Uri gpxFile, @NonNull String fileName, @Nullable OnSuccessfulGpxImport onGpxImport,
	                     boolean useImportDir, boolean save) {
		super(activity);
		this.importHelper = importHelper;
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.onGpxImport = onGpxImport;
		this.save = save;
		this.useImportDir = useImportDir;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		InputStream is = null;
		try {
			is = app.getContentResolver().openInputStream(gpxFile);
			if (is != null) {
				fileSize = is.available();
				return GPXUtilities.loadGPXFile(is);
			}
		} catch (FileNotFoundException e) {
			//
		} catch (IOException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} catch (SecurityException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
		}
		return null;
	}

	@Override
	protected void onPostExecute(GPXFile result) {
		hideProgress();
		importHelper.handleResult(result, fileName, onGpxImport, fileSize, save, useImportDir, false);
	}
}