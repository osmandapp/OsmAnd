package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.OrganizedTracksGroup;
import net.osmand.shared.gpx.enums.TracksSortScope;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes track sorting preference keys and preserves compatibility with the legacy V1 format.
 *
 * <p>V1 stored only a folder name, while V2 stores a relative folder path. Reading a V1 key is a
 * non-mutating fallback: the preference is not rewritten during startup. Because persisted keys
 * have no version marker, rename and delete operations modify only keys that are structurally
 * identifiable as V2; a plain top-level key can also be a legacy V1 key and must be retained.</p>
 */
final class TrackSortModeKeyUtils {

	static final String ROOT_FOLDER_ID = "";

	private static final String SEPARATOR = ",,";
	private static final String SCOPE_SEPARATOR = "::";

	private TrackSortModeKeyUtils() {
	}

	@Nullable
	static TracksSortMode resolveSortMode(@NonNull Map<String, TracksSortMode> sortModes,
	                                      @NonNull String id, @NonNull TracksSortScope scope) {
		String cleanId = removeExtraFileSeparator(id);
		TracksSortMode sortMode = sortModes.get(getSortModeKey(cleanId, scope));
		if (sortMode == null && scope == TracksSortScope.TRACKS) {
			String legacyId = getLegacyFolderId(cleanId);
			if (legacyId != null) {
				sortMode = sortModes.get(legacyId);
			}
		}
		return sortMode != null ? TracksSortMode.getValidOrDefault(id, scope, sortMode) : null;
	}

	@NonNull
	static Map<String, TracksSortMode> parseSortModes(@NonNull List<String> tokens) {
		Map<String, TracksSortMode> sortModes = new HashMap<>();
		if (!Algorithms.isEmpty(tokens)) {
			for (String token : tokens) {
				String[] tokenParts = token.split(SEPARATOR);
				if (tokenParts.length == 2) {
					String internalId = removeExtraFileSeparator(tokenParts[0]);
					sortModes.put(internalId, TracksSortMode.getByValue(tokenParts[1]));
				}
			}
		}
		return sortModes;
	}

	@NonNull
	static List<String> serializeSortModes(@NonNull Map<String, TracksSortMode> sortModes) {
		List<String> tokens = new ArrayList<>();
		List<String> internalIds = new ArrayList<>(sortModes.keySet());
		Collections.sort(internalIds);
		for (String internalId : internalIds) {
			TracksSortMode value = sortModes.get(internalId);
			if (value != null) {
				tokens.add(internalId + SEPARATOR + value.name());
			}
		}
		return tokens;
	}

	@NonNull
	static String getSortModeKey(@NonNull String folderId, @NonNull TracksSortScope scope) {
		String cleanId = removeExtraFileSeparator(folderId);
		return scope == TracksSortScope.TRACKS ? cleanId : cleanId + SCOPE_SEPARATOR + scope.name();
	}

	@NonNull
	static String getFolderId(@NonNull String absolutePath) {
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

	/**
	 * Moves only keys that are structurally identifiable as V2. V1 and top-level V2 keys use the
	 * same plain string format, so moving a plain key could lose a legacy setting.
	 */
	static boolean moveUnambiguousV2Keys(@NonNull Map<String, TracksSortMode> sortModes,
	                                     @NonNull String previousId,
	                                     @NonNull String newId) {
		if (previousId.equals(newId)) {
			return false;
		}
		Map<String, TracksSortMode> movedSortModes = new HashMap<>();
		for (Map.Entry<String, TracksSortMode> entry : new HashMap<>(sortModes).entrySet()) {
			String suffix = getUnambiguousV2KeySuffix(entry.getKey(), previousId);
			if (suffix != null) {
				sortModes.remove(entry.getKey());
				movedSortModes.put(newId + suffix, entry.getValue());
			}
		}
		if (movedSortModes.isEmpty()) {
			return false;
		}
		sortModes.putAll(movedSortModes);
		return true;
	}

	/**
	 * Preserves the effective legacy sorting of a nested folder after an explicit rename or move.
	 * The legacy V1 key remains untouched because it may still apply to another folder with the
	 * same leaf name. An exact V2 source is handled by {@link #moveUnambiguousV2Keys(Map, String,
	 * String)} instead, and a pre-existing destination V2 key has priority.
	 */
	static boolean copyLegacyTrackSortModeForRenamedFolder(@NonNull Map<String, TracksSortMode> sortModes,
	                                                       @NonNull String previousId,
	                                                       @NonNull String newId) {
		if (!previousId.contains(File.separator) || previousId.equals(newId)) {
			return false;
		}
		String sourceKey = getSortModeKey(previousId, TracksSortScope.TRACKS);
		String destinationKey = getSortModeKey(newId, TracksSortScope.TRACKS);
		if (sortModes.containsKey(sourceKey) || sortModes.containsKey(destinationKey)) {
			return false;
		}
		TracksSortMode sortMode = resolveSortMode(sortModes, previousId, TracksSortScope.TRACKS);
		if (sortMode == null) {
			return false;
		}
		sortModes.put(destinationKey, sortMode);
		return true;
	}

	/**
	 * Preserves the direct sorting state when a top-level folder is explicitly renamed or moved.
	 * The plain source key remains because it may be a legacy V1 key for another folder with the
	 * same name. A pre-existing key for the destination has priority over the copied value.
	 */
	static boolean copyTopLevelFolderSortMode(@NonNull Map<String, TracksSortMode> sortModes,
	                                         @NonNull String previousId,
	                                         @NonNull String newId) {
		if (previousId.isEmpty() || previousId.contains(File.separator) || previousId.equals(newId)) {
			return false;
		}
		TracksSortMode sortMode = sortModes.get(previousId);
		if (sortMode == null || sortModes.containsKey(newId)) {
			return false;
		}
		sortModes.put(newId, sortMode);
		return true;
	}

	/** Removes only structurally identifiable V2 keys after an explicit folder deletion. */
	static boolean removeUnambiguousV2Keys(@NonNull Map<String, TracksSortMode> sortModes,
	                                        @NonNull String folderId) {
		return sortModes.keySet().removeIf(key -> getUnambiguousV2KeySuffix(key, folderId) != null);
	}

	@Nullable
	private static String getLegacyFolderId(@NonNull String folderId) {
		if (folderId.isEmpty()) {
			return removeExtraFileSeparator(IndexConstants.GPX_INDEX_DIR);
		}
		int index = folderId.lastIndexOf(File.separator);
		return index > 0 ? folderId.substring(index + 1) : null;
	}

	@Nullable
	private static String getUnambiguousV2KeySuffix(@NonNull String key, @NonNull String folderId) {
		if (folderId.isEmpty()) {
			return null;
		}
		if (key.equals(folderId)) {
			return folderId.contains(File.separator) ? "" : null;
		}
		if (key.startsWith(folderId + File.separator)
				|| key.startsWith(folderId + SCOPE_SEPARATOR)
				|| key.startsWith(OrganizedTracksGroup.Companion.getBaseId(folderId))) {
			return key.substring(folderId.length());
		}
		return null;
	}

	@NonNull
	private static String removeExtraFileSeparator(@NonNull String id) {
		return id.endsWith(File.separator) ? id.substring(0, id.length() - 1) : id;
	}
}
