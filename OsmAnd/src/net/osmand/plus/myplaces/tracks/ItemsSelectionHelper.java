package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ItemsSelectionHelper<T> {

	private final Supplier<Set<T>> setFactory;

	private final Set<T> allItems;
	private final Set<T> selectedItems;
	private final Set<T> originalSelectedItems;

	public ItemsSelectionHelper() {
		this(HashSet::new);
	}

	public ItemsSelectionHelper(boolean preserveSelectionOrder) {
		this(preserveSelectionOrder ? LinkedHashSet::new : HashSet::new);
	}

	public ItemsSelectionHelper(@NonNull Supplier<Set<T>> setFactory) {
		this.setFactory = setFactory;

		this.allItems = Collections.synchronizedSet(setFactory.get());
		this.selectedItems = Collections.synchronizedSet(setFactory.get());
		this.originalSelectedItems = Collections.synchronizedSet(setFactory.get());
	}

	public void syncWith(@NonNull ItemsSelectionHelper<T> helper) {
		setAllItems(helper.getAllItems());
		setOriginalSelectedItems(helper.getOriginalSelectedItems());
		setSelectedItems(helper.getSelectedItems());
	}

	@NonNull
	public Set<T> getAllItems() {
		return copyOf(allItems);
	}

	@NonNull
	public Set<T> getSelectedItems() {
		return copyOf(selectedItems);
	}

	@NonNull
	public Set<T> getOriginalSelectedItems() {
		return copyOf(originalSelectedItems);
	}

	private Set<T> copyOf(@NonNull Set<T> src) {
		Set<T> copy = setFactory.get();
		synchronized (src) {
			copy.addAll(src);
		}
		return copy;
	}

	public void setAllItems(@NonNull Collection<? extends T> allItems) {
		this.allItems.clear();
		this.allItems.addAll(allItems);
	}

	public void setSelectedItems(Collection<? extends T> selectedItems) {
		this.selectedItems.clear();
		this.selectedItems.addAll(selectedItems);
	}

	public void setOriginalSelectedItems(Collection<? extends T> originalSelectedItems) {
		this.originalSelectedItems.clear();
		this.originalSelectedItems.addAll(originalSelectedItems);
	}

	public void onItemsSelected(@NonNull Collection<? extends T> items, boolean selected) {
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

	public void addItemToOriginalSelected(@NonNull T item) {
		originalSelectedItems.add(item);
	}

	public boolean isItemSelected(@NonNull T item) {
		return selectedItems.contains(item);
	}

	public boolean isItemsSelected(@NonNull Collection<? extends T> items) {
		return selectedItems.containsAll(items);
	}

	public boolean isAllItemsSelected() {
		return selectedItems.containsAll(allItems);
	}

	public boolean hasSelectedItems() {
		return !selectedItems.isEmpty();
	}

	public boolean hasAnyItems() {
		return !allItems.isEmpty();
	}

	public int getSelectedItemsSize() {
		return selectedItems.size();
	}

	public void clearSelectedItems() {
		selectedItems.clear();
	}

	public void selectAllItems() {
		selectedItems.addAll(allItems);
	}

	public interface SelectionHelperProvider<T> {

		@NonNull
		ItemsSelectionHelper<T> getSelectionHelper();
	}
}
