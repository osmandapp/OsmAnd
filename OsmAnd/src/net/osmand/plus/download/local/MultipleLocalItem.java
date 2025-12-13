package net.osmand.plus.download.local;

import android.content.Context;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

public class MultipleLocalItem extends BaseLocalItem {

	private final String id;
	private final String name;
	private final List<BaseLocalItem> items;
	private long totalSize;
	private long minTimestamp = Long.MAX_VALUE;
	private long maxTimestamp = Long.MIN_VALUE;

	public MultipleLocalItem(@NonNull String id,
	                         @NonNull String name,
	                         @NonNull LocalItemType type,
	                         @NonNull List<BaseLocalItem> items) {
		super(type);
		this.id = id;
		this.name = name;
		this.items = items;
		calculateStats();
	}

	private void calculateStats() {
		for (BaseLocalItem item : items) {
			// Calculate total size
			totalSize += item.getSize();

			// Calculate date range
			long lastModified = item.getLastModified();
			if (lastModified < minTimestamp) {
				minTimestamp = lastModified;
			}
			if (lastModified > maxTimestamp) {
				maxTimestamp = lastModified;
			}
		}
	}

	@NonNull
	public List<LocalItem> getLocalItems() {
		List<LocalItem> result = new ArrayList<>();
		for (BaseLocalItem item : getItems()) {
			if (item instanceof LocalItem localItem) {
				result.add(localItem);
			}
		}
		return result;
	}

	@NonNull
	public List<BaseLocalItem> getItems() {
		return items;
	}

	@NonNull
	public String getId() {
		return id;
	}

	@NonNull
	@Override
	public CharSequence getName(@NonNull Context context) {
		return LocalItemUtils.getItemName(context, this);
	}

	@NonNull
	public CharSequence getName() {
		return name;
	}

	@NonNull
	@Override
	public String getDescription(@NonNull Context context) {
		return LocalItemUtils.getItemDescription(context, this);
	}

	public long getSize() {
		return totalSize;
	}

	@Override
	public long getLastModified() {
		return 0;
	}

	public long getMinTimestamp() {
		return minTimestamp;
	}

	public long getMaxTimestamp() {
		return maxTimestamp;
	}

	@NonNull
	@Override
	public String toString() {
		return name + " " + items.toString();
	}

	public boolean isBackuped(@NonNull OsmandApplication app) {
		for (BaseLocalItem item : getItems()) {
			if (item instanceof LocalItem localItem) {
				return localItem.isBackuped(app);
			}
		}
		return false;
	}
}