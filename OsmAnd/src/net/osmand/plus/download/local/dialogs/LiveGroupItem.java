package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.LocalItemType.LIVE_UPDATES;
import static net.osmand.plus.download.local.LocalItemUtils.getFormattedDate;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LiveGroupItem extends BaseLocalItem {

	private final String name;
	private final List<LocalItem> items = new ArrayList<>();

	public LiveGroupItem(@NonNull String name) {
		super(LIVE_UPDATES);
		this.name = name;
	}

	@NonNull
	public List<LocalItem> getItems() {
		return items;
	}

	public void addLocalItem(@NonNull LocalItem item) {
		items.add(item);
	}

	@NonNull
	@Override
	public LocalItemType getType() {
		return LIVE_UPDATES;
	}

	@NonNull
	@Override
	public CharSequence getName(@NonNull Context context) {
		return name;
	}

	@NonNull
	@Override
	public String getDescription(@NonNull Context context) {
		String formattedDate = getFormattedDate(new Date(getLastModified()));
		String size = AndroidUtils.formatSize(context, getSize());
		return context.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, formattedDate);
	}

	@Override
	public long getSize() {
		long totalSize = 0;
		for (LocalItem item : items) {
			totalSize += item.getSize();
		}
		return totalSize;
	}

	public long getLastModified() {
		long lastModified = 0;
		for (LocalItem item : items) {
			if (item.getLastModified() > lastModified) {
				lastModified = item.getLastModified();
			}
		}
		return lastModified;
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}
}