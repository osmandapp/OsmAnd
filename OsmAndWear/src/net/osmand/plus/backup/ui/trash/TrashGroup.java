package net.osmand.plus.backup.ui.trash;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TrashGroup {

	private final String name;
	private final List<TrashItem> items = new ArrayList<>();

	public TrashGroup(@NonNull String name) {
		this.name = name;
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public List<TrashItem> getItems() {
		return items;
	}

	public void addItem(@NonNull TrashItem item) {
		items.add(item);
	}
}
