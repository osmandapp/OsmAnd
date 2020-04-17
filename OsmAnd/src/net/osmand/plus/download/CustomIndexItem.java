package net.osmand.plus.download;

import android.content.Context;
import android.content.res.Configuration;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CustomIndexItem extends IndexItem {

	private String downloadUrl;
	private String subfolder;
	private Map<String, String> names;
	private Map<String, String> descriptions;
	private List<String> descrImageUrl;

	public CustomIndexItem(String fileName, String subfolder, String downloadUrl,
	                       Map<String, String> names, Map<String, String> descriptions, List<String> descrImageUrl, long dateModified, String size, long contentSize,
	                       long containerSize, DownloadActivityType type) {
		super(fileName, null, dateModified, size, contentSize, containerSize, type);
		this.names = names;
		this.descriptions = descriptions;
		this.subfolder = subfolder;
		this.downloadUrl = downloadUrl;
		this.descrImageUrl = descrImageUrl;
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
	public String getVisibleName(Context ctx, OsmandRegions osmandRegions) {
		return getVisibleName(ctx, osmandRegions, true);
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

	public List<String> getDescriptionImageUrl() {
		return descrImageUrl;
	}

	public String getLocalizedDescription(Context ctx) {
		if (!Algorithms.isEmpty(descriptions)) {
			Configuration config = ctx.getResources().getConfiguration();
			String lang = config.locale.getLanguage();
			String name = descriptions.get(lang);
			if (Algorithms.isEmpty(name)) {
				name = descriptions.get("");
			}
			if (!Algorithms.isEmpty(name)) {
				return name;
			}
		}

		return super.getDescription();
	}
}
