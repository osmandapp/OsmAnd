package net.osmand.plus.download;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;

public class CustomIndexItem extends IndexItem {

	private String downloadurl;
	private String subfolder;

	public CustomIndexItem(String fileName, String subfolder, String description, String downloadurl,
	                       long dateModified, String size, long contentSize,
	                       long containerSize, DownloadActivityType type) {
		super(fileName, description, dateModified, size, contentSize, containerSize, type);
		this.subfolder = subfolder;
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

	@Override
	public File getTargetFile(OsmandApplication ctx) {
		String basename = getTranslatedBasename();
		if (!Algorithms.isEmpty(subfolder)) {
			basename = subfolder + "/" + basename;
		}
		return new File(type.getDownloadFolder(ctx, this), basename + type.getUnzipExtension(ctx, this));
	}
}
