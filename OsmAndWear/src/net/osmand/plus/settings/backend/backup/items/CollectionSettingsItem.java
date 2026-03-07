package net.osmand.plus.settings.backend.backup.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class CollectionSettingsItem<T> extends SettingsItem {

	protected List<T> items;
	protected List<T> appliedItems;
	protected List<T> duplicateItems;
	protected List<T> existingItems;

	@Override
	protected void init() {
		super.init();
		items = new ArrayList<>();
		appliedItems = new ArrayList<>();
		duplicateItems = new ArrayList<>();
	}

	public CollectionSettingsItem(@NonNull OsmandApplication app, @Nullable CollectionSettingsItem<T> baseItem, @NonNull List<T> items) {
		super(app, baseItem);
		this.items = items;
	}

	public CollectionSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@NonNull
	public List<T> getItems() {
		return items;
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	@NonNull
	public List<T> getAppliedItems() {
		return appliedItems;
	}

	@NonNull
	public List<T> getDuplicateItems() {
		return duplicateItems;
	}

	@NonNull
	public List<T> processDuplicateItems() {
		if (!items.isEmpty()) {
			for (T item : items) {
				if (isDuplicate(item)) {
					duplicateItems.add(item);
				}
			}
		}
		return duplicateItems;
	}

	@Override
	public void delete() {
		super.delete();
		if (!Algorithms.isEmpty(items)) {
			for (T item : items) {
				deleteItem(item);
			}
		}
	}

	protected abstract void deleteItem(T item);

	public List<T> getNewItems() {
		List<T> res = new ArrayList<>(items);
		res.removeAll(duplicateItems);
		return res;
	}

	@Override
	public long getEstimatedSize() {
		long size = 0;
		for (T item : items) {
			size += getEstimatedItemSize(item);
		}
		return size;
	}

	public boolean shouldShowDuplicates() {
		return true;
	}

	@Override
	public boolean shouldReadOnCollecting() {
		return true;
	}

	public abstract boolean isDuplicate(@NonNull T item);

	@NonNull
	public abstract T renameItem(@NonNull T item);

	public abstract long getEstimatedItemSize(@NonNull T item);
}
