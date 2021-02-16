package net.osmand.plus.download;

import androidx.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultipleIndexItem extends DownloadItem {

	private final List<IndexItem> items;

	public MultipleIndexItem(@NonNull WorldRegion region,
	                         @NonNull List<IndexItem> items,
	                         @NonNull DownloadActivityType type) {
		super(type);
		this.items = items;
	}

	public List<IndexItem> getAllIndexes() {
		return items;
	}

	@Override
	public boolean isOutdated() {
		for (IndexItem item : items) {
			if (item.isOutdated()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDownloaded() {
		for (IndexItem item : items) {
			if (item.isDownloaded()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDownloading(@NonNull DownloadIndexesThread thread) {
		for (IndexItem item : items) {
			if (thread.isDownloading(item)) {
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
		for (IndexItem item : items) {
			result.addAll(item.getDownloadedFiles(app));
		}
		return result;
	}

	public List<IndexItem> getIndexesToDownload() {
		List<IndexItem> indexesToDownload = new ArrayList<>();
		for (IndexItem item : items) {
			if (item.hasActualDataToDownload()) {
				indexesToDownload.add(item);
			}
		}
		return indexesToDownload;
	}

	@Override
	public boolean hasActualDataToDownload() {
		return getIndexesToDownload().size() > 0;
	}

	@Override
	public double getSizeToDownloadInMb() {
		double totalSizeMb = 0.0d;
		for (IndexItem item : items) {
			if (item.hasActualDataToDownload()) {
				totalSizeMb += item.getSizeToDownloadInMb();
			}
		}
		return totalSizeMb;
	}

	@Override
	public double getArchiveSizeMB() {
		double result = 0.0d;
		for (IndexItem item : items) {
			result += item.getArchiveSizeMB();
		}
		return result;
	}

}
