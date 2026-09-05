package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class MultipleDownloadItem extends DownloadItem {

	private final List<DownloadItem> items;

	public MultipleDownloadItem(@NonNull WorldRegion region,
	                            @NonNull List<DownloadItem> items,
	                            @NonNull DownloadActivityType type) {
		super(type);
		this.items = items;
	}

	public List<IndexItem> getAllIndexes() {
		List<IndexItem> indexes = new ArrayList<>();
		for (DownloadItem item : items) {
			IndexItem index = getIndexItem(item);
			if (index != null) {
				indexes.add(index);
			}
		}
		return indexes;
	}

	public List<DownloadItem> getAllItems() {
		return items;
	}

	@Override
	public boolean isOutdated() {
		for (DownloadItem item : items) {
			if (item.isOutdated()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDownloaded() {
		for (DownloadItem item : items) {
			if (item.isDownloaded()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDownloading(@NonNull DownloadIndexesThread thread) {
		for (DownloadItem item : items) {
			if (item.isDownloading(thread)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getFileName() {
		// The file name is used in many places.
		// But in the case of a Multiple Indexes element it's not use in most cases.
		// File is not created for Multiple Indexes element,
		// and all file names are available in internal IndexItem elements.

		// The only one place where a filename may be needed
		// is to generate the base and display names.
		// Since these names are generated based on the filename.
		// But now we don't need a name for display,
		// because on all screens where we now use multiple elements item,
		// for display used a type name instead of a file name.

		// Later, if you need a file name,
		// you can try to create it based on the WorldRegion
		// and file name of one of the internal IndexItem elements.
		return "";
	}

	@NonNull
	@Override
	public List<File> getDownloadedFiles(@NonNull OsmandApplication app) {
		List<File> result = new ArrayList<>();
		for (DownloadItem item : items) {
			result.addAll(item.getDownloadedFiles(app));
		}
		return result;
	}

	public List<DownloadItem> getItemsToDownload() {
		List<DownloadItem> itemsToDownload = new ArrayList<>();
		for (DownloadItem item : getAllItems()) {
			if (item.hasActualDataToDownload()) {
				itemsToDownload.add(item);
			}
		}
		return itemsToDownload;
	}

	@Override
	public boolean hasActualDataToDownload() {
		return getItemsToDownload().size() > 0;
	}

	@Override
	public boolean isFree() {
		for (DownloadItem item : items) {
			if (item.isFree()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getFreeMessage() {
		for (DownloadItem item : items) {
			String message = item.getFreeMessage();
			if (message != null) {
				return message;
			}
		}
		return null;
	}

	@Override
	public double getSizeToDownloadInMb() {
		double totalSizeMb = 0.0d;
		for (DownloadItem item : items) {
			if (item.hasActualDataToDownload()) {
				totalSizeMb += item.getSizeToDownloadInMb();
			}
		}
		return totalSizeMb;
	}

	@Override
	public double getArchiveSizeMB() {
		double result = 0.0d;
		for (DownloadItem item : items) {
			result += item.getArchiveSizeMB();
		}
		return result;
	}

	@Nullable
	public static IndexItem getIndexItem(@NonNull DownloadItem obj) {
		if (obj instanceof IndexItem) {
			return (IndexItem) obj;
		} else if (obj instanceof SrtmDownloadItem) {
			return ((SrtmDownloadItem) obj).getDefaultIndexItem();
		}
		return null;
	}

	@Nullable
	@Override
	public String getAdditionalDescription(Context ctx) {
		return null;
	}

	@Override
	public String getDate(@NonNull DateFormat dateFormat, boolean remote) {
		return "";
	}
}
