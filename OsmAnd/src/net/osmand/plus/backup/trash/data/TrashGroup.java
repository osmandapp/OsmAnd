package net.osmand.plus.backup.trash.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TrashGroup {

	private final String formattedDate;
	private final long dateTime;
	private final List<TrashItem> trashItems = new ArrayList<>();

	public TrashGroup(@NonNull String formattedDate, long dateTime) {
		this.formattedDate = formattedDate;
		this.dateTime = dateTime;
	}

	@NonNull
	public String getFormattedDate() {
		return formattedDate;
	}

	public void addTrashItem(@NonNull TrashItem trashItem) {
		trashItems.add(trashItem);
	}

	@NonNull
	public List<TrashItem> getTrashItems() {
		return trashItems;
	}
}
