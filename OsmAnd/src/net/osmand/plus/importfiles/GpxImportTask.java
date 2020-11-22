package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.util.Algorithms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class GpxImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private ImportHelper importHelper;
	private Uri gpxFile;
	private String fileName;
	private long fileSize;

	private boolean save;
	private boolean useImportDir;
	private boolean showInDetailsActivity;

	public GpxImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
						 @NonNull Uri gpxFile, @NonNull String fileName, boolean save, boolean useImportDir,
						 boolean showInDetailsActivity) {
		super(activity);
		this.importHelper = importHelper;
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.save = save;
		this.useImportDir = useImportDir;
		this.showInDetailsActivity = showInDetailsActivity;
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
		importHelper.handleResult(result, fileName, fileSize, save, useImportDir, false, showInDetailsActivity);
	}
}