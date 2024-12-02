package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TrackSortModesHelper {

	private static final String ROOT_FOLDER = IndexConstants.GPX_INDEX_DIR;
	private static final String SEPARATOR = ",,";

	private final Map<String, TracksSortMode> cachedSortModes = new ConcurrentHashMap<>();
	private final ListStringPreference preference;

	public TrackSortModesHelper(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		preference = settings.TRACKS_TABS_SORT_MODES;
		loadFromPreference(app);
	}

	@NonNull
	public TracksSortMode getRootFolderSortMode() {
		return requireSortMode("");
	}

	@NonNull
	public TracksSortMode requireSortMode(@Nullable String id) {
		TracksSortMode sortMode = getSortMode(id);
		return sortMode != null ? sortMode : TracksSortMode.getDefaultSortMode();
	}

	@Nullable
	public TracksSortMode getSortMode(@Nullable String id) {
		id = removeExtraFileSeparator(id);
		return cachedSortModes.get(id);
	}

	public void setSortMode(@NonNull String id, @NonNull TracksSortMode sortMode) {
		id = removeExtraFileSeparator(id);
		cachedSortModes.put(id, sortMode);
	}

	public void setSortModes(@NonNull Map<String, TracksSortMode> sortModes) {
		cachedSortModes.clear();
		cachedSortModes.putAll(sortModes);
	}

	public void updateAfterMoveTrackFolder(@NonNull TrackFolder trackFolder, @NonNull File oldDir) {
		String previousId = getFolderId(oldDir.getAbsolutePath());
		TracksSortMode sortMode = getSortMode(previousId);
		if (sortMode != null) {
			setSortMode(trackFolder.getId(), sortMode);
			syncSettings();
		}
	}

	public void updateAfterDeleteTrackFolder(@NonNull TrackFolder trackFolder) {
		cachedSortModes.remove(trackFolder.getId());
		syncSettings();
	}

	public void syncSettings() {
		saveToPreference();
	}

	private void loadFromPreference(@NonNull OsmandApplication app) {
		List<String> tokens = preference.getStringsList();
		if (!Algorithms.isEmpty(tokens)) {
			for (String token : tokens) {
				String[] tokenParts = token.split(SEPARATOR);
				if (tokenParts.length == 2) {
					cachedSortModes.put(tokenParts[0], TracksSortMode.getByValue(tokenParts[1]));
				}
			}
			UpgradeTrackSortModeKeysAlgorithm.execute(app, this);
		}
	}

	private void saveToPreference() {
		List<String> tokens = new ArrayList<>();
		for (Entry<String, TracksSortMode> entry : cachedSortModes.entrySet()) {
			tokens.add(entry.getKey() + SEPARATOR + entry.getValue().name());
		}
		preference.setStringsList(tokens);
	}

	@NonNull
	public static String getFolderId(@NonNull String absolutePath) {
		int index = absolutePath.indexOf(ROOT_FOLDER);
		if (index > 0) {
			index += ROOT_FOLDER.length();
		}
		return index > 0 ? absolutePath.substring(index) : absolutePath;
	}

	@Nullable
	private static String removeExtraFileSeparator(@Nullable String id) {
		// Ensure consistency by removing trailing File.separator from relative paths
		// before querying or saving to settings to avoid key mismatches.
		if (id != null && id.endsWith(File.separator)) {
			return id.substring(0, id.length() - 1);
		}
		return id;
	}
}
