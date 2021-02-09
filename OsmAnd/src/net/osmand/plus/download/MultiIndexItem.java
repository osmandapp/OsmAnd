package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

public class MultiIndexItem implements DisplayItem {

	private List<IndexItem> items;
	private DownloadActivityType type;
	private double sizeMb;

	// Update information
	DownloadResourceGroup relatedGroup;

	public MultiIndexItem(@NonNull List<IndexItem> items,
	                      @NonNull DownloadActivityType tp) {
		this.type = tp;
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
	public double getArchiveSizeMB() {
		double result = 0.0d;
		for (IndexItem item : items) {
			result += item.getArchiveSizeMB();
		}
		return result;
	}

	@Override
	public double getContentSizeMB() {
		double result = 0.0d;
		for (IndexItem item : items) {
			result += item.getContentSizeMB();
		}
		return result;
	}

	@Override
	public DownloadActivityType getType() {
		return type;
	}

	public String getBasename() {
		return "";
	}

	@Override
	public boolean isDownloading(DownloadIndexesThread thread) {
		for (IndexItem item : items) {
			if (thread.isDownloading(item)) {
				return true;
			}
		}
		return false;
	}

	public List<IndexItem> getIndexesToDownload() {
		List<IndexItem> indexesToDownload = new ArrayList<>();
		for (IndexItem item : items) {
			if (!item.isDownloaded() || item.isOutdated()) {
				indexesToDownload.add(item);
			}
		}
		return indexesToDownload;
	}

	public boolean hasMapsToDownload() {
		return getIndexesToDownload().size() > 0;
	}

	public String getSizeToDownloadDescription(Context ctx) {
		double totalSizeMb = 0.0d;
		for (IndexItem item : items) {
			if (!item.isDownloaded() || item.isOutdated()) {
				totalSizeMb += Double.parseDouble(item.size);
			}
		}
		this.sizeMb = totalSizeMb;
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, getSizeInMB(sizeMb), "MB");
	}

	private String getSizeInMB(double sizeMb) {
		return String.format("%.2f", sizeMb);
	}

	@Override
	public void setRelatedGroup(DownloadResourceGroup relatedGroup) {
		this.relatedGroup = relatedGroup;
	}

	@Override
	public DownloadResourceGroup getRelatedGroup() {
		return relatedGroup;
	}

}
