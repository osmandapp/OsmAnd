package net.osmand.plus.myplaces.favorites;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.IndexConstants.FAVORITES_INDEX_DIR;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.shared.gpx.GpxFile.XML_COLON;

import android.util.AtomicFile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavouritesFileHelper {

	private static final Log log = PlatformUtil.getLog(FavouritesFileHelper.class);

	private static final String TIME_PATTERN = "yyyy-MM-dd_HHmmss";

	private static final int BACKUP_MAX_COUNT = 10;
	private static final int BACKUP_MAX_PER_DAY = 2; // The third one is the current backup

	public static final String FAV_FILE_PREFIX = "favorites";
	public static final String FAV_GROUP_NAME_SEPARATOR = "-";
	public static final String LEGACY_FAV_FILE_PREFIX = "favourites";
	public static final String BAK_FILE_SUFFIX = "_bak";

	public static final String SUBFOLDER_PLACEHOLDER = "_%_";

	private final OsmandApplication app;
	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
	private final Object saveLock = new Object();

	private boolean saveRunning;
	@Nullable
	private SaveBatch pendingSave;

	protected FavouritesFileHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	protected File getInternalFile() {
		return app.getFileStreamPath(LEGACY_FAV_FILE_PREFIX + BAK_FILE_SUFFIX + GPX_FILE_EXT);
	}

	@NonNull
	public File getLegacyExternalFile() {
		return new File(app.getAppPath(null), LEGACY_FAV_FILE_PREFIX + GPX_FILE_EXT);
	}

	@NonNull
	public File getExternalFile(@NonNull FavoriteGroup group) {
		return new File(getExternalDir(), getFolderFileName(group.getName()) + GPX_FILE_EXT);
	}

	@NonNull
	public static String getFolderFileName(@NonNull String folderPath) {
		return folderPath.isEmpty() ? FAV_FILE_PREFIX
				: FAV_FILE_PREFIX + FAV_GROUP_NAME_SEPARATOR + getGroupFileName(folderPath);
	}

	@NonNull
	public File getExternalDir() {
		File favFolder = app.getAppPath(FAVORITES_INDEX_DIR);
		if (!favFolder.exists()) {
			favFolder.mkdir();
		}
		return favFolder;
	}

	@NonNull
	public Map<String, FavoriteGroup> loadInternalGroups() {
		Map<String, FavoriteGroup> groups = new LinkedHashMap<>();
		File file = getInternalFile();
		recoverAtomicFile(file);
		if (file.exists()) {
			loadFileGroups(file, groups, false);
		}
		return groups;
	}

	@NonNull
	public Map<String, FavoriteGroup> loadExternalGroups() {
		Map<String, FavoriteGroup> groups = new LinkedHashMap<>();
		File[] files = getFavoritesFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				if (file.exists()) {
					loadFileGroups(file, groups, false);
				}
			}
		}
		return groups;
	}

	private void loadFileGroups(@NonNull File file, @NonNull Map<String, FavoriteGroup> groups, boolean async) {
		CallbackWithObject<GpxFile> callback = gpxFile -> {
			if (gpxFile.getError() == null) {
				collectFavoriteGroups(gpxFile, groups);
			}
			return true;
		};
		if (async) {
			loadGpxFile(file, callback);
		} else {
			loadGpxFileSync(file, callback);
		}
	}

	public void loadGpxFile(@NonNull File file, @NonNull CallbackWithObject<GpxFile> callback) {
		GpxFileLoaderTask loaderTask = new GpxFileLoaderTask(file, null, callback);
		OsmAndTaskManager.executeTask(loaderTask, singleThreadExecutor);
	}

	public void loadGpxFileSync(@NonNull File file, @NonNull CallbackWithObject<GpxFile> callback) {
		GpxFileLoaderTask loaderTask = new GpxFileLoaderTask(file, null, null);
		try {
			GpxFile gpxFile = OsmAndTaskManager.executeTask(loaderTask, singleThreadExecutor).get();
			callback.processResult(gpxFile);
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
	}

	public void saveFavoritesIntoFile(@NonNull List<FavoriteGroup> groups, boolean saveAllGroups, @Nullable FavoritesListener listener) {
		enqueueSave(groups, saveAllGroups, listener, null);
	}

	public void saveFavoritesIntoFileSync(@NonNull List<FavoriteGroup> groups, boolean saveAllGroups, @Nullable FavoritesListener listener) {
		CountDownLatch waiter = new CountDownLatch(1);
		enqueueSave(groups, saveAllGroups, listener, waiter);
		try {
			waiter.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error(e.getMessage(), e);
		}
	}

	private void enqueueSave(@NonNull List<FavoriteGroup> groups, boolean saveAllGroups,
			@Nullable FavoritesListener listener, @Nullable CountDownLatch waiter) {
		SaveBatch batchToStart = null;
		synchronized (saveLock) {
			if (!saveRunning) {
				saveRunning = true;
				batchToStart = new SaveBatch(groups, saveAllGroups, listener, waiter);
			} else if (pendingSave == null) {
				pendingSave = new SaveBatch(groups, saveAllGroups, listener, waiter);
			} else {
				pendingSave.merge(groups, saveAllGroups, listener, waiter);
			}
		}
		if (batchToStart != null) {
			startSave(batchToStart);
		}
	}

	private void startSave(@NonNull SaveBatch batch) {
		try {
			SaveFavoritesTask task = new SaveFavoritesTask(app, this, batch.getGroups(), batch.getSaveAllGroups(),
					(success, journalState) -> onSaveFinished(batch, success, journalState));
			OsmAndTaskManager.executeTask(task, singleThreadExecutor);
		} catch (RuntimeException e) {
			log.error("Failed to start favorites save", e);
			onSaveFinished(batch, false, null);
		}
	}

	private void onSaveFinished(@NonNull SaveBatch completedBatch, boolean success,
	                            @Nullable FavoriteDeletionsJournal.JournalState journalState) {
		SaveBatch batchToStart;
		synchronized (saveLock) {
			batchToStart = pendingSave;
			pendingSave = null;
			if (batchToStart == null) {
				saveRunning = false;
			}
		}
		if (success && completedBatch.getSaveAllGroups() && journalState != null) {
			FavoriteDeletionsJournal.clearIfUnchanged(app, journalState);
		}
		for (CountDownLatch waiter : completedBatch.getWaiters()) {
			waiter.countDown();
		}
		for (FavoritesListener listener : completedBatch.getListeners()) {
			app.runInUIThread(() -> listener.onSavingFavoritesFinished(success));
		}
		if (batchToStart != null) {
			startSave(batchToStart);
		}
	}

	public void collectFavoriteGroups(@NonNull GpxFile gpxFile, @NonNull Map<String, FavoriteGroup> favoriteGroups) {
		Map<String, PointsGroup> pointsGroups = gpxFile.getPointsGroups();
		boolean singleGroupFile = pointsGroups.size() == 1;
		File file = !Algorithms.isEmpty(gpxFile.getPath()) ? new File(gpxFile.getPath()) : null;
		boolean useFileMetadata = singleGroupFile && file != null && file.exists();
		for (Map.Entry<String, PointsGroup> entry : pointsGroups.entrySet()) {
			String key = entry.getKey();
			PointsGroup pointsGroup = entry.getValue();
			FavoriteGroup favoriteGroup = FavoriteGroup.fromPointsGroup(pointsGroup);
			if (useFileMetadata) {
				favoriteGroup.setSize(file.length());
				favoriteGroup.setTimeModified(gpxFile.getModifiedTime());
			} else {
				FavoriteGroup existingGroup = favoriteGroups.get(key);
				if (existingGroup != null) {
					favoriteGroup.copyFileMetadata(existingGroup);
				}
			}
			favoriteGroups.put(key, favoriteGroup);
		}
	}

	@Nullable
	public File[] getFavoritesFiles() {
		File dir = app.getAppPath(FAVORITES_INDEX_DIR);
		if (!dir.exists() || !dir.isDirectory()) {
			return null;
		}
		File[] remnants = dir.listFiles((d, name) -> name.endsWith(".new") || name.endsWith(".bak"));
		if (!Algorithms.isEmpty(remnants)) {
			for (File remnant : remnants) {
				String baseName = remnant.getName().substring(0, remnant.getName().length() - 4);
				if (isFavoritesFileName(baseName)) {
					recoverAtomicFile(new File(dir, baseName));
				}
			}
		}
		return dir.listFiles((d, name) -> isFavoritesFileName(name));
	}

	private static boolean isFavoritesFileName(@NonNull String name) {
		return (name.startsWith(FAV_FILE_PREFIX + FAV_GROUP_NAME_SEPARATOR)
				&& name.endsWith(GPX_FILE_EXT))
				|| name.equals(FAV_FILE_PREFIX + GPX_FILE_EXT)
				|| name.equals(LEGACY_FAV_FILE_PREFIX + GPX_FILE_EXT);
	}

	private static void recoverAtomicFile(@NonNull File file) {
		try (FileInputStream ignored = new AtomicFile(file).openRead()) {
			// Opening recovers a previous valid base file and removes stale temporary data.
		} catch (IOException ignored) {
			// No committed base file exists yet.
		}
	}

	@NonNull
	public GpxFile asGpxFile(@NonNull List<FavoriteGroup> favoriteGroups) {
		GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
		for (FavoriteGroup group : favoriteGroups) {
			gpxFile.addPointsGroup(group.toPointsGroup(app));
		}
		return gpxFile;
	}

	@Nullable
	public Exception saveFile(@NonNull List<FavoriteGroup> favoriteGroups, @NonNull File file) {
		GpxFile gpx = asGpxFile(favoriteGroups);
		return SharedUtil.writeGpxFile(file, gpx);
	}

	@Nullable
	protected Exception saveFileAtomic(@NonNull List<FavoriteGroup> favoriteGroups, @NonNull File file) {
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		GpxFile gpx = asGpxFile(favoriteGroups);
		AtomicFile atomicFile = new AtomicFile(file);
		FileOutputStream output = null;
		try {
			output = atomicFile.startWrite();
			Exception error = SharedUtil.writeGpx(output, gpx, null);
			if (error != null) {
				throw error;
			}
			atomicFile.finishWrite(output);
			return null;
		} catch (Exception e) {
			if (output != null) {
				atomicFile.failWrite(output);
			}
			return e;
		}
	}

	@NonNull
	private File getBackupsFolder() {
		File folder = new File(app.getAppPath(null), BACKUP_INDEX_DIR);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	@NonNull
	protected File getBackupFile() {
		clearOldBackups(getBackupFilesForToday(), BACKUP_MAX_PER_DAY);
		String baseName = FAV_FILE_PREFIX + BAK_FILE_SUFFIX + "_" + formatTime(System.currentTimeMillis());
		return new File(getBackupsFolder(), baseName + GPX_FILE_EXT + ZIP_EXT);
	}

	@NonNull
	private List<File> getBackupFilesForToday() {
		List<File> result = new ArrayList<>();
		List<File> files = getBackupFiles();
		long now = System.currentTimeMillis();
		for (File file : files) {
			if (OsmAndFormatter.isSameDay(now, file.lastModified())) {
				result.add(file);
			}
		}
		return result;
	}

	@NonNull
	public List<File> getBackupFiles() {
		List<File> backupFiles = new ArrayList<>();
		File[] files = getBackupsFolder().listFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				if (file.getName().endsWith(GPX_FILE_EXT + ZIP_EXT)) {
					backupFiles.add(file);
				}
			}
		}
		return backupFiles;
	}

	protected void clearOldBackups() {
		clearOldBackups(getBackupFiles(), BACKUP_MAX_COUNT);
	}

	private void clearOldBackups(@NonNull List<File> files, int maxCount) {
		if (files.size() >= maxCount) {
			// sort in order from oldest to newest
			Collections.sort(files, (f1, f2) -> {
				return Long.compare(f2.lastModified(), f1.lastModified());
			});
			for (int i = files.size(); i > maxCount; --i) {
				File oldest = files.get(i - 1);
				oldest.delete();
			}
		}
	}

	@NonNull
	private static String formatTime(long time) {
		SimpleDateFormat format = getTimeFormatter();
		return format.format(new Date(time));
	}

	@NonNull
	private static SimpleDateFormat getTimeFormatter() {
		SimpleDateFormat format = new SimpleDateFormat(TIME_PATTERN, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}

	@NonNull
	public static String getGroupFileName(@NonNull String groupName) {
		if (groupName.contains("/")) {
			return groupName.replaceAll("/", SUBFOLDER_PLACEHOLDER);
		}
		if (groupName.contains(":")) {
			return groupName.replaceAll(":", XML_COLON);
		}
		return groupName;
	}

	@NonNull
	public static String getGroupName(@NonNull String fileName) {
		if (fileName.contains(SUBFOLDER_PLACEHOLDER)) {
			return fileName.replaceAll(SUBFOLDER_PLACEHOLDER, "/");
		}
		if (fileName.contains(XML_COLON)) {
			return fileName.replaceAll(XML_COLON, ":");
		}
		return fileName;
	}
}
