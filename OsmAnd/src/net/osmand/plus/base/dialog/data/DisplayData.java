package net.osmand.plus.base.dialog.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayData {

	private final List<DisplayItem> items = new ArrayList<>();
	private final Map<DialogExtra, Object> extras = new HashMap<>();

	@NonNull
	public List<DisplayItem> getDisplayItems() {
		return items;
	}

	public void addDisplayItem(@NonNull DisplayItem displayItem) {
		items.add(displayItem);
	}

	@Nullable
	public Object getExtra(@NonNull DialogExtra parameterKey) {
		return extras.get(parameterKey);
	}

	public void putExtra(@NonNull DialogExtra parameterKey, @Nullable Object value) {
		extras.put(parameterKey, value);
	}

	public void clear() {
		items.clear();
		extras.clear();
	}
}
