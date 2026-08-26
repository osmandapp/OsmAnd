package net.osmand.plus.myplaces.favorites;

import static net.osmand.IndexConstants.ZIP_EXT;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class SaveFavoritesTask extends AsyncTask<Void, String, Void> {

	private static final Log log = PlatformUtil.getLog(SaveFavoritesTask.class);

	private final OsmandApplication app;
	private final FavouritesFileHelper helper;
	private final List<FavoriteGroup> groups;
	private final CompletionListener completionListener;
	@Nullable
	private FavoriteDeletionsJournal.JournalState journalState;
	private final boolean saveAllGroups;

	SaveFavoritesTask(@NonNull OsmandApplication app, @NonNull FavouritesFileHelper helper,
	                  @NonNull List<FavoriteGroup> groups, boolean saveAllGroups,
	                  @NonNull CompletionListener completionListener) {
		this.app = app;
		this.helper = helper;
		this.groups = groups;
		this.saveAllGroups = saveAllGroups;
		this.completionListener = completionListener;
	}

	@Override
	protected Void doInBackground(Void... params) {
		boolean success;
		try {
			success = saveAllGroups
					? saveAllGroups(groups)
					: saveSelectedGroupsOnly(groups);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			success = false;
		}
		completionListener.onSaveFinished(success, journalState);
		return null;
	}

	private boolean saveAllGroups(@NonNull List<FavoriteGroup> groups) {
		try {
			FavoriteDeletionsJournal.ReadResult journalRead = FavoriteDeletionsJournal.read(app);
			journalState = journalRead.getState();

			if (journalRead.getReadFailed()) {
				log.error("Favorite deletions journal could not be read. Favorites save will continue, but journal will not be cleared.");
			}

			Set<String> deletedPointKeys = collectStalePointKeys(groups, journalRead.getDeletions().getPointKeys());

			boolean success = saveExternalFiles(groups, deletedPointKeys);
			if (!success) {
				return false;
			}

			File internalFile = helper.getInternalFile();
			Exception internalError = helper.saveFileAtomic(groups, internalFile);
			if (internalError != null) {
				log.error(internalError.getMessage(), internalError);
				return false;
			}

			backup(helper.getBackupFile(), internalFile);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@NonNull
	private Set<String> collectStalePointKeys(@NonNull List<FavoriteGroup> groups, @NonNull Set<String> journalDeletedKeys) {
		Set<String> staleKeys = new HashSet<>(journalDeletedKeys);
		File internalFile = helper.getInternalFile();
		if (!internalFile.exists()) {
			return staleKeys;
		}

		GpxFile gpxFile = SharedUtil.loadGpxFile(internalFile);
		if (gpxFile.getError() != null) {
			return staleKeys;
		}

		Map<String, FavoriteGroup> previousGroups = new LinkedHashMap<>();
		helper.collectFavoriteGroups(gpxFile, previousGroups);

		Set<String> currentKeys = new HashSet<>();
		for (FavoriteGroup group : groups) {
			for (FavouritePoint point : group.getPoints()) {
				currentKeys.add(point.getKey());
			}
		}
		for (FavoriteGroup group : previousGroups.values()) {
			for (FavouritePoint point : group.getPoints()) {
				if (!currentKeys.contains(point.getKey())) {
					staleKeys.add(point.getKey());
				}
			}
		}
		return staleKeys;
	}

	private boolean saveSelectedGroupsOnly(@NonNull List<FavoriteGroup> groupsToSave) {
		try {
			// No need to touch internal file or backup
			// Changes will be picked up during next loadFavorites()
			for (FavoriteGroup group : groupsToSave) {
				if (!saveFavoriteGroup(group)) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	private void loadGPXFiles(@NonNull Map<String, FavoriteGroup> favoriteGroups) {
		File[] files = helper.getFavoritesFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				GpxFile gpxFile = SharedUtil.loadGpxFile(file);
				if (gpxFile.getError() == null) {
					helper.collectFavoriteGroups(gpxFile, favoriteGroups);
				}
			}
		}
	}

	private boolean saveExternalFiles(@NonNull List<FavoriteGroup> localGroups,
	                                  @NonNull Set<String> deleted) {
		Map<String, FavoriteGroup> fileGroups = new LinkedHashMap<>();
		loadGPXFiles(fileGroups);
		return saveLocalGroups(localGroups, fileGroups, deleted) && cleanupOrphanedGroupFiles(localGroups, fileGroups);
	}

	private boolean cleanupOrphanedGroupFiles(@NonNull List<FavoriteGroup> localGroups,
	                                          @NonNull Map<String, FavoriteGroup> fileGroups) {
		for (FavoriteGroup fileGroup : fileGroups.values()) {
			// Search corresponding group in memory
			boolean hasLocalGroup = false;
			for (FavoriteGroup group : localGroups) {
				if (Algorithms.stringsEqual(group.getName(), fileGroup.getName())) {
					hasLocalGroup = true;
					break;
				}
			}
			// Delete external group file if it does not exist in local groups
			if (!hasLocalGroup) {
				File file = helper.getExternalFile(fileGroup);
				if (file.exists() && !file.delete()) {
					log.warn("Failed to delete orphaned favorites file: " + file.getAbsolutePath());
					return false;
				}
			}
		}
		return true;
	}

	private boolean saveLocalGroups(@NonNull List<FavoriteGroup> localGroups,
			@NonNull Map<String, FavoriteGroup> fileGroups, @NonNull Set<String> deleted) {
		for (FavoriteGroup localGroup : localGroups) {
			FavoriteGroup fileGroup = fileGroups.get(localGroup.getName());
			// Collect non deleted points from external group
			Map<String, FavouritePoint> all = new LinkedHashMap<>();
			if (fileGroup != null) {
				for (FavouritePoint point : fileGroup.getPoints()) {
					String key = point.getKey();
					if (!deleted.contains(key)) {
						all.put(key, point);
					}
				}
			}
			// Remove already existing in memory
			List<FavouritePoint> localPoints = new ArrayList<>(localGroup.getPoints());
			for (FavouritePoint point : localPoints) {
				all.remove(point.getKey());
			}
			// save favoritePoints from memory in order to update existing
			localGroup.getPoints().addAll(all.values());
			// Save file if group changed
			if (!localGroup.equals(fileGroup)) {
				if (!saveFavoriteGroup(localGroup)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean saveFavoriteGroup(@NonNull FavoriteGroup group) {
		File externalFile = helper.getExternalFile(group);
		Exception exception = helper.saveFileAtomic(Collections.singletonList(group), externalFile);
		if (exception != null) {
			log.error(exception.getMessage(), exception);
			return false;
		} else if (externalFile.exists()) {
			group.setSize(externalFile.length());
			group.setTimeModified(externalFile.lastModified());
		}
		return true;
	}

	private void backup(@NonNull File backupFile, @NonNull File externalFile) {
		String name = backupFile.getName();
		String nameNoExt = name.substring(0, name.lastIndexOf(ZIP_EXT));
		InputStream fis = null;
		ZipOutputStream zos = null;
		try {
			File file = new File(backupFile.getParentFile(), backupFile.getName());
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			fis = new BufferedInputStream(new FileInputStream(externalFile));
			zos.putNextEntry(new ZipEntry(nameNoExt));
			Algorithms.streamCopy(fis, zos);
			zos.closeEntry();
			zos.flush();
			zos.finish();
		} catch (Exception e) {
			log.warn("Backup failed", e);
		} finally {
			Algorithms.closeStream(zos);
			Algorithms.closeStream(fis);
		}
		helper.clearOldBackups();
	}

	interface CompletionListener {
		void onSaveFinished(boolean success,
		                    @Nullable FavoriteDeletionsJournal.JournalState journalState);
	}
}
