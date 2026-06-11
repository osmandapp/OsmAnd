package net.osmand.plus.plugins.audionotes;

import static net.osmand.IndexConstants.AV_INDEX_DIR;

import android.media.CamcorderProfile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.DataTileManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordingsFileHelper {

	private static final Log log = PlatformUtil.getLog(RecordingsFileHelper.class);

	public static final int CLIP_LENGTH_DEFAULT = 5;
	public static final int STORAGE_SIZE_DEFAULT = 5;
	public static final String IMG_EXTENSION = "jpg";
	public static final String MPEG4_EXTENSION = "mp4";
	public static final String THREEGP_EXTENSION = "3gp";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public final CommonPreference<Boolean> AV_RECORDER_SPLIT;
	public final CommonPreference<Integer> AV_RS_CLIP_LENGTH;
	public final CommonPreference<Integer> AV_RS_STORAGE_SIZE;

	private final DataTileManager<Recording> recordings = new DataTileManager<>(14);
	private Map<String, Recording> recordingByFileName = new LinkedHashMap<>();

	public RecordingsFileHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();

		AV_RECORDER_SPLIT = settings.registerBooleanPreference("av_recorder_split", false);
		AV_RS_CLIP_LENGTH = settings.registerIntPreference("av_rs_clip_length", CLIP_LENGTH_DEFAULT);
		AV_RS_STORAGE_SIZE = settings.registerIntPreference("av_rs_storage_size", STORAGE_SIZE_DEFAULT);
	}

	@NonNull
	public DataTileManager<Recording> getRecordings() {
		return recordings;
	}

	@NonNull
	public Map<String, Recording> getRecordingByFileName() {
		return recordingByFileName;
	}

	@Nullable
	public List<String> indexingFiles(boolean reIndexAndKeepOld, boolean registerNew) {
		File avPath = app.getAppPath(AV_INDEX_DIR);
		if (avPath.canRead()) {
			if (!reIndexAndKeepOld) {
				recordings.clear();
				recordingByFileName = new LinkedHashMap<>();
			}
			File[] files = avPath.listFiles();
			if (files != null) {
				for (File file : files) {
					indexFile(registerNew, file, false);
				}
			}
		}
		return null;
	}

	public boolean indexSingleFile(@NonNull File file, boolean updatePhotoInformation) {
		boolean oldFileExist = recordingByFileName.containsKey(file.getName());
		if (oldFileExist) {
			return false;
		}
		Recording recording = new Recording(file);
		String fileName = file.getName();
		String otherName = recording.getOtherName(fileName);
		int i = otherName.indexOf('.');
		if (i > 0) {
			otherName = otherName.substring(0, i);
		}
		recording.setFile(file);
		GeoParsedPoint geo = MapUtils.decodeShortLinkString(otherName);
		recording.setLatitude(geo.getLatitude());
		recording.setLongitude(geo.getLongitude());
		Float heading = app.getLocationProvider().getHeading();
		Location loc = app.getLocationProvider().getLastKnownLocation();

		if (updatePhotoInformation) {
			float rot = heading != null ? heading : 0;
			try {
				recording.updatePhotoInformation(recording.getLatitude(), recording.getLongitude(), loc, rot == 0 ? Double.NaN : rot);
			} catch (IOException e) {
				log.error("Error updating EXIF information " + e.getMessage(), e);
			}
		}
		recordings.registerObject(recording.getLatitude(), recording.getLongitude(), recording);

		Map<String, Recording> newMap = new LinkedHashMap<>(recordingByFileName);
		newMap.put(file.getName(), recording);
		recordingByFileName = newMap;

		return true;
	}

	public void deleteRecording(@NonNull Recording recording) {
		recordings.unregisterObject(recording.getLatitude(), recording.getLongitude(), recording);
		Map<String, Recording> newMap = new LinkedHashMap<>(recordingByFileName);
		newMap.remove(recording.getFile().getName());
		recordingByFileName = newMap;
		Algorithms.removeAllFiles(recording.getFile());
	}

	boolean indexFile(boolean registerInGPX, @NonNull File file, boolean updatePhotoInformation) {
		String name = file.getName();
		if (CollectionUtils.endsWithAny(name, THREEGP_EXTENSION, MPEG4_EXTENSION, IMG_EXTENSION)) {
			boolean newFileIndexed = indexSingleFile(file, updatePhotoInformation);
			if (newFileIndexed && registerInGPX) {
				Recording recording = recordingByFileName.get(name);
				if (recording != null && (settings.SAVE_TRACK_TO_GPX.get() || settings.SAVE_GLOBAL_TRACK_TO_GPX.get())
						&& PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
					app.getSavingTrackHelper().insertPointData(recording.getLatitude(), recording.getLongitude(), null, name, null, 0);
				}
			}
			return newFileIndexed;
		}
		return false;
	}

	@Nullable
	Recording getNewRecording(@NonNull Set<String> indexedFiles) {
		Recording result = null;
		for (Recording recording : recordingByFileName.values()) {
			if (!indexedFiles.contains(recording.getFileName())
					&& (result == null || recording.getLastModified() > result.getLastModified())) {
				result = recording;
			}
		}
		return result;
	}

	boolean cleanupSpace(@NonNull CamcorderProfile profile) {
		File[] files = app.getAppPath(AV_INDEX_DIR).listFiles((dir, filename) -> filename.endsWith("." + MPEG4_EXTENSION));

		if (files != null) {
			double usedSpace = 0;
			for (File f : files) {
				usedSpace += f.length();
			}
			usedSpace /= (1 << 30); // gigabytes

			double bitrate = (((profile.videoBitRate + profile.audioBitRate) / 8f) * 60f) / (1 << 30); // gigabytes per minute
			double clipSpace = bitrate * AV_RS_CLIP_LENGTH.get();
			double storageSize = AV_RS_STORAGE_SIZE.get();
			double availableSpace = (double) AndroidUtils.getAvailableSpace(app) / (1 << 30) - clipSpace;

			if (usedSpace + clipSpace > storageSize || clipSpace > availableSpace) {
				Arrays.sort(files, (lhs, rhs) -> Long.compare(lhs.lastModified(), rhs.lastModified()));
				boolean wasAnyDeleted = false;
				ArrayList<File> arr = new ArrayList<>(Arrays.asList(files));
				while (arr.size() > 0 && (usedSpace + clipSpace > storageSize || clipSpace > availableSpace)) {
					File f = arr.remove(0);
					double length = ((double) f.length()) / (1 << 30);
					Recording r = recordingByFileName.get(f.getName());
					if (r != null) {
						deleteRecording(r);
						wasAnyDeleted = true;
						usedSpace -= length;
						availableSpace += length;
					} else if (f.delete()) {
						usedSpace -= length;
						availableSpace += length;
					}
				}
				return wasAnyDeleted;
			}
		}
		return false;
	}

	@NonNull
	public static File getBaseFileName(double lat, double lon, OsmandApplication app, String ext) {
		return getBaseFileName(lat, lon, app.getAppPath(AV_INDEX_DIR), ext);
	}

	@NonNull
	public static File getBaseFileName(double lat, double lon, @NonNull File dir, @NonNull String ext) {
		String basename = MapUtils.createShortLinkString(lat, lon, 15);
		int index = 1;
		dir.mkdirs();
		File file;
		do {
			file = new File(dir, basename + "." + (index++) + "." + ext);
		} while (file.exists());
		return file;
	}
}