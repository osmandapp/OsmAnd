package net.osmand.plus.myplaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.logging.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FavouritesFileHelper {

	private static final Log log = PlatformUtil.getLog(FavouritesFileHelper.class);

	public static final int BACKUP_MAX_COUNT = 20;
	public static final String BACKUP_FOLDER = "backup";
	public static final String BACKUP_FILE_PREFIX = "favourites_bak_";
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
	public Map<String, FavouritePoint> loadInternalPoints() {
		Map<String, FavouritePoint> points = new LinkedHashMap<>();
		loadGPXFile(getInternalFile(), points);
		return points;
	}

	@NonNull
	public Map<String, FavouritePoint> loadExternalPoints() {
		Map<String, FavouritePoint> points = new LinkedHashMap<>();
		loadGPXFile(getExternalFile(), points);
		return points;
	}

	public boolean loadGPXFile(@NonNull File file, @NonNull Map<String, FavouritePoint> points) {
		if (!file.exists()) {
			return false;
		}
		GPXFile res = GPXUtilities.loadGPXFile(file);
		if (res.error != null) {
			return false;
		}
		for (WptPt wptPt : res.getPoints()) {
			FavouritePoint favouritePoint = FavouritePoint.fromWpt(wptPt, app);
			points.put(favouritePoint.getKey(), favouritePoint);
		}
		return true;
	}

	@NonNull
	public GPXFile asGpxFile(@NonNull List<FavouritePoint> points) {
		GPXFile gpx = new GPXFile(Version.getFullVersion(app));
		for (FavouritePoint point : points) {
			app.getSelectedGpxHelper().addPoint(point.toWpt(app), gpx);
		}
		return gpx;
	}

	@Nullable
	public Exception saveFile(@NonNull List<FavouritePoint> points, @NonNull File file) {
		GPXFile gpx = asGpxFile(points);
		return GPXUtilities.writeGpxFile(file, gpx);
	}

	public void saveCurrentPointsIntoFile(@NonNull List<FavouritePoint> points) {
		try {
			Map<String, FavouritePoint> deletedInMemory = new LinkedHashMap<>();
			loadGPXFile(getInternalFile(), deletedInMemory);
			for (FavouritePoint point : points) {
				deletedInMemory.remove(point.getKey());
			}
			saveFile(points, getInternalFile());
			saveExternalFile(points, deletedInMemory.keySet());
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	protected Exception saveExternalFile(@NonNull List<FavouritePoint> points, @Nullable Set<String> deleted) {
		Map<String, FavouritePoint> all = new LinkedHashMap<>();
		loadGPXFile(getExternalFile(), all);
		if (!Algorithms.isEmpty(deleted)) {
			for (String key : deleted) {
				all.remove(key);
			}
		}
		// remove already existing in memory
		for (FavouritePoint point : points) {
			all.remove(point.getKey());
		}
		// save favoritePoints from memory in order to update existing
		points.addAll(all.values());
		return saveFile(points, getExternalFile());
	}

	public void backup(@NonNull File backupFile, @NonNull File externalFile) {
		try {
			File f = new File(backupFile.getParentFile(), backupFile.getName());
			BZip2CompressorOutputStream out = new BZip2CompressorOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			FileInputStream fis = new FileInputStream(externalFile);
			Algorithms.streamCopy(fis, out);
			fis.close();
			out.close();
		} catch (Exception e) {
			log.warn("Backup failed", e);
		}
	}

	private File getBackupFile() {
		File fld = new File(app.getAppPath(null), BACKUP_FOLDER);
		if (!fld.exists()) {
			fld.mkdirs();
		}
		int back = 1;
		String backPrefix;
		File firstModified = null;
		long firstModifiedMin = System.currentTimeMillis();
		while (back <= BACKUP_MAX_COUNT) {
			backPrefix = "" + back;
			if (back < 10) {
				backPrefix = "0" + backPrefix;
			}
			File bak = new File(fld, BACKUP_FILE_PREFIX + backPrefix + ".gpx.bz2");
			if (!bak.exists()) {
				return bak;
			} else if (bak.lastModified() < firstModifiedMin) {
				firstModified = bak;
				firstModifiedMin = bak.lastModified();
			}
			back++;
		}
		return firstModified;
	}
}