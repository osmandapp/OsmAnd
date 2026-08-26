package net.osmand.plus.myplaces.favorites.dialogs;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FavoriteSelection {

	private final List<FavouritePoint> points = new ArrayList<>();
	private final List<FavoriteFolder> folders = new ArrayList<>();
	private final List<FavoriteGroup> exactGroups = new ArrayList<>();

	FavoriteSelection(@NonNull Iterable<?> items) {
		for (Object item : items) {
			if (item instanceof FavouritePoint point) {
				points.add(point);
			} else if (item instanceof FavoriteFolder folder) {
				folders.add(folder);
			} else if (item instanceof FavoriteGroup group) {
				exactGroups.add(group);
			}
		}
	}

	@NonNull
	public Set<FavouritePoint> getPoints() {
		return new LinkedHashSet<>(points);
	}

	@NonNull
	public List<FavoriteFolder> getFolders() {
		return folders;
	}

	@NonNull
	public Set<FavoriteGroup> getExactGroups() {
		return new LinkedHashSet<>(exactGroups);
	}

	@NonNull
	public List<String> getFolderPaths() {
		Set<String> paths = new LinkedHashSet<>();
		for (FavoriteFolder folder : folders) {
			paths.add(folder.getFullPath());
		}
		for (FavoriteGroup group : exactGroups) {
			paths.add(group.getName());
		}
		return new ArrayList<>(paths);
	}

	public boolean hasPoints() {
		return !points.isEmpty();
	}

	public boolean hasFolders() {
		return !folders.isEmpty() || !exactGroups.isEmpty();
	}

	public boolean hasSnapshotFolders() {
		return !folders.isEmpty();
	}

	public boolean canMoveFolders() {
		if (!hasFolders()) {
			return false;
		}
		for (String folderPath : getFolderPaths()) {
			if (Algorithms.isEmpty(folderPath)) {
				return false;
			}
		}
		return true;
	}

	public boolean isOnlyPoints() {
		return hasPoints() && !hasFolders();
	}

	public boolean isOnlyExactGroups() {
		return !hasPoints() && !hasSnapshotFolders() && !exactGroups.isEmpty();
	}

	public boolean isMixed() {
		return hasPoints() && hasFolders();
	}
}
