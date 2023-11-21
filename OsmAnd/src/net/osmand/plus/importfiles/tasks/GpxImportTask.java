package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.importfiles.ImportHelper.KML_SUFFIX;
import static net.osmand.plus.importfiles.ImportHelper.KMZ_SUFFIX;

import android.net.Uri;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kotlin.Triple;

public class GpxImportTask extends BaseImportAsyncTask<Void, Void, GPXFile> {

	private final Uri uri;
	private File tmpFile;
	private final String fileName;
	private final CallbackWithObject<Triple<GPXFile, Long, File>> callback;
	private long fileSize;


	public GpxImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
	                     @NonNull String fileName, @NonNull CallbackWithObject<Triple<GPXFile, Long, File>> callback) {
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
			if (fileName.endsWith(KML_SUFFIX) ||
					fileName.endsWith(KMZ_SUFFIX) ||
					fileName.endsWith(ZIP_EXT)) {
				is = app.getContentResolver().openInputStream(uri);
				if (is != null && fileName != null) {
					fileSize = is.available();
					if (fileName.endsWith(KML_SUFFIX)) {
						return loadGPXFileFromKml(is);
					} else if (fileName.endsWith(KMZ_SUFFIX)) {
						zis = new ZipInputStream(is);
						return loadGPXFileFromKmz(zis);
					} else {
						zis = new ZipInputStream(is);
						return loadGPXFileFromZip(zis);
					}
				}
			} else {
				File tmpDir = FileUtils.getTempDir(app);
				if (!tmpDir.exists()) {
					tmpDir.mkdir();
				}
				tmpFile = FileUtils.saveUriToFileSystem(app, uri, fileName, tmpDir);
				if (tmpFile != null) {
					is = new FileInputStream(tmpFile);
					return GPXUtilities.loadGPXFile(is);
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
		callback.processResult(new Triple<>(gpxFile, fileSize, tmpFile));
	}
}