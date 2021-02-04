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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.osmand.plus.importfiles.KmlImportTask.convertKmlToGpxStream;

class GpxOrFavouritesImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private ImportHelper importHelper;
	private Uri fileUri;
	private String fileName;
	private long fileSize;

	private boolean save;
	private boolean useImportDir;
	private boolean forceImportGpx;
	private boolean forceImportFavourites;

	public GpxOrFavouritesImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
									 @NonNull Uri fileUri, String fileName, boolean save, boolean useImportDir,
									 boolean forceImportFavourites, boolean forceImportGpx) {
		super(activity);
		this.importHelper = importHelper;
		this.fileUri = fileUri;
		this.fileName = fileName;
		this.save = save;
		this.useImportDir = useImportDir;
		this.forceImportFavourites = forceImportFavourites;
		this.forceImportGpx = forceImportGpx;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		InputStream is = null;
		ZipInputStream zis = null;
		try {
			is = app.getContentResolver().openInputStream(fileUri);
			if (is != null) {
				fileSize = is.available();
				if (fileName != null && fileName.endsWith(ImportHelper.KML_SUFFIX)) {
					InputStream gpxStream = convertKmlToGpxStream(is);
					if (gpxStream != null) {
						fileSize = gpxStream.available();
						return GPXUtilities.loadGPXFile(gpxStream);
					}
				} else if (fileName != null && fileName.endsWith(ImportHelper.KMZ_SUFFIX)) {
					try {
						zis = new ZipInputStream(is);
						ZipEntry entry;
						while ((entry = zis.getNextEntry()) != null) {
							if (entry.getName().endsWith(ImportHelper.KML_SUFFIX)) {
								InputStream gpxStream = convertKmlToGpxStream(zis);
								if (gpxStream != null) {
									fileSize = gpxStream.available();
									return GPXUtilities.loadGPXFile(gpxStream);
								}
							}
						}
					} catch (Exception e) {
						return null;
					}
				} else {
					return GPXUtilities.loadGPXFile(is);
				}
			}
		} catch (FileNotFoundException e) {
			//
		} catch (IOException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} catch (SecurityException e) {
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
		importHelper.importGpxOrFavourites(result, fileName, fileSize, save, useImportDir, forceImportFavourites, forceImportGpx);
	}
}