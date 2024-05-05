package net.osmand.plus.download.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalGroup {

	private final LocalItemType type;
	private final Map<String, BaseLocalItem> items = new HashMap<>();

	public LocalGroup(@NonNull LocalItemType type) {
		this.type = type;
	}

	@NonNull
	public LocalItemType getType() {
		return type;
	}

	@Nullable
	public BaseLocalItem getItem(@NonNull String key) {
		return items.get(key);
	}

	@NonNull
	public List<BaseLocalItem> getItems() {
		return new ArrayList<>(items.values());
	}

	@NonNull
	public String getName(@NonNull Context context) {
		return type.toHumanString(context);
	}

	public void addItem(@NonNull String key, @NonNull BaseLocalItem item) {
		items.put(key, item);
	}

	public void removeItem(@NonNull OsmandApplication app,  @NonNull BaseLocalItem item) {
		if (item instanceof LocalItem) {
			items.remove(((LocalItem) item).getFileName());
		} else {
			items.remove(item.getName(app).toString());
		}
	}

	public long getSize() {
		long size = 0;
		for (BaseLocalItem item : items.values()) {
			size += item.getSize();
		}
		return size;
	}
}
