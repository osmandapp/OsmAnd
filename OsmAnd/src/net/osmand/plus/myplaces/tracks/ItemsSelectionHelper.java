package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemsSelectionHelper<T> {

	private final Set<T> allItems = new HashSet<>();
	private final Set<T> selectedItems = new HashSet<>();
	private final Set<T> originalSelectedItems = new HashSet<>();

	@NonNull
	public List<T> getAllItems() {
		return new ArrayList<>(allItems);
	}

	@NonNull
	public List<T> getSelectedItems() {
		return new ArrayList<>(selectedItems);
	}

	@NonNull
	public List<T> getOriginalSelectedItems() {
		return new ArrayList<>(originalSelectedItems);
	}

	public void setAllItems(@NonNull List<T> allItems) {
		this.allItems.clear();
		this.allItems.addAll(allItems);
	}

	public void setSelectedItems(List<T> selectedItems) {
		this.selectedItems.clear();
		this.selectedItems.addAll(selectedItems);
	}

	public void setOriginalSelectedItems(List<T> originalSelectedItems) {
		this.originalSelectedItems.clear();
		this.originalSelectedItems.addAll(originalSelectedItems);
	}

	public void onItemsSelected(@NonNull Set<T> items, boolean selected) {
		if (selected) {
			selectedItems.addAll(items);
		} else {
			selectedItems.removeAll(items);
		}
	}

	public boolean hasItemsToApply() {
		return !Algorithms.objectEquals(selectedItems, originalSelectedItems);
	}

	public void addItemToAll(@NonNull T item) {
		allItems.add(item);
	}
}
