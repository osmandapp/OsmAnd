package net.osmand.plus.myplaces;

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
import java.util.Arrays;
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

	private static final String TIME_PATTERN = "yyyy_MM_dd__HH_mm_ss";
	private static final String GPX_FILE_EXT = ".gpx";
	private static final String ZIP_FILE_EXT = ".zip";

	public static final int BACKUP_MAX_COUNT = 10;
	public static final int BACKUP_MAX_PER_DAY = 2; // The third one is the current backup
	public static final String BACKUP_FOLDER = "backup";
	public static final String FILE_TO_SAVE = "favourites.gpx";
	public static final String FILE_TO_BACKUP = "favourites_bak.gpx";

	private final OsmandApplication app;

	protected FavouritesFileHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public File getInternalFile() {
		return app.getFileStreamPath(FILE_TO_BACKUP);
	}

	@NonNull
	public File getExternalFile() {
		return new File(app.getAppPath(null), FILE_TO_SAVE);
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
		loadGPXFile(getExternalFile(), favoriteGroups);
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
			Map<String, FavouritePoint> deletedInMemory = new LinkedHashMap<>();
			loadPointsFromFile(getInternalFile(), deletedInMemory);
			for (FavouritePoint point : getPointsFromGroups(groups)) {
				deletedInMemory.remove(point.getKey());
			}
			saveFile(groups, getInternalFile());
			saveExternalFile(groups, deletedInMemory.keySet());
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	protected Exception saveExternalFile(@NonNull List<FavoriteGroup> groups, @NonNull Set<String> deleted) {
		Map<String, FavouritePoint> all = new LinkedHashMap<>();
		loadPointsFromFile(getExternalFile(), all);
		for (String key : deleted) {
			all.remove(key);
		}
		// remove already existing in memory
		for (FavouritePoint point : getPointsFromGroups(groups)) {
			all.remove(point.getKey());
		}
		// save favoritePoints from memory in order to update existing
		for (FavouritePoint point : all.values()) {
			for (FavoriteGroup group : groups) {
				if (Algorithms.stringsEqual(point.getCategory(), group.getName())) {
					group.getPoints().add(point);
					break;
				}
			}
		}
		return saveFile(groups, getExternalFile());
	}

	public void backup(@NonNull File backupFile, @NonNull File externalFile) {
		String name = backupFile.getName();
		String nameNoExt = name.substring(0, name.lastIndexOf(ZIP_FILE_EXT));
		ZipEntry entry = new ZipEntry(nameNoExt);
		try {
			File f = new File(backupFile.getParentFile(), backupFile.getName());
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			FileInputStream fis = new FileInputStream(externalFile);
			out.putNextEntry(entry);
			Algorithms.streamCopy(fis, out);
			out.closeEntry();
			fis.close();
			out.close();
		} catch (Exception e) {
			log.warn("Backup failed", e);
		}
		clearOldBackups(getBackupFiles(), BACKUP_MAX_COUNT);
	}

	private File getBackupsFolder() {
		File folder = new File(app.getAppPath(null), BACKUP_FOLDER);
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

	private List<File> getBackupFiles() {
		File folder = getBackupsFolder();
		File[] files = folder.listFiles();
		return files != null ? Arrays.asList(files) : Collections.emptyList();
	}

	private void clearOldBackups(List<File> files, int maxCount) {
		if (files.size() >= maxCount) {
			// sort in order from oldest to newest
			Collections.sort(files, (f1, f2) -> {
				return Long.compare(f2.lastModified(), f1.lastModified());
			});
			for (int i = files.size(); i > maxCount ; --i) {
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