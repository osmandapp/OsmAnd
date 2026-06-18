package net.osmand.plus.settings.mediastorage.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.gallery.data.GalleryKey;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.mediastorage.MediaSource;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageUtils;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;
import net.osmand.plus.settings.mediastorage.MediaTarget;
import net.osmand.plus.settings.datastorage.MoveFilesStopListener;
import net.osmand.plus.settings.datastorage.StorageMigrationFragment;
import net.osmand.plus.settings.datastorage.StorageMigrationListener;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.StorageMigrationAsyncTask.FileCopyListener;
import net.osmand.plus.settings.enums.MediaStorageType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.media.MediaFileNameFormat;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Moves OsmAnd-owned attached media from a captured source location to a captured target location.
 */
public class MoveMediaFilesTask extends AsyncTask<Void, Object, Map<String, Pair<String, Long>>> {

	private static final Log log = PlatformUtil.getLog(MoveMediaFilesTask.class);

	private final OsmandApplication app;
	private final MediaStorageHelper mediaStorageHelper;
	private final WeakReference<OsmandActionBarActivity> activity;
	private final MediaStorageLocation to;
	private final StorageItem fromItem;
	private final StorageItem toItem;

	private ProgressHelper progressHelper;
	private StorageMigrationListener migrationListener;
	private final MoveFilesStopListener stopTaskListener;
	private int copyProgress;
	private final Pair<Long, Long> filesSize;
	private final List<MediaSource> sources;
	private final Map<String, MovedMedia> movedMedia = new HashMap<>();
	private final List<MediaSource> movedSources = new ArrayList<>();
	@Nullable
	private final CallbackWithObject<Boolean> moveListener;

	public MoveMediaFilesTask(@NonNull OsmandApplication app, @Nullable OsmandActionBarActivity activity,
			@NonNull MediaStorageLocation from, @NonNull MediaStorageLocation to,
			@NonNull List<MediaSource> sources, @NonNull Pair<Long, Long> filesSize,
			@Nullable MoveFilesStopListener stopTaskListener, @Nullable CallbackWithObject<Boolean> moveListener) {
		this.app = app;
		this.mediaStorageHelper = new MediaStorageHelper(app);
		this.activity = new WeakReference<>(activity);
		this.to = to;
		this.fromItem = toStorageItem(from);
		this.toItem = toStorageItem(to);
		this.sources = sources;
		this.filesSize = filesSize;
		this.stopTaskListener = stopTaskListener;
		this.moveListener = moveListener;
		logDebug("Move media task created: from=" + from.getStorageType() + ", to=" + to.getStorageType() + ", sources=" + sources.size() + ", size=" + filesSize.first);
	}

	@NonNull
	private StorageItem toStorageItem(@NonNull MediaStorageLocation location) {
		MediaStorageType storageType = location.getStorageType();
		Uri manualTreeUri = location.getManualUri();
		String manualUri = manualTreeUri == null ? null : manualTreeUri.toString();
		String directory = mediaStorageHelper.getStorageDisplayDirectory(storageType, manualUri);
		return StorageItem.builder()
				.setKey(storageType.name().toLowerCase(Locale.US))
				.setTitle(storageType.toHumanString(app))
				.setDescription(app.getString(storageType.getDescriptionId()))
				.setDirectory(directory)
				.setIconResId(R.drawable.ic_action_folder)
				.createItem();
	}

	@Override
	protected void onPreExecute() {
		progressHelper = new ProgressHelper(() -> {
			copyProgress = progressHelper.getLastKnownProgress();
			publishProgress(copyProgress);
		});
		progressHelper.setTimeInterval(100);

		FragmentActivity fActivity = activity.get();
		if (AndroidUtils.isActivityNotDestroyed(fActivity)) {
			FragmentManager manager = fActivity.getSupportFragmentManager();
			migrationListener = StorageMigrationFragment.showInstance(manager, toItem, fromItem, filesSize,
					copyProgress, sources.size(), false, false, null, stopTaskListener);
		}
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (migrationListener != null) {
			for (Object object : values) {
				if (object instanceof String) {
					migrationListener.onFileCopyStarted((String) object);
				} else if (object instanceof Integer) {
					copyProgress = (Integer) object;
					migrationListener.onFilesCopyProgress(copyProgress);
				} else if (object instanceof Pair) {
					migrationListener.onRemainingFilesUpdate((Pair<Integer, Long>) object);
				}
			}
		}
	}

	@Override
	protected Map<String, Pair<String, Long>> doInBackground(Void... params) {
		Map<String, Pair<String, Long>> errors = new HashMap<>();
		FileCopyListener fileCopyListener = getCopyFilesListener();

		long remainingSize = filesSize.first;
		long totalSize = filesSize.second / 1024;
		progressHelper.onStartWork((int) totalSize);
		for (int i = 0; i < sources.size(); i++) {
			if (isCancelled()) {
				break;
			}
			MediaSource source = sources.get(i);
			long fileLength = source.getLength();
			remainingSize -= fileLength;
			moveMediaFile(source, errors, fileCopyListener);
			publishProgress(new Pair<>(sources.size() - i, remainingSize));
		}
		progressHelper.onFinishTask();
		return errors;
	}

	private void moveMediaFile(@NonNull MediaSource source, @NonNull Map<String, Pair<String, Long>> errors, @NonNull FileCopyListener listener) {
		long fileLength = source.getLength();
		String sourceFileName = source.getFileName();
		listener.onFileCopyStarted(sourceFileName);

		MediaTarget target = createTarget(source);
		if (target == null) {
			addError(errors, sourceFileName, R.string.media_storage_directory_not_writable, fileLength);
			finishProgress(sourceFileName, fileLength, listener);
			return;
		}
		if (target.exists()) {
			addError(errors, sourceFileName, R.string.file_already_exists, fileLength);
			finishProgress(sourceFileName, fileLength, listener);
			return;
		}

		String error = copyFile(source, target, listener);
		if (error != null) {
			errors.put(sourceFileName, new Pair<>(error, fileLength));
		} else {
			String targetHref = getTargetHref(target);
			if (!Algorithms.isEmpty(targetHref)) {
				String mimeType = MediaStorageUtils.getMimeType(source.getMimeType(), source.getFileName(), source.getDirType());
				MovedMedia media = new MovedMedia(targetHref, mimeType);
				for (String href : source.getHrefKeys()) {
					movedMedia.put(href, media);
				}
				movedSources.add(source);
			} else {
				addError(errors, sourceFileName, R.string.shared_string_io_error, fileLength);
				deleteTarget(target);
			}
		}
		listener.onFileCopyFinished(sourceFileName, FileUtils.APPROXIMATE_FILE_SIZE_BYTES / 1024);
	}

	@Nullable
	private MediaTarget createTarget(@NonNull MediaSource source) {
		String sourceFileName = source.getFileName();
		String fileName = MediaFileNameFormat.createUniqueGeneratedMediaFileName(sourceFileName,
				name -> mediaStorageHelper.mediaFileExists(to, source.getDirType(), name));
		return mediaStorageHelper.createTarget(to, source.getDirType(), fileName, source.getMimeType());
	}

	private void addError(@NonNull Map<String, Pair<String, Long>> errors, @NonNull String fileName, int errorMessageId, long fileLength) {
		errors.put(fileName, new Pair<>(app.getString(errorMessageId), fileLength));
	}

	@Nullable
	private String getTargetHref(@NonNull MediaTarget target) {
		try {
			return target.getHref();
		} catch (RuntimeException e) {
			log.warn("Failed to resolve media target href: " + target.getFileName(), e);
			return null;
		}
	}

	private void finishProgress(@NonNull String fileName, long fileLength, @NonNull FileCopyListener listener) {
		int deltaProgress = (int) ((fileLength + FileUtils.APPROXIMATE_FILE_SIZE_BYTES) / 1024);
		listener.onFileCopyFinished(fileName, deltaProgress);
	}

	@Nullable
	private String copyFile(@NonNull MediaSource source, @NonNull MediaTarget target, @NonNull FileCopyListener listener) {
		String fileName = source.getFileName();
		try {
			return MediaStorageUtils.copyToTarget(source.openInputStream(), target, getCopyProgress(fileName, listener));
		} catch (Exception e) {
			return MediaStorageUtils.getErrorMessage(e);
		}
	}

	@NonNull
	private IProgress getCopyProgress(@NonNull String fileName, @NonNull FileCopyListener listener) {
		return new AbstractProgress() {
			@Override
			public void progress(int deltaWork) {
				listener.onFileCopyProgress(fileName, deltaWork);
			}
		};
	}

	@Override
	protected void onPostExecute(Map<String, Pair<String, Long>> errors) {
		onTaskFinished(errors);
	}

	@Override
	protected void onCancelled() {
		onTaskFinished(null);
	}

	private void onTaskFinished(@Nullable Map<String, Pair<String, Long>> errors) {
		Set<GalleryKey> changedGalleryKeys = new HashSet<>();
		boolean favoritesChanged = updateFavoriteMediaLinks(movedMedia, changedGalleryKeys);
		app.getGalleryHelper().notifyAttachedMediaChanged(changedGalleryKeys);
		// Delete the originals only after the rewritten links are persisted, so a crash between
		// copy and save can never leave an on-disk link pointing at an already-deleted source.
		boolean copySuccess = !isCancelled() && Algorithms.isEmpty(errors);
		persistChangesAndDeleteSources(favoritesChanged, persistSuccess -> {
			finishMoveTask(errors, copySuccess && persistSuccess);
			return true;
		});
	}

	private void finishMoveTask(@Nullable Map<String, Pair<String, Long>> errors, boolean success) {
		logDebug("Move media task finished: success=" + success + ", cancelled=" + isCancelled()
				+ ", movedLinks=" + movedMedia.size() + ", errors=" + (errors == null ? null : errors.size()));
		if (!isCancelled() && (!success || !Algorithms.isEmpty(errors))) {
			app.showToastMessage(R.string.shared_string_io_error);
		}
		if (migrationListener != null && !isCancelled()) {
			migrationListener.onFilesCopyFinished(errors == null ? new HashMap<>() : errors, new ArrayList<>());
		}
		if (moveListener != null) {
			moveListener.processResult(success);
		}
	}

	private void deleteMovedSources(@NonNull CallbackWithObject<Boolean> callback) {
		List<MediaSource> sourcesToDelete = new ArrayList<>(movedSources);
		movedSources.clear();
		if (sourcesToDelete.isEmpty()) {
			callback.processResult(true);
			return;
		}
		logDebug("Move media deleting old sources: count=" + sourcesToDelete.size());
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return deleteMovedSources(sourcesToDelete);
			}

			@Override
			protected void onPostExecute(Boolean success) {
				callback.processResult(success);
			}
		});
	}

	private boolean deleteMovedSources(@NonNull List<MediaSource> sourcesToDelete) {
		boolean failed = false;
		for (MediaSource source : sourcesToDelete) {
			try {
				source.delete();
			} catch (IOException e) {
				failed = true;
				log.warn("Failed to delete moved media source: " + source.getId(), e);
			}
		}
		logDebug("Move media delete old sources finished: success=" + !failed);
		return !failed;
	}

	private void deleteTarget(@NonNull MediaTarget target) {
		try {
			target.delete();
		} catch (IOException e) {
			log.warn("Failed to delete media target: " + target.getFileName(), e);
		}
	}

	@NonNull
	private FileCopyListener getCopyFilesListener() {
		return new FileCopyListener() {

			@Override
			public void onFileCopyStarted(@NonNull String fileName) {
				publishProgress(fileName);
			}

			@Override
			public void onFileCopyProgress(@NonNull String fileName, int deltaWork) {
				progressHelper.onProgress(deltaWork);
			}

			@Override
			public void onFileCopyFinished(@NonNull String fileName, int deltaWork) {
				progressHelper.onProgress(deltaWork);
			}
		};
	}

	private boolean updateFavoriteMediaLinks(@NonNull Map<String, MovedMedia> media,
			@NonNull Set<GalleryKey> changedGalleryKeys) {
		if (media.isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (FavouritePoint point : app.getFavoritesHelper().getFavouritePoints()) {
			boolean pointChanged = updateLinks(point.getLinks(), media);
			if (pointChanged) {
				changedGalleryKeys.add(new GalleryKey.Favorite(point.getKey()));
				changed = true;
			}
		}
		return changed;
	}

	private void persistChangesAndDeleteSources(boolean favoritesChanged, @NonNull CallbackWithObject<Boolean> callback) {
		if (!favoritesChanged) {
			// No link changes to persist; the moved sources can be removed immediately.
			deleteMovedSources(callback);
			return;
		}
		app.getFavoritesHelper().saveCurrentPointsIntoFile(true, new FavoritesListener() {
			@Override
			public void onSavingFavoritesFinished() {
				// Only delete the originals once favorites are written with the new links.
				deleteMovedSources(callback);
			}
		});
	}

	private boolean updateLinks(@Nullable List<Link> links, @NonNull Map<String, MovedMedia> media) {
		boolean changed = false;
		if (!Algorithms.isEmpty(links)) {
			for (Link link : links) {
				MovedMedia moved = media.get(link.getHref());
				if (moved != null) {
					link.setHref(moved.href());
					if (Algorithms.isEmpty(link.getType())) {
						link.setType(moved.mimeType());
					}
					changed = true;
				}
			}
		}
		return changed;
	}

	private record MovedMedia(@NonNull String href, @NonNull String mimeType) {
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			log.debug(message);
		}
	}
}
