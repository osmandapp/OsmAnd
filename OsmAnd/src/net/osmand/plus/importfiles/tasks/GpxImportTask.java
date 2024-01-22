package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.importfiles.ImportHelper.KML_SUFFIX;
import static net.osmand.plus.importfiles.ImportHelper.KMZ_SUFFIX;

import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.helpers.Kml2Gpx;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GpxImportTask extends BaseImportAsyncTask<Void, Void, GPXFile> {

	private final Uri uri;
	private final String fileName;
	private final CallbackWithObject<Pair<GPXFile, Long>> callback;
	private long fileSize;


	public GpxImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
	                     @NonNull String fileName, @NonNull CallbackWithObject<Pair<GPXFile, Long>> callback) {
		super(activity);
		this.uri = uri;
		this.fileName = fileName;
		this.callback = callback;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		InputStream is = null;
		ZipInputStream zis = null;
		try {
			boolean createTmpFile = !CollectionUtils.endsWithAny(fileName, KML_SUFFIX, KMZ_SUFFIX, ZIP_EXT);
			if (createTmpFile) {
				File tmpDir = FileUtils.getTempDir(app);
				File file = new File(tmpDir, System.currentTimeMillis() + "_" + fileName);
				String error = ImportHelper.copyFile(app, file, uri, true, false);
				if (error == null) {
					fileSize = file.length();
					return GPXUtilities.loadGPXFile(file);
				}
			} else {
				is = app.getContentResolver().openInputStream(uri);
				if (is != null) {
					fileSize = is.available();
					if (fileName.endsWith(KML_SUFFIX)) {
						return loadGPXFileFromKml(is);
					} else if (fileName.endsWith(KMZ_SUFFIX)) {
						zis = new ZipInputStream(is);
						return loadGPXFileFromKmz(zis);
					} else if (fileName.endsWith(ZIP_EXT)) {
						zis = new ZipInputStream(is);
						return loadGPXFileFromZip(zis);
					}
				}
			}
		} catch (IOException | SecurityException | IllegalStateException e) {
			ImportHelper.LOG.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
			Algorithms.closeStream(zis);
		}
		return null;
	}

	@Nullable
	private GPXFile loadGPXFileFromKml(@NonNull InputStream stream) throws IOException {
		InputStream gpxStream = convertKmlToGpxStream(stream);
		if (gpxStream != null) {
			fileSize = gpxStream.available();
			return GPXUtilities.loadGPXFile(gpxStream);
		}
		return null;
	}

	@Nullable
	private GPXFile loadGPXFileFromZip(@NonNull ZipInputStream stream) throws IOException {
		ZipEntry entry;
		while ((entry = stream.getNextEntry()) != null) {
			if (entry.getName().endsWith(GPX_FILE_EXT)) {
				return GPXUtilities.loadGPXFile(stream);
			}
		}
		return null;
	}

	@Nullable
	private GPXFile loadGPXFileFromKmz(@NonNull ZipInputStream stream) throws IOException {
		ZipEntry entry;
		while ((entry = stream.getNextEntry()) != null) {
			if (entry.getName().endsWith(KML_SUFFIX)) {
				InputStream gpxStream = convertKmlToGpxStream(stream);
				if (gpxStream != null) {
					fileSize = gpxStream.available();
					return GPXUtilities.loadGPXFile(gpxStream);
				}
			}
		}
		return null;
	}

	@Nullable
	private InputStream convertKmlToGpxStream(@NonNull InputStream is) {
		String result = Kml2Gpx.toGpx(is);
		if (result != null) {
			try {
				return new ByteArrayInputStream(result.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				ImportHelper.LOG.error(e.getMessage(), e);
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(GPXFile gpxFile) {
		hideProgress();
		notifyImportFinished();
		callback.processResult(new Pair<>(gpxFile, fileSize));
	}
}