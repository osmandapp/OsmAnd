package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.shared.gpx.enums.TracksSortScope;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackSortModesHelper {

	private volatile Map<String, TracksSortMode> cachedSortModes = new ConcurrentHashMap<>();
	private final ListStringPreference preference;
	// Held because the preference keeps its listeners weakly.
	private final StateChangedListener<String> preferenceListener;

	public TrackSortModesHelper(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		preference = settings.TRACKS_TABS_SORT_MODES;
		reloadFromPreference();
		preferenceListener = change -> reloadFromPreference();
		preference.addListener(preferenceListener);
	}

	@NonNull
	public TracksSortMode getRootFolderSortMode() {
		return requireSortMode(TrackSortModeKeyUtils.ROOT_FOLDER_ID, TracksSortScope.TRACKS);
	}

	@NonNull
	public TracksSortMode requireSortMode(@Nullable String id, @NonNull TracksSortScope scope) {
		TracksSortMode sortMode = id != null ? getSortMode(id, scope) : null;
		return sortMode != null ? sortMode : TracksSortMode.getDefaultSortMode(id, scope);
	}

	@Nullable
	public TracksSortMode getSortMode(@NonNull String id, @NonNull TracksSortScope scope) {
		return TrackSortModeKeyUtils.resolveSortMode(cachedSortModes, id, scope);
	}

	public void setSortMode(@NonNull String id,
	                        @NonNull TracksSortScope scope,
	                        @Nullable TracksSortMode sortMode) {
		String sortModeKey = TrackSortModeKeyUtils.getSortModeKey(id, scope);
		if (sortMode != null) {
			cachedSortModes.put(sortModeKey, sortMode);
		} else {
			cachedSortModes.remove(sortModeKey);
		}
	}

	public void setSortModes(@NonNull Map<String, TracksSortMode> sortModes) {
		// The extra file separator is not checked here to avoid redundant validations.
		// It should be handled in the methods that call this one.
		cachedSortModes.clear();
		cachedSortModes.putAll(sortModes);
	}

	public void onTrackFolderIdChanged(@NonNull TracksGroup trackFolder, @NonNull File oldDir) {
		String previousId = getFolderId(oldDir.getAbsolutePath());
		String newId = trackFolder.getId();
		boolean keysChanged = TrackSortModeKeyUtils.copyLegacyTrackSortModeForRenamedFolder(cachedSortModes, previousId, newId);
		keysChanged |= TrackSortModeKeyUtils.moveUnambiguousV2Keys(cachedSortModes, previousId, newId);
		keysChanged |= TrackSortModeKeyUtils.copyTopLevelFolderSortMode(cachedSortModes, previousId, newId);
		if (keysChanged) {
			syncSettings();
		}
	}

	public void onTrackFolderDeleted(@NonNull TracksGroup trackFolder) {
		if (TrackSortModeKeyUtils.removeUnambiguousV2Keys(cachedSortModes, trackFolder.getId())) {
			syncSettings();
		}
	}

	public void syncSettings() {
		saveToPreference();
	}

	@NonNull
	public static LatLon getReferenceLocation(@NonNull OsmandApplication app,
	                                          @NonNull TracksSortMode sortMode) {
		return sortMode == TracksSortMode.NEAREST_TO_MAP_CENTER
				? app.getMapViewTrackingUtilities().getMapLocation()
				: app.getMapViewTrackingUtilities().getDefaultLocation();
	}

	@Nullable
	public static LatLon getDisplayReferenceLocation(@NonNull OsmandApplication app,
	                                                 @NonNull TracksSortMode sortMode) {
		return sortMode == TracksSortMode.NEAREST_TO_MAP_CENTER
				? getReferenceLocation(app, sortMode)
				: null;
	}

	void reloadFromPreference() {
		Map<String, TracksSortMode> stored = TrackSortModeKeyUtils.parseSortModes(preference.getStringsList());
		// Import runs on the UI thread while folder moves may mutate the cache in background,
		// so the map is replaced atomically instead of updated in place.
		cachedSortModes = new ConcurrentHashMap<>(stored);
	}

	private void saveToPreference() {
		preference.setStringsList(TrackSortModeKeyUtils.serializeSortModes(cachedSortModes));
	}

	@NonNull
	public static String getFolderId(@NonNull String absolutePath) {
		return TrackSortModeKeyUtils.getFolderId(absolutePath);
	}
}
