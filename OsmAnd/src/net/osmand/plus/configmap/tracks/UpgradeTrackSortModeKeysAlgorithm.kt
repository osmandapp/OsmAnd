package net.osmand.plus.configmap.tracks;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.AppInitEvents.GPX_DB_INITIALIZED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDirItem;
import net.osmand.shared.gpx.SmartFolderHelper;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.io.KFile;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Removes surplus keys and upgrades outdated ones.
 * Upgraded keys will use folder relative path as a key (V2) instead of folder name (V1).
 */
public class UpgradeTrackSortModeKeysAlgorithm {

	private final OsmandApplication app;
	private final GpxDbHelper gpxDbHelper;
	private final SmartFolderHelper smartFolderHelper;
	private final TrackSortModesHelper sortModesHelper;

	private UpgradeTrackSortModeKeysAlgorithm(@NonNull OsmandApplication app,
			@NonNull TrackSortModesHelper sortModesHelper) {
		this.app = app;
		this.sortModesHelper = sortModesHelper;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.smartFolderHelper = app.getSmartFolderHelper();
	}

	public static void execute(@NonNull OsmandApplication app,
			@NonNull TrackSortModesHelper sortModesHelper) {
		new UpgradeTrackSortModeKeysAlgorithm(app, sortModesHelper).execute();
	}

	private void execute() {
		if (gpxDbHelper.isInitialized()) {
			executeImpl();
		} else {
			app.getAppInitializer().addOnProgressListener(GPX_DB_INITIALIZED, init -> executeImpl());
		}
	}

	private void executeImpl() {
		Map<String, TracksSortMode> upgradedCache = new HashMap<>();
		putUpgradedKey(upgradedCache, TrackTabType.ON_MAP.name());
		putUpgradedKey(upgradedCache, TrackTabType.ALL.name());
		putUpgradedKey(upgradedCache, TrackTabType.FOLDERS.name());
		for (String id : collectStandardDirectoriesIds()) {
			putUpgradedKey(upgradedCache, id);
		}
		for (SmartFolder folder : smartFolderHelper.getSmartFolders()) {
			putUpgradedKey(upgradedCache, folder.getId());
		}
		sortModesHelper.setSortModes(upgradedCache);
		sortModesHelper.syncSettings();
	}

	@NonNull
	private Set<String> collectStandardDirectoriesIds() {
		Set<String> result = new HashSet<>();
		// add root directory
		result.add("");
		// collect all directories registered in tracks DB
		Set<String> absolutePaths = new HashSet<>();
		for (GpxDirItem item : gpxDbHelper.getDirItems()) {
			KFile file = item.getFile();
			absolutePaths.add(file.absolutePath());
		}
		// collect all subdirectories which contain at least one gpx track
		for (GpxDataItem item : gpxDbHelper.getItems()) {
			KFile file = item.getFile();
			KFile directory = file.getParentFile();
			if (directory != null) {
				absolutePaths.add(directory.absolutePath());
			}
		}
		for (String absolutePath : absolutePaths) {
			result.add(getFolderIdV2(absolutePath));
		}
		return result;
	}

	private void putUpgradedKey(@NonNull Map<String, TracksSortMode> map, @NonNull String id) {
		TracksSortMode sortMode = getSortMode(id);
		if (sortMode != null) {
			map.put(id, sortMode);
		}
	}

	@Nullable
	private TracksSortMode getSortMode(@NonNull String id) {
		TracksSortMode sortMode = sortModesHelper.getSortMode(id);
		if (sortMode == null) {
			String idV1 = getFolderIdV1(id);
			return idV1 != null ? sortModesHelper.getSortMode(idV1) : null;
		}
		return sortMode;
	}

	@NonNull
	private static String getFolderIdV2(@NonNull String absolutePath) {
		return TrackSortModesHelper.getFolderId(absolutePath);
	}

	@Nullable
	private static String getFolderIdV1(@Nullable String id) {
		if (id != null && id.isEmpty()) {
			return GPX_INDEX_DIR;
		}
		int index = id != null ? id.lastIndexOf(File.separator) : -1;
		return index > 0 ? id.substring(index + 1) : null;
	}
}
