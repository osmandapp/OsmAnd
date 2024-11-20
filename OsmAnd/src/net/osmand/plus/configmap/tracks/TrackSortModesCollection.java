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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TrackSortModesCollection {

	private static final String ROOT_FOLDER = IndexConstants.GPX_INDEX_DIR;
	private static final String SEPARATOR = ",,";

	private final Map<String, TracksSortMode> cachedSortModes = new ConcurrentHashMap<>();
	private final ListStringPreference preference;

	public TrackSortModesCollection(@NonNull OsmandSettings settings) {
		this.preference = settings.TRACKS_TABS_SORT_MODES;
		loadFromPreference();
	}

	@NonNull
	public TracksSortMode getRootSortMode() {
		return requireSortMode(ROOT_FOLDER);
	}

	@NonNull
	public TracksSortMode requireSortMode(@Nullable String id) {
		TracksSortMode sortMode = getSortMode(id);
		return sortMode != null ? sortMode : TracksSortMode.getDefaultSortMode();
	}

	@Nullable
	public TracksSortMode getSortMode(@Nullable String id) {
		TracksSortMode sortMode = cachedSortModes.get(id);
		if (sortMode == null && id != null && isFolderIdV2(id)) {
			String idV1 = getFolderIdV1(id);
			if (idV1 != null) {
				sortMode = cachedSortModes.get(idV1);
			}
		}
		return sortMode;
	}

	public void setSortMode(@NonNull String id, @NonNull TracksSortMode sortMode) {
		cachedSortModes.put(id, sortMode);
	}

	public void askSyncWithUpgrade(@NonNull OsmandApplication app, @NonNull TrackFolder trackFolder) {
		askSyncWithUpgrade(app, trackFolder, false);
	}

	public void askSyncWithUpgrade(@NonNull OsmandApplication app, @NonNull TrackFolder trackFolder,
	                               boolean forceSync) {
		if (askUpgradeCachedKeys(app, trackFolder) || forceSync) {
			syncSettings();
		}
	}

	/**
	 * Removes surplus keys and upgrades outdated ones.
	 * Upgraded keys will use folder relative path as a key (V2) instead of folder name (V1).
	 */
	private boolean askUpgradeCachedKeys(@NonNull OsmandApplication app, @NonNull TrackFolder trackFolder) {
		TrackFolder rootFolder = trackFolder.getRootFolder();
		if (!Objects.equals(rootFolder.getDirFile(), app.getAppPathKt(ROOT_FOLDER))) {
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

	public void updateAfterMoveTrackFolder(@NonNull OsmandApplication app,
	                                       @NonNull TrackFolder trackFolder, @NonNull File oldDir) {
		String previousId = getFolderId(oldDir.getAbsolutePath());
		TracksSortMode sortMode = getSortMode(previousId);
		if (sortMode != null) {
			setSortMode(trackFolder.getId(), sortMode);
			askSyncWithUpgrade(app, trackFolder, true);
		}
	}

	public void updateAfterDeleteTrackFolder(@NonNull OsmandApplication app,
	                                         @NonNull TrackFolder trackFolder) {
		cachedSortModes.remove(trackFolder.getId());
		askSyncWithUpgrade(app, trackFolder, true);
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

	@NonNull
	public static String getFolderId(@NonNull String absolutePath) {
		int index = absolutePath.indexOf(ROOT_FOLDER);
		return index > 0 ? absolutePath.substring(index) : absolutePath;
	}

	private static boolean isFolderIdV2(@NonNull String id) {
		return id.startsWith(ROOT_FOLDER);
	}

	@Nullable
	private static String getFolderIdV1(@NonNull String idV2) {
		int index = idV2.lastIndexOf(File.separator);
		return index > 0 ? idV2.substring(index + 1) : null;
	}
}
