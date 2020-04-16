package net.osmand.plus.download;

import android.content.Context;
import android.content.res.Configuration;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class CustomIndexItem extends IndexItem {

	private String downloadUrl;
	private String subfolder;
	private Map<String, String> names;

	public CustomIndexItem(String fileName, String subfolder, String description, String downloadUrl,
	                       Map<String, String> names, long dateModified, String size, long contentSize,
	                       long containerSize, DownloadActivityType type) {
		super(fileName, description, dateModified, size, contentSize, containerSize, type);
		this.names = names;
		this.subfolder = subfolder;
		this.downloadUrl = downloadUrl;
	}

	@Override
	public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
		DownloadEntry entry = super.createDownloadEntry(ctx);
		if (entry != null) {
			entry.urlToDownload = downloadUrl;
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

	@Override
	public String getVisibleName(Context ctx, OsmandRegions osmandRegions, boolean includingParent) {
		if (!Algorithms.isEmpty(names)) {
			Configuration config = ctx.getResources().getConfiguration();
			String lang = config.locale.getLanguage();
			String name = names.get(lang);
			if (Algorithms.isEmpty(name)) {
				name = names.get("");
			}
			if (!Algorithms.isEmpty(name)) {
				return name;
			}
		}

		return super.getVisibleName(ctx, osmandRegions, includingParent);
	}
}
