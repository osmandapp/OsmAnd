package net.osmand.plus.download.local.dialogs;

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

	public String name;
	public List<LocalItem> localItems = new ArrayList<>();

	public LiveGroupItem(String name){
		super(LocalItemType.LIVE_UPDATES);
		this.name = name;
	}

	public void addLocalItem(LocalItem localItem){
		localItems.add(localItem);
	}

	@Override
	public LocalItemType getLocalItemType() {
		return LocalItemType.LIVE_UPDATES;
	}

	@Override
	public long getLocalItemCreated() {
		return localItems.isEmpty() ? 0 : localItems.get(0).getLastModified();
	}

	@Override
	public long getLocalItemSize() {
		long totalSize = 0;
		for(LocalItem item : localItems){
			totalSize += item.getSize();
		}
		return totalSize;
	}

	@Override
	public CharSequence getName(@NonNull Context context) {
		return name;
	}

	@Override
	public String getDescription(Context context) {
		String formattedDate = getFormattedDate(new Date(getLocalItemCreated()));
		String size = AndroidUtils.formatSize(context, getLocalItemSize());
		return context.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, formattedDate);
	}
}