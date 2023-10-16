package net.osmand.plus.backup.ui.trash;

import static net.osmand.plus.backup.ui.trash.CloudTrashController.GROUP_DATE_PATTERN;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrashGroup {

	private final long time;
	private final List<TrashItem> items = new ArrayList<>();

	public TrashGroup(long time) {
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	@NonNull
	public String getName() {
		DateFormat format = new SimpleDateFormat(GROUP_DATE_PATTERN, Locale.getDefault());
		return Algorithms.capitalizeFirstLetter(format.format(time));
	}

	@NonNull
	public List<TrashItem> getItems() {
		return items;
	}

	public void addItem(@NonNull TrashItem item) {
		items.add(item);
	}
}
