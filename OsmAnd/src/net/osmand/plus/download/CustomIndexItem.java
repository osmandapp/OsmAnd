package net.osmand.plus.download;

import net.osmand.plus.OsmandApplication;

public class CustomIndexItem extends IndexItem {

	private final String downloadurl;

	public CustomIndexItem(String fileName, String description, String downloadurl,
	                       long dateModified, String size, long contentSize,
	                       long containerSize, DownloadActivityType type) {
		super(fileName, description, dateModified, size, contentSize, containerSize, type);
		this.downloadurl = downloadurl;
	}

	@Override
	public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
		DownloadEntry entry = super.createDownloadEntry(ctx);
		if (entry != null) {
			entry.urlToDownload = downloadurl;
		}

		return entry;
	}
}
