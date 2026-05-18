package net.osmand.plus.plugins.custom;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.ui.DownloadDescriptionInfo;
import net.osmand.plus.utils.JsonUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class CustomIndexItem extends IndexItem {

	private final String subfolder;
	private final String downloadUrl;

	private final Map<String, String> names;
	private final Map<String, String> firstSubNames;
	private final Map<String, String> secondSubNames;

	private final DownloadDescriptionInfo descriptionInfo;

	public CustomIndexItem(String fileName,
	                       String subfolder,
	                       String downloadUrl,
	                       String size,
	                       long timestamp,
	                       long contentSize,
	                       long containerSize,
	                       Map<String, String> names,
	                       Map<String, String> firstSubNames,
	                       Map<String, String> secondSubNames,
	                       @NonNull DownloadActivityType type,
	                       DownloadDescriptionInfo descriptionInfo,
	                       boolean isHidden) {
		super(fileName, null, timestamp, size, contentSize, containerSize, type, false, null, isHidden);
		this.names = names;
		this.firstSubNames = firstSubNames;
		this.secondSubNames = secondSubNames;
		this.subfolder = subfolder;
		this.downloadUrl = downloadUrl;
		this.descriptionInfo = descriptionInfo;
	}

	@Override
	public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
		DownloadEntry entry = super.createDownloadEntry(ctx);
		if (entry != null) {
			entry.urlToDownload = downloadUrl;
			entry.zipStream = fileName.endsWith(".zip");
		}
		return entry;
	}

	@Override
	@NonNull
	public File getTargetFile(@NonNull OsmandApplication ctx) {
		String basename = getTranslatedBasename();
		if (!Algorithms.isEmpty(subfolder)) {
			basename = subfolder + "/" + basename;
		}
		return new File(type.getDownloadFolder(ctx, this), basename + type.getUnzipExtension(ctx, this));
	}

	@Override
	public String getVisibleName(@NonNull Context ctx, @NonNull OsmandRegions regions, boolean includingParent, boolean useShortName) {
		String name = super.getVisibleName(ctx, regions, includingParent, useShortName);
		return JsonUtils.getLocalizedResFromMap(ctx, names, name);
	}

	public String getSubName(Context ctx) {
		String subName = getFirstSubName(ctx);

		String secondSubName = getSecondSubName(ctx);
		if (secondSubName != null) {
			subName = subName == null ? secondSubName : subName + " • " + secondSubName;
		}
		return subName;
	}

	public String getFirstSubName(Context ctx) {
		return JsonUtils.getLocalizedResFromMap(ctx, firstSubNames, null);
	}

	public String getSecondSubName(Context ctx) {
		return JsonUtils.getLocalizedResFromMap(ctx, secondSubNames, null);
	}

	public DownloadDescriptionInfo getDescriptionInfo() {
		return descriptionInfo;
	}

	public static class CustomIndexItemBuilder {

		private String fileName;
		private String subfolder;
		private String downloadUrl;
		private String size;

		private long timestamp;
		private long contentSize;
		private long containerSize;

		private Map<String, String> names;
		private Map<String, String> firstSubNames;
		private Map<String, String> secondSubNames;
		private DownloadActivityType type;
		private boolean hidden;

		private DownloadDescriptionInfo descriptionInfo;

		public CustomIndexItemBuilder setFileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public CustomIndexItemBuilder setSubfolder(String subfolder) {
			this.subfolder = subfolder;
			return this;
		}

		public CustomIndexItemBuilder setDownloadUrl(String downloadUrl) {
			this.downloadUrl = downloadUrl;
			return this;
		}

		public CustomIndexItemBuilder setSize(String size) {
			this.size = size;
			return this;
		}

		public CustomIndexItemBuilder setTimestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public CustomIndexItemBuilder setContentSize(long contentSize) {
			this.contentSize = contentSize;
			return this;
		}

		public CustomIndexItemBuilder setContainerSize(long containerSize) {
			this.containerSize = containerSize;
			return this;
		}

		public CustomIndexItemBuilder setNames(Map<String, String> names) {
			this.names = names;
			return this;
		}

		public CustomIndexItemBuilder setFirstSubNames(Map<String, String> firstSubNames) {
			this.firstSubNames = firstSubNames;
			return this;
		}

		public CustomIndexItemBuilder setSecondSubNames(Map<String, String> secondSubNames) {
			this.secondSubNames = secondSubNames;
			return this;
		}

		public CustomIndexItemBuilder setDescriptionInfo(DownloadDescriptionInfo descriptionInfo) {
			this.descriptionInfo = descriptionInfo;
			return this;
		}

		public CustomIndexItemBuilder setType(@NonNull DownloadActivityType type) {
			this.type = type;
			return this;
		}

		public CustomIndexItemBuilder setIsHidden(boolean isHidden) {
			this.hidden = isHidden;
			return this;
		}

		public CustomIndexItem create() {
			return new CustomIndexItem(fileName,
					subfolder,
					downloadUrl,
					size,
					timestamp,
					contentSize,
					containerSize,
					names,
					firstSubNames,
					secondSubNames,
					type,
					descriptionInfo,
					hidden);
		}
	}
}