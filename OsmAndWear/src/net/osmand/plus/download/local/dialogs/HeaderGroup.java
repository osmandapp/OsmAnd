package net.osmand.plus.download.local.dialogs;

import androidx.annotation.NonNull;

import net.osmand.plus.download.local.BaseLocalItem;

import java.util.ArrayList;
import java.util.List;

public class HeaderGroup {

	private final String name;
	private final List<BaseLocalItem> items = new ArrayList<>();

	public HeaderGroup(@NonNull String name, @NonNull List<BaseLocalItem> items) {
		this.name = name;
		this.items.addAll(items);
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public List<BaseLocalItem> getItems() {
		return items;
	}
}
