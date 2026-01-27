package net.osmand.plus.myplaces.favorites.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.shared.gpx.data.OrganizedTracksGroup;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class FavoriteSortModesHelper {

	private final OsmandApplication app;
	private static final String SEPARATOR = ",,";

	private final Map<String, FavoriteListSortMode> cachedSortModes = new ConcurrentHashMap<>();
	private final ListStringPreference preference;

	public FavoriteSortModesHelper(@NonNull OsmandApplication app) {
		this.app = app;
		OsmandSettings settings = app.getSettings();
		preference = settings.FAVORITE_SORT_MODES;
		loadFromPreference();
	}

	@NonNull
	public FavoriteListSortMode requireSortMode(@Nullable String id) {
		FavoriteListSortMode sortMode = id != null ? getSortMode(id) : null;
		return sortMode != null ? sortMode : FavoriteListSortMode.getDefaultSortMode();
	}

	@Nullable
	public FavoriteListSortMode getSortMode(@NonNull String id) {
		FavoriteListSortMode sortMode = cachedSortModes.get(getInternalId(id));
		return sortMode != null ? sortMode : FavoriteListSortMode.getDefaultSortMode();
	}

	public void setSortMode(@NonNull String id,
	                        @Nullable FavoriteListSortMode sortMode) {
		String internalId = getInternalId(id);
		if (sortMode != null) {
			cachedSortModes.put(internalId, sortMode);
		} else {
			cachedSortModes.remove(internalId);
		}
	}

	public void onFavoriteFolderDeleted(@NonNull FavoriteGroup favoriteGroup) {
		String folderId = favoriteGroup.getDisplayName(app);
		clearRelatedKeys(folderId);
		syncSettings();
	}

	public void clearRelatedKeys(@NonNull String folderId) {
		List<String> prefixesToRemove = getRelatedKeyPrefixes(folderId);
		if (prefixesToRemove.isEmpty()) return;

		cachedSortModes.keySet().removeIf(key -> {
			for (String prefix : prefixesToRemove) {
				if (key.startsWith(prefix)) {
					return true;
				}
			}
			return false;
		});
	}

	@NonNull
	private List<String> getRelatedKeyPrefixes(@NonNull String folderId) {
		List<String> prefixes = new ArrayList<>();
		prefixes.add(OrganizedTracksGroup.Companion.getBaseId(folderId));
		return prefixes;
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
					String internalId = removeExtraFileSeparator(tokenParts[0]);
					cachedSortModes.put(internalId, FavoriteListSortMode.getByValue(tokenParts[1]));
				}
			}
		}
	}

	private void saveToPreference() {
		List<String> tokens = new ArrayList<>();
		for (Entry<String, FavoriteListSortMode> entry : cachedSortModes.entrySet()) {
			FavoriteListSortMode value = entry.getValue();
			tokens.add(entry.getKey() + SEPARATOR + value.name());
		}
		preference.setStringsList(tokens);
	}

	@NonNull
	public static String getInternalId(@NonNull String folderId) {
		return removeExtraFileSeparator(folderId);
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
