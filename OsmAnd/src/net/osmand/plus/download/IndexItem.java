package net.osmand.plus.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.text.format.DateFormat;

public class IndexItem implements Comparable<IndexItem> {
	private static final Log log = PlatformUtil.getLog(IndexItem.class);
	
	String description;
	String fileName;
	String size;
	long timestamp;
	long contentSize;
	long containerSize;
	IndexItem attachedItem;
	DownloadActivityType type;


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
	
	
	public String getSizeDescription(Context ctx) {
		return size + " MB";
	}
	

	public List<DownloadEntry> createDownloadEntry(OsmandApplication ctx, DownloadActivityType type, 
			List<DownloadEntry> downloadEntries) {
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
		final DownloadEntry entry;
		if (parent == null || !parent.exists()) {
			ctx.showToastMessage(R.string.sd_dir_not_accessible);
		} else {
			entry = new DownloadEntry(this);
			entry.type = type;
			entry.baseName = getBasename();
			entry.urlToDownload = entry.type.getBaseUrl(ctx, fileName) + entry.type.getUrlSuffix(ctx);
			entry.zipStream = type.isZipStream(ctx, this);
			entry.unzipFolder = type.isZipFolder(ctx, this);
			entry.dateModified = timestamp; 
			entry.sizeMB = contentSize / (1024f*1024f);
			String extension = type.getUnzipExtension(ctx, this);
			entry.targetFile = new File(parent, entry.baseName + extension);
			File backup = new File(ctx.getAppPath(IndexConstants.BACKUP_INDEX_DIR), entry.targetFile.getName());
			if (backup.exists()) {
				entry.existingBackupFile = backup;
			}
			if (attachedItem != null) {
				ArrayList<DownloadEntry> sz = new ArrayList<DownloadEntry>();
				attachedItem.createDownloadEntry(ctx, type, sz);
				if(sz.size() > 0) {
					entry.attachedEntry = sz.get(0);
				}
			}
			downloadEntries.add(entry);
		}
		return downloadEntries;
	}
	
	@Override
	public int compareTo(IndexItem another) {
		if(another == null) {
			return -1;
		}
		return getFileName().compareTo(another.getFileName());
	}

	public boolean isAlreadyDownloaded(Map<String, String> listAlreadyDownloaded) {
		return listAlreadyDownloaded.containsKey(getTargetFileName());
	}

	public String getBasename() {
		return type.getBasename(this);
	}

	public String getVisibleName(Context ctx, OsmandRegions osmandRegions) {
		return type.getVisibleName(this, ctx, osmandRegions);
	}

	public String getVisibleDescription(OsmandApplication clctx) {
		return type.getVisibleDescription(this, clctx);
	}

	public String getTargetFileName() {
		return type.getTargetFileName(this);
	}

	public String getDate(java.text.DateFormat format) {
		return format.format(new Date(timestamp));
	}

}