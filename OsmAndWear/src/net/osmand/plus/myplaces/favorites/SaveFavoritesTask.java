package net.osmand.plus.myplaces.favorites;

import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.myplaces.favorites.FavouritesHelper.getPointsFromGroups;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SaveFavoritesTask extends AsyncTask<Void, String, Void> {

	private static final Log log = PlatformUtil.getLog(SaveFavoritesTask.class);

	private final FavouritesFileHelper fileHelper;
	private final List<FavoriteGroup> favoriteGroups;
	private final SaveFavoritesListener listener;

	public SaveFavoritesTask(@NonNull FavouritesFileHelper fileHelper,
	                         @NonNull List<FavoriteGroup> favoriteGroups,
	                         @Nullable SaveFavoritesListener listener) {
		this.fileHelper = fileHelper;
		this.favoriteGroups = favoriteGroups;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... params) {
		saveCurrentPointsIntoFile(favoriteGroups);
		return null;
	}

	private void saveCurrentPointsIntoFile(@NonNull List<FavoriteGroup> groups) {
		try {
			Map<String, FavoriteGroup> deletedGroups = new LinkedHashMap<>();
			Map<String, FavouritePoint> deletedPoints = new LinkedHashMap<>();

			File internalFile = fileHelper.getInternalFile();
			GpxFile gpxFile = SharedUtil.loadGpxFile(internalFile);
			if (gpxFile.getError() == null) {
				fileHelper.collectFavoriteGroups(gpxFile, deletedGroups);
			}
			// Get all points from internal file to filter later
			for (FavoriteGroup group : deletedGroups.values()) {
				for (FavouritePoint point : group.getPoints()) {
					deletedPoints.put(point.getKey(), point);
				}
			}
			// Hold only deleted points in map
			for (FavouritePoint point : getPointsFromGroups(groups)) {
				deletedPoints.remove(point.getKey());
			}
			// Hold only deleted groups in map
			for (FavoriteGroup group : groups) {
				deletedGroups.remove(group.getName());
			}
			// Save groups to internal file
			fileHelper.saveFile(groups, internalFile);
			// Save groups to external files
			saveExternalFiles(groups, deletedPoints.keySet());
			// Save groups to backup file
			// backup(groups, getBackupFile()); // creates new, but does not zip
			backup(fileHelper.getBackupFile(), internalFile); // simply backs up internal file, hence internal name is reflected in gpx <name> metadata
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void loadGPXFiles(@NonNull Map<String, FavoriteGroup> favoriteGroups) {
		File[] files = fileHelper.getFavoritesFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				GpxFile gpxFile = SharedUtil.loadGpxFile(file);
				if (gpxFile.getError() == null) {
					fileHelper.collectFavoriteGroups(gpxFile, favoriteGroups);
				}
			}
		}
	}

	private void saveExternalFiles(@NonNull List<FavoriteGroup> localGroups, @NonNull Set<String> deleted) {
		Map<String, FavoriteGroup> fileGroups = new LinkedHashMap<>();
		loadGPXFiles(fileGroups);
		saveFileGroups(localGroups, fileGroups);
		saveLocalGroups(localGroups, fileGroups, deleted);
	}

	private void saveFileGroups(@NonNull List<FavoriteGroup> localGroups, @NonNull Map<String, FavoriteGroup> fileGroups) {
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
				fileHelper.getExternalFile(fileGroup).delete();
			}
		}
	}

	private void saveLocalGroups(@NonNull List<FavoriteGroup> localGroups, @NonNull Map<String, FavoriteGroup> fileGroups, @NonNull Set<String> deleted) {
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
				File externalFile = fileHelper.getExternalFile(localGroup);
				Exception exception = fileHelper.saveFile(Collections.singletonList(localGroup), externalFile);
				if (exception != null) {
					log.error(exception);
				}
			}
		}
	}

	private void backup(@NonNull File backupFile, @NonNull File externalFile) {
		String name = backupFile.getName();
		String nameNoExt = name.substring(0, name.lastIndexOf(ZIP_EXT));
		FileInputStream fis = null;
		ZipOutputStream zos = null;
		try {
			File file = new File(backupFile.getParentFile(), backupFile.getName());
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			fis = new FileInputStream(externalFile);
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
		fileHelper.clearOldBackups();
	}

	@Override
	protected void onPostExecute(Void result) {
		if (listener != null) {
			listener.onSavingFavoritesFinished();
		}
	}

	public interface SaveFavoritesListener {

		void onSavingFavoritesFinished();
	}
}
