package net.osmand.plus.download.local;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class LocalCategory {

	private final CategoryType type;
	private final Map<LocalItemType, LocalGroup> groups = new HashMap<>();

	public LocalCategory(@NonNull CategoryType type) {
		this.type = type;
	}

	@NonNull
	public CategoryType getType() {
		return type;
	}

	@NonNull
	public Map<LocalItemType, LocalGroup> getGroups() {
		return groups;
	}

	@NonNull
	public String getName(@NonNull Context context) {
		return context.getString(type.getTitleId());
	}

	public void addLocalItem(@NonNull LocalItem item) {
		LocalItemType itemType = item.getType();

		LocalGroup group = groups.get(itemType);
		if (group == null) {
			group = new LocalGroup(itemType);
			groups.put(itemType, group);
		}
		group.addItem(item.getFileName(), item);
	}

	public long getSize() {
		long size = 0;
		for (LocalGroup group : groups.values()) {
			size += group.getSize();
		}
		return size;
	}
}
