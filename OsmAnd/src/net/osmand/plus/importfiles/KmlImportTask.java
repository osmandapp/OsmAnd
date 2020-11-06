package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.helpers.Kml2Gpx;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

class KmlImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private ImportHelper importHelper;
	private Uri uri;
	private String name;
	private long fileSize;

	private boolean save;
	private boolean useImportDir;

	public KmlImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
						 @NonNull Uri uri, String name, boolean save, boolean useImportDir) {
		super(activity);
		this.importHelper = importHelper;
		this.uri = uri;
		this.name = name;
		this.save = save;
		this.useImportDir = useImportDir;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		InputStream is = null;
		try {
			is = app.getContentResolver().openInputStream(uri);
			if (is != null) {
				InputStream gpxStream = convertKmlToGpxStream(is);
				if (gpxStream != null) {
					fileSize = gpxStream.available();
					return GPXUtilities.loadGPXFile(gpxStream);
				}
			}
		} catch (FileNotFoundException e) {
			//
		} catch (SecurityException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} catch (IOException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
		}
		return null;
	}

	@Override
	protected void onPostExecute(GPXFile result) {
		hideProgress();
		importHelper.handleResult(result, name, fileSize, save, useImportDir, false);
	}

	protected static InputStream convertKmlToGpxStream(@NonNull InputStream is) {
		String result = Kml2Gpx.toGpx(is);
		if (result != null) {
			try {
				return new ByteArrayInputStream(result.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				ImportHelper.log.error(e.getMessage(), e);
			}
		}
		return null;
	}
}