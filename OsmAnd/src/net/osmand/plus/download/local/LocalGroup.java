package net.osmand.plus.download.local;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class LocalGroup {

	private final LocalItemType type;
	private final List<LocalItem> items = new ArrayList<>();

	public LocalGroup(@NonNull LocalItemType type) {
		this.type = type;
	}

	@NonNull
	public LocalItemType getType() {
		return type;
	}

	@NonNull
	public List<LocalItem> getItems() {
		return items;
	}

	@NonNull
	public String getName(@NonNull Context context) {
		return type.toHumanString(context);
	}

	public long getSize() {
		long size = 0;
		for (LocalItem item : items) {
			size += item.getSize();
		}
		return size;
	}
}
