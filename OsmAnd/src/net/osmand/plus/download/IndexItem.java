package net.osmand.plus.download;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.support.annotation.NonNull;

public class IndexItem implements Comparable<IndexItem> {
	private static final Log log = PlatformUtil.getLog(IndexItem.class);
	
	String description;
	String fileName;
	String size;
	long timestamp;
	long contentSize;
	long containerSize;
	DownloadActivityType type;
	boolean extra;
	
	// Update information
	boolean outdated;
	boolean downloaded;
	long localTimestamp;
	DownloadResourceGroup relatedGroup;


	public IndexItem(String fileName, String description, long timestamp, String size, long contentSize,
			long containerSize, @NonNull DownloadActivityType tp) {
		this.fileName = fileName;
		this.description = description;
		this.timestamp = timestamp;
		this.size = size;
		this.contentSize = contentSize;
		this.containerSize = containerSize;
		this.type = tp;
	}

	public DownloadActivityType getType() {
		return type;
	}
	
	public void setRelatedGroup(DownloadResourceGroup relatedGroup) {
		this.relatedGroup = relatedGroup;
	}
	
	public DownloadResourceGroup getRelatedGroup() {
		return relatedGroup;
	}

	public String getFileName() {
		return fileName;
	}


	public String getDescription() {
		return description;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public long getSize(){
		return containerSize;
	}
	
	public long getContentSize() {
		return contentSize;
	}
	
	public double getContentSizeMB() {
		return ((double)contentSize) / (1 << 20);
	}
	
	public double getArchiveSizeMB() {
		return ((double)containerSize) / (1 << 20);
	}

	public String getSizeDescription(Context ctx) {
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, size, "MB");
	}
	

	public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
		String fileName = this.fileName;
		File parent = type.getDownloadFolder(ctx, this);
		boolean preventMediaIndexing = type.preventMediaIndexing(ctx, this);
		if (parent != null) {
			parent.mkdirs();
			// ".nomedia" indicates there are no pictures and no music to list in this dir for the Gallery and Music apps
			if (preventMediaIndexing) {
				try {
					new File(parent, ".nomedia").createNewFile();//$NON-NLS-1$	
				} catch (IOException e) {
					// swallow io exception
					log.error("IOException", e);
				}
			}
		}
		DownloadEntry entry;
		if (parent == null || !parent.exists()) {
			ctx.showToastMessage(R.string.sd_dir_not_accessible);
			entry = null;
		} else {
			entry = new DownloadEntry();
			entry.type = type;
			entry.baseName = getBasename();
			entry.urlToDownload = entry.type.getBaseUrl(ctx, fileName) + entry.type.getUrlSuffix(ctx);
			entry.zipStream = type.isZipStream(ctx, this);
			entry.unzipFolder = type.isZipFolder(ctx, this);
			entry.dateModified = timestamp; 
			entry.sizeMB = contentSize / (1024f*1024f);
			entry.targetFile = getTargetFile(ctx);
		}
		return entry;
	}

	public String getTargetFileName() {
		return type.getTargetFileName(this);
	}

	public String getBasename() {
		return type.getBasename(this);
	}
	
	public File getTargetFile(OsmandApplication ctx) {
		String basename;
		if (type == DownloadActivityType.HILLSHADE_FILE) {
			basename = (FileNameTranslationHelper.HILL_SHADE + getBasename()).replace("_", " ");
		} else {
			basename = getBasename();
		}
		return new File(type.getDownloadFolder(ctx, this), basename + type.getUnzipExtension(ctx, this));
	}

	public File getBackupFile(OsmandApplication ctx) {
		File backup = new File(ctx.getAppPath(IndexConstants.BACKUP_INDEX_DIR), getTargetFile(ctx).getName());
		return backup;
	}
	
	@Override
	public int compareTo(@NonNull IndexItem another) {
		return getFileName().compareTo(another.getFileName());
	}

	
	public String getDaysBehind(OsmandApplication app) {
		if (localTimestamp > 0) {
			long days = Math.max(1, (getTimestamp() - localTimestamp) / (24 * 60 * 60 * 1000) + 1);
			return days + " " + app.getString(R.string.days_behind);
		}
		return "";
	}
	
	public String getRemoteDate(DateFormat dateFormat) {
		if(timestamp <= 0) {
			return "";
		}
		return dateFormat.format(new Date(timestamp));
	}
	
	
	public String getLocalDate(DateFormat dateFormat) {
		if(localTimestamp <= 0) {
			return "";
		}
		return dateFormat.format(new Date(localTimestamp));
	}
	
	public boolean isOutdated() {
		return outdated && getType() != DownloadActivityType.HILLSHADE_FILE ;
	}
	
	public void setOutdated(boolean outdated) {
		this.outdated = outdated;
	}
	
	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}
	
	public void setLocalTimestamp(long localTimestamp) {
		this.localTimestamp = localTimestamp;
	}
	
	
	public boolean isDownloaded() {
		return downloaded;
	}

	public String getVisibleName(Context ctx, OsmandRegions osmandRegions) {
		return type.getVisibleName(this, ctx, osmandRegions, true);
	}

	public String getVisibleName(Context ctx, OsmandRegions osmandRegions, boolean includingParent) {
		return type.getVisibleName(this, ctx, osmandRegions, includingParent);
	}

	public String getVisibleDescription(OsmandApplication clctx) {
		return type.getVisibleDescription(this, clctx);
	}

	

	public String getDate(java.text.DateFormat format) {
		return format.format(new Date(timestamp));
	}
	
	public static class DownloadEntry {
		public long dateModified;
		public double sizeMB;
		
		public File targetFile;
		public boolean zipStream;
		public boolean unzipFolder;
		
		public File fileToDownload;
		
		public String baseName;
		public String urlToDownload;
		public boolean isAsset;
		public String assetName;
		public DownloadActivityType type;
		

		public DownloadEntry() {
		}
		
		public DownloadEntry(String assetName, String fileName, long dateModified) {
			this.dateModified = dateModified;
			targetFile = new File(fileName);
			this.assetName = assetName;
			isAsset = true;
		}
	}


}
