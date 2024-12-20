package net.osmand.plus.base.dialog.data;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.CONTROLS_COLOR;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.ColorUtilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayData {

	private final List<DisplayItem> items = new ArrayList<>();
	private final Map<DialogExtra, Object> extras = new HashMap<>();

	@NonNull
	public DisplayItem getItemAt(int position) {
		return items.get(position);
	}

	public int getItemsSize() {
		return items.size();
	}

	@NonNull
	public List<DisplayItem> getDisplayItems() {
		return items;
	}

	public void addDisplayItem(@NonNull DisplayItem displayItem) {
		items.add(displayItem);
	}

	public void addAllDisplayItems(@NonNull Collection<DisplayItem> displayItems) {
		items.addAll(displayItems);
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

	@ColorInt
	public int getControlsColor(@NonNull Context context, @NonNull DisplayItem item, boolean nightMode) {
		Integer color = item.getControlsColor();
		if (color == null) {
			color = (Integer) getExtra(CONTROLS_COLOR);
		}
		if (color == null) {
			color = ColorUtilities.getActiveColor(context, nightMode);
		}
		return color;
	}

	@ColorInt
	@Nullable
	public Integer getBackgroundColor(@NonNull DisplayItem item) {
		Integer color = item.getBackgroundColor();
		if (color == null) {
			color = (Integer) getExtra(BACKGROUND_COLOR);
		}
		return color;
	}
}
