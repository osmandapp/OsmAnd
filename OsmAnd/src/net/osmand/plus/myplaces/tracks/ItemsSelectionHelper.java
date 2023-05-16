package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ItemsSelectionHelper<T> {

	private final Set<T> allItems = Collections.synchronizedSet(new HashSet<>());
	private final Set<T> selectedItems = Collections.synchronizedSet(new HashSet<>());
	private final Set<T> originalSelectedItems = Collections.synchronizedSet(new HashSet<>());

	@NonNull
	public Set<T> getAllItems() {
		return new HashSet<>(allItems);
	}

	@NonNull
	public Set<T> getSelectedItems() {
		return new HashSet<>(selectedItems);
	}

	@NonNull
	public Set<T> getOriginalSelectedItems() {
		return new HashSet<>(originalSelectedItems);
	}

	public void setAllItems(@NonNull Collection<T> allItems) {
		this.allItems.clear();
		this.allItems.addAll(allItems);
	}

	public void setSelectedItems(Collection<T> selectedItems) {
		this.selectedItems.clear();
		this.selectedItems.addAll(selectedItems);
	}

	public void setOriginalSelectedItems(Collection<T> originalSelectedItems) {
		this.originalSelectedItems.clear();
		this.originalSelectedItems.addAll(originalSelectedItems);
	}

	public void onItemsSelected(@NonNull Collection<T> items, boolean selected) {
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

	public boolean isItemSelected(@NonNull T item) {
		return selectedItems.contains(item);
	}

	public boolean isItemsSelected(@NonNull Set<T> items) {
		return selectedItems.containsAll(items);
	}

	public boolean hasSelectedItems() {
		return !selectedItems.isEmpty();
	}

	public int getSelectedItemsSize(){
		return selectedItems.size();
	}
}
