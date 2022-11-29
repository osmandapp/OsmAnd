package net.osmand.plus.myplaces;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.plus.myplaces.FavouritesHelper.getPointsFromGroups;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FavouritesFileHelper {

	private static final Log log = PlatformUtil.getLog(FavouritesFileHelper.class);

	private static final String TIME_PATTERN = "yyyy_MM_dd_hh_mm_ss";
	public static final String GPX_FILE_EXT = ".gpx";
	private static final String ZIP_FILE_EXT = ".zip";

	private static final int BACKUP_MAX_COUNT = 10;
	private static final int BACKUP_MAX_PER_DAY = 2; // The third one is the current backup

	public static final String FILE_PREFIX_TO_SAVE = "favorites";
	public static final String FOLDER_TO_SAVE = "favorites";
	public static final String FILE_GROUP_NAME_SEPARATOR = "-";
	public static final String LEGACY_FILE_PREFIX_TO_SAVE = "favourites";
	public static final String LEGACY_FILE_TO_SAVE = LEGACY_FILE_PREFIX_TO_SAVE + GPX_FILE_EXT;
	public static final String FILE_TO_BACKUP = LEGACY_FILE_PREFIX_TO_SAVE + "_bak" + GPX_FILE_EXT;

	private final OsmandApplication app;

	protected FavouritesFileHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public File getInternalFile() {
		return app.getFileStreamPath(FILE_TO_BACKUP);
	}

	@NonNull
	public File getOldExternalFile() {
		return new File(app.getAppPath(null), LEGACY_FILE_TO_SAVE);
	}

	public File getExternalFile(FavoriteGroup group) {
		File favDir = getExternalDir();
		String fileName = (group.getName().isEmpty() ? LEGACY_FILE_PREFIX_TO_SAVE : FILE_PREFIX_TO_SAVE + FILE_GROUP_NAME_SEPARATOR + group.getName())
				+ GPX_FILE_EXT;
		return new File(favDir, fileName);
	}

	@NonNull
	public File getExternalDir() {
		File favFolder = app.getAppPath(FOLDER_TO_SAVE);
		if (!favFolder.exists()) {
			favFolder.mkdir();
		}
		return favFolder;
	}

	@NonNull
	public Map<String, FavoriteGroup> loadInternalGroups() {
		Map<String, FavoriteGroup> favoriteGroups = new LinkedHashMap<>();
		loadGPXFile(getInternalFile(), favoriteGroups);
		return favoriteGroups;
	}

	@NonNull
	public Map<String, FavoriteGroup> loadExternalGroups() {
		Map<String, FavoriteGroup> favoriteGroups = new LinkedHashMap<>();
		loadGPXFiles(FILE_PREFIX_TO_SAVE, favoriteGroups);
		return favoriteGroups;
	}

	public boolean loadGPXFile(@NonNull File file, @NonNull Map<String, FavoriteGroup> favoriteGroups) {
		if (!file.exists()) {
			return false;
		}
		GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
		if (gpxFile.error != null) {
			return false;
		}
		for (Map.Entry<String, PointsGroup> entry : gpxFile.getPointsGroups().entrySet()) {
			String key = entry.getKey();
			PointsGroup pointsGroup = entry.getValue();
			FavoriteGroup favoriteGroup = FavoriteGroup.fromPointsGroup(pointsGroup);

			favoriteGroups.put(key, favoriteGroup);
		}
		return true;
	}

	public boolean loadGPXFiles(@NonNull String prefix, @NonNull Map<String, FavoriteGroup> favoriteGroups) {
		File file = app.getAppPath(FOLDER_TO_SAVE);
		if (file == null || !file.exists() || !file.isDirectory()) {
			return false;
		}
		File[] files = file.listFiles((dir, name) ->
				name.startsWith(prefix + FILE_GROUP_NAME_SEPARATOR) || name.equals(LEGACY_FILE_TO_SAVE));
		if (Algorithms.isEmpty(files)) {
			return false;
		}
		for (File f : files) {
			GPXFile gpxFile = GPXUtilities.loadGPXFile(f);
			if (gpxFile.error != null) {
				continue;
			}
			Map<String, FavoriteGroup> groups = new LinkedHashMap<>();
			if (loadGPXFile(f, groups)) {
				favoriteGroups.putAll(groups);
			}
		}
		return true;
	}

	public boolean loadPointsFromFile(@NonNull File file, @NonNull Map<String, FavouritePoint> points) {
		Map<String, FavoriteGroup> groups = new LinkedHashMap<>();
		boolean gpxLoaded = loadGPXFile(file, groups);
		if (gpxLoaded) {
			for (FavoriteGroup group : groups.values()) {
				for (FavouritePoint point : group.getPoints()) {
					points.put(point.getKey(), point);
				}
			}
		}
		return gpxLoaded;
	}

	@NonNull
	public GPXFile asGpxFile(@NonNull List<FavoriteGroup> favoriteGroups) {
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		for (FavoriteGroup group : favoriteGroups) {
			gpxFile.addPointsGroup(group.toPointsGroup(app));
		}
		return gpxFile;
	}

	@Nullable
	public Exception saveFile(@NonNull List<FavoriteGroup> favoriteGroups, @NonNull File file) {
		GPXFile gpx = asGpxFile(favoriteGroups);
		return GPXUtilities.writeGpxFile(file, gpx);
	}

	public void saveCurrentPointsIntoFile(@NonNull List<FavoriteGroup> groups) {
		try {
			Map<String, FavoriteGroup> deletedGroups = new LinkedHashMap<>();
			Map<String, FavouritePoint> deletedPoints = new LinkedHashMap<>();
			loadGPXFile(getInternalFile(), deletedGroups);
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
			saveFile(groups, getInternalFile());
			// Save groups to external files
			saveExternalFiles(groups, deletedPoints.keySet());
			// Save groups to backup
			backup(groups, getBackupFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Nullable
	protected Exception saveExternalFiles(@NonNull List<FavoriteGroup> localGroups, @NonNull Set<String> deleted) {
		Exception result = null;
		Map<String, FavoriteGroup> fileGroups = new LinkedHashMap<>();
		loadGPXFiles(FILE_PREFIX_TO_SAVE, fileGroups);
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
				getExternalFile(fileGroup).delete();
			}
		}
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
			for (FavouritePoint point : localGroup.getPoints()) {
				all.remove(point.getKey());
			}
			// save favoritePoints from memory in order to update existing
			localGroup.getPoints().addAll(all.values());
			// Save file if group changed
			if (!localGroup.equals(fileGroup)) {
				Exception exception = saveFile(Collections.singletonList(localGroup), getExternalFile(localGroup));
				if (exception != null) {
					result = exception;
				}
			}
		}
		return result;
	}

	public void backup(@NonNull File backupFile, @NonNull File externalFile) {
		String name = backupFile.getName();
		String nameNoExt = name.substring(0, name.lastIndexOf(ZIP_FILE_EXT));
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
		clearOldBackups(getBackupFiles(), BACKUP_MAX_COUNT);
	}

	public void backup(@NonNull List<FavoriteGroup> favoriteGroups, @NonNull File backupFile) {
		saveFile(favoriteGroups, backupFile);
		clearOldBackups(getBackupFiles(), BACKUP_MAX_COUNT);
	}

	private File getBackupsFolder() {
		File folder = new File(app.getAppPath(null), BACKUP_INDEX_DIR);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	private File getBackupFile() {
		clearOldBackups(getBackupFilesForToday(), BACKUP_MAX_PER_DAY);
		String baseName = formatTime(System.currentTimeMillis());
		return new File(getBackupsFolder(), baseName + GPX_FILE_EXT + ZIP_FILE_EXT);
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

	public List<File> getBackupFiles() {
		List<File> backupFiles = new ArrayList<>();
		File[] files = getBackupsFolder().listFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				if (file.getName().endsWith(GPX_FILE_EXT + ZIP_FILE_EXT)) {
					backupFiles.add(file);
				}
			}
		}
		return backupFiles;
	}

	private void clearOldBackups(List<File> files, int maxCount) {
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

	private static String formatTime(long time) {
		SimpleDateFormat format = getTimeFormatter();
		return format.format(new Date(time));
	}

	private static SimpleDateFormat getTimeFormatter() {
		SimpleDateFormat format = new SimpleDateFormat(TIME_PATTERN, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}
}
