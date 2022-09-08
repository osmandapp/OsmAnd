package net.osmand.plus.importfiles;

import static net.osmand.plus.importfiles.KmlImportTask.convertKmlToGpxStream;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class GpxOrFavouritesImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private final ImportHelper importHelper;
	private final Uri fileUri;
	private final String fileName;
	private long fileSize;

	private final boolean save;
	private final boolean useImportDir;
	private final boolean forceImportGpx;
	private final boolean forceImportFavourites;

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
			if (is != null && fileName != null) {
				fileSize = is.available();
				if (fileName.endsWith(ImportHelper.KML_SUFFIX)) {
					InputStream gpxStream = convertKmlToGpxStream(is);
					if (gpxStream != null) {
						fileSize = gpxStream.available();
						return GPXUtilities.loadGPXFile(gpxStream);
					}
				} else if (fileName.endsWith(ImportHelper.KMZ_SUFFIX)) {
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
				} else if (fileName.endsWith(IndexConstants.ZIP_EXT)) {
					zis = new ZipInputStream(is);
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						if (entry.getName().endsWith(IndexConstants.GPX_FILE_EXT)) {
							return GPXUtilities.loadGPXFile(zis);
						}
					}
				} else {
					return GPXUtilities.loadGPXFile(is);
				}
			}
		} catch (IOException | SecurityException | IllegalStateException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
			Algorithms.closeStream(zis);
		}
		return null;
	}

	@Override
	protected void onPostExecute(GPXFile gpxFile) {
		hideProgress();
		importHelper.importGpxOrFavourites(gpxFile, fileName, fileSize, save, useImportDir, forceImportFavourites, forceImportGpx);
	}
}