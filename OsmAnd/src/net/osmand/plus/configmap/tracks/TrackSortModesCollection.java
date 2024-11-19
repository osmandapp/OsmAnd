package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class TrackSortModesCollection {

	private static final String SEPARATOR = ",,";

	private final Map<String, TracksSortMode> cachedSortModes = new HashMap<>();
	private final ListStringPreference preference;

	public TrackSortModesCollection(@NonNull OsmandSettings settings) {
		this.preference = settings.TRACKS_TABS_SORT_MODES;
		loadFromPreference();
	}

	@NonNull
	public TracksSortMode getRootSortMode() {
		return requireSortMode(IndexConstants.GPX_INDEX_DIR);
	}

	@NonNull
	public TracksSortMode requireSortMode(@Nullable String id) {
		TracksSortMode sortMode = getSortMode(id);
		return sortMode != null ? sortMode : TracksSortMode.getDefaultSortMode();
	}

	@Nullable
	public TracksSortMode getSortMode(@Nullable String id) {
		TracksSortMode sortMode = cachedSortModes.get(id);
		if (sortMode == null && id != null && TrackFolderUtil.isStandardFolderId(id)) {
			String oldId = TrackFolderUtil.getOutdatedStandardFolderId(id);
			sortMode = cachedSortModes.get(oldId);
		}
		return sortMode;
	}

	public void setSortMode(@NonNull String id, @NonNull TracksSortMode sortMode) {
		cachedSortModes.put(id, sortMode);
	}

	public void replaceKey(@NonNull String oldKey, @NonNull String newKey) {
		TracksSortMode sortMode = cachedSortModes.remove(oldKey);
		if (sortMode != null) {
			cachedSortModes.put(newKey, sortMode);
		}
	}

	public void askSyncWithUpgrade(@NonNull OsmandApplication app, @NonNull TrackFolder trackFolder) {
		if (askUpgradeStoredKeys(app, trackFolder.getRootFolder())) {
			syncSettings();
		}
	}

	/**
	 * Removes surplus keys and upgrades outdated ones.
	 * Then saves sort modes with upgraded keys to the preferences.
	 */
	public boolean askUpgradeStoredKeys(@NonNull OsmandApplication app, @NonNull TrackFolder rootFolder) {
		if (!Objects.equals(rootFolder.getDirFile(), app.getAppPathKt(IndexConstants.GPX_INDEX_DIR))) {
			// Execute only from tracks root folder to not lose valid entries
			return false;
		}
		Map<String, TracksSortMode> upgradedCache = new HashMap<>();
		putUpgradedKey(upgradedCache, TrackTabType.ON_MAP.name());
		putUpgradedKey(upgradedCache, TrackTabType.ALL.name());
		putUpgradedKey(upgradedCache, rootFolder.getId());
		for (TrackFolder folder : rootFolder.getFlattenedSubFolders()) {
			putUpgradedKey(upgradedCache, folder.getId());
		}
		for (SmartFolder folder : app.getSmartFolderHelper().getSmartFolders()) {
			putUpgradedKey(upgradedCache, folder.getId());
		}
		cachedSortModes.clear();
		cachedSortModes.putAll(upgradedCache);
		return true;
	}

	private void putUpgradedKey(@NonNull Map<String, TracksSortMode> map, @NonNull String id) {
		TracksSortMode sortMode = getSortMode(id);
		if (sortMode != null) {
			map.put(id, sortMode);
		}
	}

	public void syncSettings() {
		saveToPreference();
	}

	private void loadFromPreference() {
		List<String> tokens = preference.getStringsList();
		if (!Algorithms.isEmpty(tokens)) {
			for (String token : tokens) {
				String[] tokenParts = token.split(SEPARATOR);
				if (tokenParts.length == 2) {
					cachedSortModes.put(tokenParts[0], TracksSortMode.getByValue(tokenParts[1]));
				}
			}
		}
	}

	private void saveToPreference() {
		List<String> tokens = new ArrayList<>();
		for (Entry<String, TracksSortMode> entry : cachedSortModes.entrySet()) {
			tokens.add(entry.getKey() + SEPARATOR + entry.getValue().name());
		}
		preference.setStringsList(tokens);
	}
}
