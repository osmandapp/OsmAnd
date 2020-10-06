package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.util.Algorithms;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.osmand.plus.importfiles.KmlImportTask.loadGpxFromKml;

class GpxOrFavouritesImportTask extends BaseImportAsyncTask<Void, Void, GPXFile> {

	private ImportHelper importHelper;
	private Uri fileUri;
	private String fileName;
	private boolean save;
	private boolean useImportDir;
	private boolean forceImportFavourites;
	private boolean forceImportGpx;

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
				if (fileName != null && fileName.endsWith(ImportHelper.KML_SUFFIX)) {
					return loadGpxFromKml(is);
				} else if (fileName != null && fileName.endsWith(ImportHelper.KMZ_SUFFIX)) {
					try {
						zis = new ZipInputStream(is);
						ZipEntry entry;
						while ((entry = zis.getNextEntry()) != null) {
							if (entry.getName().endsWith(ImportHelper.KML_SUFFIX)) {
								return loadGpxFromKml(zis);
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
		} catch (SecurityException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
			Algorithms.closeStream(zis);
		}
		return null;
	}

	@Override
	protected void onPostExecute(final GPXUtilities.GPXFile result) {
		hideProgress();
		importHelper.importGpxOrFavourites(result, fileName, save, useImportDir, forceImportFavourites, forceImportGpx);
	}
}