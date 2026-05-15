package net.osmand.plus.myplaces.favorites.dialogs;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.myplaces.favorites.FavoriteFolderNode;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FavoriteSelection {

	private final List<FavouritePoint> points = new ArrayList<>();
	private final List<FavoriteFolderNode> folderNodes = new ArrayList<>();
	private final List<FavoriteGroup> exactGroups = new ArrayList<>();

	FavoriteSelection(@NonNull Iterable<?> items) {
		for (Object item : items) {
			if (item instanceof FavouritePoint point) {
				points.add(point);
			} else if (item instanceof FavoriteFolderNode folder) {
				folderNodes.add(folder);
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
	public List<FavoriteFolderNode> getFolderNodes() {
		return folderNodes;
	}

	@NonNull
	public Set<FavoriteGroup> getExactGroups() {
		return new LinkedHashSet<>(exactGroups);
	}

	public boolean hasPoints() {
		return !points.isEmpty();
	}

	public boolean hasFolders() {
		return !folderNodes.isEmpty() || !exactGroups.isEmpty();
	}

	public boolean hasFolderNodes() {
		return !folderNodes.isEmpty();
	}

	public boolean isOnlyPoints() {
		return hasPoints() && !hasFolders();
	}

	public boolean isOnlyExactGroups() {
		return !hasPoints() && !hasFolderNodes() && !exactGroups.isEmpty();
	}

	public boolean isMixed() {
		return hasPoints() && hasFolders();
	}
}
