package net.osmand.plus.myplaces.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FavoriteFolder {

	private final String fullPath;
	private final String name;
	@Nullable
	private final FavoriteFolder parent;
	private final List<FavoriteFolder> subFolders = new ArrayList<>();
	@Nullable
	private FavoriteGroup group;

	private int subtreePointsCount;
	private long subtreeFileSize;
	private int subtreeFoldersCount;
	private long subtreeLastModified;

	FavoriteFolder(@NonNull String fullPath, @Nullable FavoriteFolder parent) {
		this.fullPath = fullPath;
		this.name = FavoriteFolderPath.lastSegment(fullPath);
		this.parent = parent;
	}

	@NonNull
	public String getFullPath() {
		return fullPath;
	}

	@NonNull
	public String getName() {
		return name;
	}

	@Nullable
	public FavoriteFolder getParent() {
		return parent;
	}

	@Nullable
	public FavoriteGroup getGroup() {
		return group;
	}

	public void setGroup(@Nullable FavoriteGroup group) {
		this.group = group;
	}

	public boolean isVirtual() {
		return group == null;
	}

	public boolean isRoot() {
		return parent == null;
	}

	@NonNull
	public List<FavoriteFolder> getSubFolders() {
		return Collections.unmodifiableList(subFolders);
	}

	public void addSubFolder(@NonNull FavoriteFolder subFolder) {
		subFolders.add(subFolder);
	}

	public void sortSubFolders(@NonNull Collator collator) {
		subFolders.sort((lhs, rhs) -> {
			if (FavoriteGroup.PERSONAL_CATEGORY.equals(lhs.getFullPath())) {
				return -1;
			} else if (FavoriteGroup.PERSONAL_CATEGORY.equals(rhs.getFullPath())) {
				return 1;
			}
			return collator.compare(lhs.getName(), rhs.getName());
		});
		for (FavoriteFolder subFolder : subFolders) {
			subFolder.sortSubFolders(collator);
		}
	}

	public void updateSubtreeStats() {
		int pointsCount = getExactPointsCount();
		long fileSize = group != null ? group.getSize() : 0;
		int foldersCount = subFolders.size();
		long lastModified = group != null ? group.getTimeModified() : 0;
		for (FavoriteFolder subFolder : subFolders) {
			subFolder.updateSubtreeStats();
			pointsCount += subFolder.subtreePointsCount;
			fileSize += subFolder.subtreeFileSize;
			foldersCount += subFolder.subtreeFoldersCount;
			lastModified = Math.max(lastModified, subFolder.subtreeLastModified);
		}
		subtreePointsCount = pointsCount;
		subtreeFileSize = fileSize;
		subtreeFoldersCount = foldersCount;
		subtreeLastModified = lastModified;
	}

	@NonNull
	public List<FavouritePoint> getExactPoints() {
		return group != null ? group.getPoints() : Collections.emptyList();
	}

	public int getExactPointsCount() {
		return group != null ? group.getPoints().size() : 0;
	}

	public int getSubtreePointsCount() {
		return subtreePointsCount;
	}

	public long getSubtreeFileSize() {
		return subtreeFileSize;
	}

	public int getSubtreeFoldersCount() {
		return subtreeFoldersCount;
	}

	public long getSubtreeLastModified() {
		return subtreeLastModified;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof FavoriteFolder folder)) {
			return false;
		}
		return Objects.equals(fullPath, folder.fullPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fullPath);
	}
}
