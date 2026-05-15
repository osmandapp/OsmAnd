package net.osmand.plus.myplaces.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FavoriteFolderTree {

	private final FavoriteFolderNode root = new FavoriteFolderNode("", null);
	private final Map<String, FavoriteFolderNode> nodes = new LinkedHashMap<>();

	public FavoriteFolderTree(@NonNull Collection<FavoriteGroup> groups) {
		nodes.put(root.getFullPath(), root);
		for (FavoriteGroup group : groups) {
			addGroup(group);
		}
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		root.sortChildren(collator);
	}

	private void addGroup(@NonNull FavoriteGroup group) {
		String fullPath = group.getName();
		if (Algorithms.isEmpty(fullPath)) {
			root.setGroup(group);
			return;
		}
		FavoriteFolderNode parent = root;
		String currentPath = "";
		for (String segment : FavoriteFolderPath.split(fullPath)) {
			currentPath = Algorithms.isEmpty(currentPath)
					? segment
					: currentPath + FavoriteFolderPath.DELIMITER + segment;
			FavoriteFolderNode node = nodes.get(currentPath);
			if (node == null) {
				node = new FavoriteFolderNode(currentPath, parent);
				nodes.put(currentPath, node);
				parent.addChild(node);
			}
			parent = node;
		}
		parent.setGroup(group);
	}

	@NonNull
	public FavoriteFolderNode getRoot() {
		return root;
	}

	@NonNull
	public List<FavoriteFolderNode> getRootChildren() {
		return root.getChildren();
	}

	@Nullable
	public FavoriteFolderNode getNode(@NonNull String fullPath) {
		return nodes.get(fullPath);
	}

	public boolean hasNode(@NonNull String fullPath) {
		return nodes.containsKey(fullPath);
	}

	@NonNull
	public List<FavoriteFolderNode> flatten() {
		return flatten(false);
	}

	@NonNull
	public List<FavoriteFolderNode> flatten(boolean includeRoot) {
		List<FavoriteFolderNode> result = new ArrayList<>();
		if (includeRoot) {
			result.add(root);
		}
		for (FavoriteFolderNode child : root.getChildren()) {
			collectFlattenedNodes(child, result);
		}
		return result;
	}

	private void collectFlattenedNodes(@NonNull FavoriteFolderNode node, @NonNull List<FavoriteFolderNode> result) {
		result.add(node);
		for (FavoriteFolderNode child : node.getChildren()) {
			collectFlattenedNodes(child, result);
		}
	}

	@NonNull
	public List<FavoriteFolderNode> getNodesInSubtree(@NonNull String fullPath) {
		List<FavoriteFolderNode> result = new ArrayList<>();
		FavoriteFolderNode node = getNode(fullPath);
		if (node != null) {
			collectFlattenedNodes(node, result);
		}
		return result;
	}

	@NonNull
	public List<FavoriteGroup> getGroupsInSubtree(@NonNull String fullPath) {
		List<FavoriteGroup> result = new ArrayList<>();
		for (FavoriteFolderNode node : getNodesInSubtree(fullPath)) {
			FavoriteGroup group = node.getGroup();
			if (group != null) {
				result.add(group);
			}
		}
		return result;
	}
}
