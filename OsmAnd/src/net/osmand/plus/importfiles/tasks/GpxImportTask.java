package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.importfiles.ImportHelper.KML_SUFFIX;
import static net.osmand.plus.importfiles.ImportHelper.KMZ_SUFFIX;

import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.helper.ImportGpx;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okio.Okio;

public class GpxImportTask extends BaseImportAsyncTask<Void, Void, Pair<GpxFile, Long>> {

	private final Uri uri;
	private final String fileName;
	private final CallbackWithObject<Pair<GpxFile, Long>> callback;

	public GpxImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
	                     @NonNull String fileName, @NonNull CallbackWithObject<Pair<GpxFile, Long>> callback) {
		super(activity);
		this.uri = uri;
		this.fileName = fileName;
		this.callback = callback;
	}

	@Override
	protected Pair<GpxFile, Long> doInBackground(Void... nothing) {
		InputStream is = null;
		try {
			boolean createTmpFile = !CollectionUtils.endsWithAny(fileName, KML_SUFFIX, KMZ_SUFFIX, ZIP_EXT);
			if (createTmpFile) {
				File tmpDir = FileUtils.getTempDir(app);
				File file = new File(tmpDir, System.currentTimeMillis() + "_" + fileName);
				String error = ImportHelper.copyFile(app, file, uri, true, false);
				if (error == null) {
					long fileSize = file.length();
					GpxFile gpxFile = SharedUtil.loadGpxFile(file);
					return new Pair<>(gpxFile, fileSize);
				}
			} else {
				is = app.getContentResolver().openInputStream(uri);
				if (is != null) {
					kotlin.Pair<GpxFile, Long> gpxInfo = ImportGpx.INSTANCE.loadGpxWithFileSize(Okio.source(is), fileName);
					return new Pair<>(gpxInfo.getFirst(), gpxInfo.getSecond());
				}
			}
		} catch (IOException | SecurityException | IllegalStateException e) {
			ImportHelper.LOG.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Pair<GpxFile, Long> gpxInfo) {
		hideProgress();
		notifyImportFinished();
		callback.processResult(gpxInfo);
	}
}