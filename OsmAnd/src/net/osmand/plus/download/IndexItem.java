package net.osmand.plus.download;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

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


	public IndexItem(String fileName, String description, long timestamp, String size, long contentSize,
			long containerSize, DownloadActivityType tp) {
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

	public String getSizeDescription(Context ctx) {
		return size + " MB";
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
	
	private File getTargetFile(OsmandApplication ctx) {
		return new File(type.getDownloadFolder(ctx, this), getBasename() + type.getUnzipExtension(ctx, this));
	}

	public File getBackupFile(OsmandApplication ctx) {
		File backup = new File(ctx.getAppPath(IndexConstants.BACKUP_INDEX_DIR), getTargetFile(ctx).getName());
		return backup;
	}
	
	@Override
	public int compareTo(@NonNull IndexItem another) {
		return getFileName().compareTo(another.getFileName());
	}

	
	public boolean isOutdated() {
		FIXME;
	}
	
	
	public boolean isDownloaded() {
//		return listAlreadyDownloaded.containsKey(getTargetFileName());
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