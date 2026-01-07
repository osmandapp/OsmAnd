package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.enums.TracksSortScope;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TrackSortModesHelper {

	private static final String ROOT_FOLDER_ID = "";
	private static final String SEPARATOR = ",,";
	private static final String SCOPE_SEPARATOR = "::";

	private final Map<String, TracksSortMode> cachedSortModes = new ConcurrentHashMap<>();
	private final ListStringPreference preference;

	public TrackSortModesHelper(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		preference = settings.TRACKS_TABS_SORT_MODES;
		loadFromPreference(app);
	}

	@NonNull
	public TracksSortMode getRootFolderSortMode() {
		return requireSortMode(ROOT_FOLDER_ID, TracksSortScope.TRACKS);
	}

	@NonNull
	public TracksSortMode requireSortMode(@Nullable String id, @NonNull TracksSortScope scope) {
		TracksSortMode sortMode = id != null ? getSortMode(id, scope) : null;
		return sortMode != null ? sortMode : TracksSortMode.getDefaultSortMode(id, scope);
	}

	@Nullable
	public TracksSortMode getSortMode(@NonNull String id, @NonNull TracksSortScope scope) {
		TracksSortMode sortMode = cachedSortModes.get(getInternalId(id, scope));
		return sortMode != null ? TracksSortMode.getValidOrDefault(id, scope, sortMode) : null;
	}

	public void setSortMode(@NonNull String id,
	                        @NonNull TracksSortScope scope,
	                        @Nullable TracksSortMode sortMode) {
		String internalId = getInternalId(id, scope);
		if (sortMode != null) {
			cachedSortModes.put(internalId, sortMode);
		} else {
			cachedSortModes.remove(internalId);
		}
	}

	public void setSortModes(@NonNull Map<String, TracksSortMode> sortModes) {
		// The extra file separator is not checked here to avoid redundant validations.
		// It should be handled in the methods that call this one.
		cachedSortModes.clear();
		cachedSortModes.putAll(sortModes);
	}

	public void updateAfterMoveTrackFolder(@NonNull TrackFolder trackFolder, @NonNull File oldDir) {
		String previousId = getFolderId(oldDir.getAbsolutePath());
		String newId = trackFolder.getId();
		boolean updated = false;
		for (TracksSortScope scope : trackFolder.getSupportedSortScopes()) {
			TracksSortMode sortMode = getSortMode(previousId, scope);
			if (sortMode != null) {
				setSortMode(newId, scope, sortMode);
				updated = true;
			}
		}
		if (updated) {
			syncSettings();
		}
	}

	public void updateAfterDeleteTrackFolder(@NonNull TrackFolder trackFolder) {
		String folderId = trackFolder.getId();
		for (TracksSortScope scope : trackFolder.getSupportedSortScopes()) {
			setSortMode(folderId, scope, null);
		}
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
					String internalId = removeExtraFileSeparator(tokenParts[0]);
					cachedSortModes.put(internalId, TracksSortMode.getByValue(tokenParts[1]));
				}
			}
			UpgradeTrackSortModeKeysAlgorithm.Companion.execute(app, this);
		}
	}

	private void saveToPreference() {
		List<String> tokens = new ArrayList<>();
		for (Entry<String, TracksSortMode> entry : cachedSortModes.entrySet()) {
			TracksSortMode value = entry.getValue();
			tokens.add(entry.getKey() + SEPARATOR + value.name());
		}
		preference.setStringsList(tokens);
	}

	@NonNull
	public Set<String> getAllCachedInternalIds() {
		return cachedSortModes.keySet();
	}

	@Nullable
	public TracksSortMode getRawSortMode(@NonNull String internalId) {
		return cachedSortModes.get(internalId);
	}

	@NonNull
	public static String getInternalId(@NonNull String folderId, @NonNull TracksSortScope scope) {
		String cleanId = removeExtraFileSeparator(folderId);
		if (scope == TracksSortScope.TRACKS) {
			return cleanId;
		}
		return cleanId + SCOPE_SEPARATOR + scope.name();
	}

	@NonNull
	public static String getFolderId(@NonNull String absolutePath) {
		String basePath = IndexConstants.GPX_INDEX_DIR;
		int index = absolutePath.indexOf(basePath);
		if (index > 0) {
			index += basePath.length();
			String relativePath = absolutePath.substring(index);
			return removeExtraFileSeparator(relativePath);
		} else if (absolutePath.endsWith(removeExtraFileSeparator(basePath))) {
			return ROOT_FOLDER_ID;
		}
		return absolutePath;
	}

	@NonNull
	private static String removeExtraFileSeparator(@NonNull String id) {
		// Ensure consistency by removing trailing File.separator from relative paths
		// before querying or saving to settings to avoid key mismatches.
		if (id.endsWith(File.separator)) {
			return id.substring(0, id.length() - 1);
		}
		return id;
	}
}
