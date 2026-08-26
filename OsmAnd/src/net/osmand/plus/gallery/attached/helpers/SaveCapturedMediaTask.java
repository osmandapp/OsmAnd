package net.osmand.plus.gallery.attached.helpers;

import static net.osmand.plus.settings.enums.MediaStorageType.MAIN_STORAGE;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.mediastorage.MediaDirType;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageUtils;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;
import net.osmand.plus.settings.mediastorage.MediaTarget;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.media.MediaFileNameFormat;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class SaveCapturedMediaTask extends AsyncTask<Void, Void, Link> {

	private static final Log LOG = PlatformUtil.getLog(SaveCapturedMediaTask.class);

	private final OsmandApplication app;
	private final File sourceFile;
	private final MediaTarget target;
	private final String name;
	private final String mimeType;
	private final CallbackWithObject<Link> callback;

	SaveCapturedMediaTask(@NonNull OsmandApplication app, @NonNull File sourceFile, @NonNull MediaTarget target,
	                      @Nullable String name, @NonNull String mimeType, @NonNull CallbackWithObject<Link> callback) {
		this.app = app;
		this.sourceFile = sourceFile;
		this.target = target;
		this.name = name;
		this.mimeType = mimeType;
		this.callback = callback;
	}

	@Override
	protected Link doInBackground(Void... params) {
		logDebug("Save captured media started: source=" + sourceFile + ", target=" + target.getFileName() + ", mimeType=" + mimeType);
		if (!sourceFile.exists() || sourceFile.length() == 0) {
			logDebug("Save captured media skipped: empty source=" + sourceFile);
			discardTarget();
			Algorithms.removeAllFiles(sourceFile);
			return null;
		}
		Link link = copyToTarget(target);
		if (link == null) {
			logDebug("Save captured media primary target failed, trying internal fallback: target=" + target.getFileName());
			link = copyToInternalFallback();
		}
		if (link != null) {
			Algorithms.removeAllFiles(sourceFile);
			logDebug("Save captured media finished: href=" + link.getHref() + ", sourceDeleted=true");
		}
		return link;
	}

	@Nullable
	private Link copyToTarget(@NonNull MediaTarget target) {
		try {
			InputStream inputStream = new FileInputStream(sourceFile);
			String error = MediaStorageUtils.copyToTarget(inputStream, target, null);
			if (error != null) {
				LOG.warn("Failed to copy captured media to target: " + target.getFileName() + ", " + error);
				return null;
			}
			logDebug("Captured media copied to target: " + target.getFileName() + ", href=" + target.getHref());
			return new Link(target.getHref(), name, mimeType);
		} catch (Exception e) {
			LOG.warn("Failed to copy captured media to target: " + target.getFileName(), e);
			return null;
		}
	}

	@Nullable
	private Link copyToInternalFallback() {
		MediaStorageHelper mediaStorageHelper = new MediaStorageHelper(app);
		MediaStorageLocation location = MediaStorageLocation.fromSelection(MAIN_STORAGE, null);
		String extension = Algorithms.getFileNameExtension(target.getFileName());
		MediaDirType dirType = MediaDirType.fromMimeTypeOrExtension(mimeType, extension);
		String fileName = MediaFileNameFormat.createUniqueGeneratedMediaFileName(target.getFileName(),
				name -> mediaStorageHelper.mediaFileExists(location, dirType, name));
		MediaTarget fallbackTarget = mediaStorageHelper.createTarget(location, dirType, fileName, mimeType);
		if (fallbackTarget == null) {
			LOG.warn("Failed to create internal fallback media target: " + fileName);
			return null;
		}
		logDebug("Internal fallback media target created: " + fallbackTarget.getFileName());
		return copyToTarget(fallbackTarget);
	}

	private void discardTarget() {
		try {
			target.finish(false);
			logDebug("Captured media target discarded: " + target.getFileName());
		} catch (IOException e) {
			LOG.warn("Failed to discard captured media target: " + target.getFileName(), e);
		}
	}

	@Override
	protected void onPostExecute(Link link) {
		if (link == null) {
			app.showToastMessage(app.getString(R.string.shared_string_io_error));
		} else {
			callback.processResult(link);
		}
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(message);
		}
	}
}