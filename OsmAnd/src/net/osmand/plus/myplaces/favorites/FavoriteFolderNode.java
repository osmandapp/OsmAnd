package net.osmand.plus.myplaces.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FavoriteFolderNode {

	private final String fullPath;
	private final String name;
	@Nullable
	private final FavoriteFolderNode parent;
	private final List<FavoriteFolderNode> children = new ArrayList<>();
	@Nullable
	private FavoriteGroup group;

	FavoriteFolderNode(@NonNull String fullPath, @Nullable FavoriteFolderNode parent) {
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
	public FavoriteFolderNode getParent() {
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
	public List<FavoriteFolderNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public void addChild(@NonNull FavoriteFolderNode child) {
		children.add(child);
	}

	public void sortChildren(@NonNull Collator collator) {
		children.sort((lhs, rhs) -> {
			if (FavoriteGroup.PERSONAL_CATEGORY.equals(lhs.getFullPath())) {
				return -1;
			} else if (FavoriteGroup.PERSONAL_CATEGORY.equals(rhs.getFullPath())) {
				return 1;
			}
			return collator.compare(lhs.getName(), rhs.getName());
		});
		for (FavoriteFolderNode child : children) {
			child.sortChildren(collator);
		}
	}

	@NonNull
	public List<FavouritePoint> getExactPoints() {
		return group != null ? group.getPoints() : Collections.emptyList();
	}

	public int getExactPointsCount() {
		return group != null ? group.getPoints().size() : 0;
	}

	public int getSubtreePointsCount() {
		int count = getExactPointsCount();
		for (FavoriteFolderNode child : children) {
			count += child.getSubtreePointsCount();
		}
		return count;
	}

	public long getSubtreeFileSize() {
		long size = group != null ? group.getSize() : 0;
		for (FavoriteFolderNode child : children) {
			size += child.getSubtreeFileSize();
		}
		return size;
	}

	public int getSubtreeFoldersCount() {
		int count = children.size();
		for (FavoriteFolderNode child : children) {
			count += child.getSubtreeFoldersCount();
		}
		return count;
	}

	public long getSubtreeLastModified() {
		long lastModified = group != null ? group.getTimeModified() : 0;
		for (FavoriteFolderNode child : children) {
			lastModified = Math.max(lastModified, child.getSubtreeLastModified());
		}
		return lastModified;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof FavoriteFolderNode node)) {
			return false;
		}
		return Objects.equals(fullPath, node.fullPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fullPath);
	}
}
