package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.util.Algorithms;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.osmand.plus.importfiles.ImportHelper.KML_SUFFIX;
import static net.osmand.plus.importfiles.KmlImportTask.convertKmlToGpxStream;

class KmzImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private ImportHelper importHelper;
	private Uri uri;
	private String name;
	private long fileSize;

	private boolean save;
	private boolean useImportDir;

	public KmzImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
						 @NonNull Uri uri, @NonNull String name, boolean save, boolean useImportDir) {
		super(activity);
		this.importHelper = importHelper;
		this.uri = uri;
		this.name = name;
		this.save = save;
		this.useImportDir = useImportDir;
	}

	@Override
	protected GPXFile doInBackground(Void... voids) {
		InputStream is = null;
		ZipInputStream zis = null;
		try {
			is = app.getContentResolver().openInputStream(uri);
			if (is != null) {
				zis = new ZipInputStream(is);

				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().endsWith(KML_SUFFIX)) {
						InputStream gpxStream = convertKmlToGpxStream(zis);
						if (gpxStream != null) {
							fileSize = gpxStream.available();
							return GPXUtilities.loadGPXFile(gpxStream);
						}
					}
				}
			}
		} catch (Exception e) {
			ImportHelper.log.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
			Algorithms.closeStream(zis);
		}
		return null;
	}

	@Override
	protected void onPostExecute(GPXFile result) {
		hideProgress();
		importHelper.handleResult(result, name, fileSize, save, useImportDir, false);
	}
}