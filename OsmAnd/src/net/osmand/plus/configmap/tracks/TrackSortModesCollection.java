package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
		return id != null ? cachedSortModes.get(id) : null;
	}

	public void setSortMode(@NonNull String id, @NonNull TracksSortMode sortMode) {
		cachedSortModes.put(id, sortMode);
	}

	public void clearSurplusKeys(@NonNull Collection<String> validKeys) {
		Set<String> keysToClear = new HashSet<>();
		for (String key : cachedSortModes.keySet()) {
			if (!validKeys.contains(key)) {
				keysToClear.add(key);
			}
		}
		for (String key : keysToClear) {
			cachedSortModes.remove(key);
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
