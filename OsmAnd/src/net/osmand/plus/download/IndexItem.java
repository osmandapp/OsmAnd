package net.osmand.plus.download;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.HasName;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class IndexItem implements Comparable<IndexItem>, HasName, Parcelable {
	private static final Log log = PlatformUtil.getLog(IndexItem.class);
	
	String description;
	String fileName;
	String simplifiedFileName;
	String size;
	long timestamp;
	long contentSize;
	long containerSize;
	DownloadActivityType type;
	boolean extra;

	private String initializedName;

	public IndexItem(String fileName, String description, long timestamp, String size, long contentSize,
			long containerSize, DownloadActivityType tp) {
		this.fileName = fileName;
		this.simplifiedFileName = fileName.toLowerCase().replace("_2.", ".").replace("hillshade_", "");
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

	public String getSimplifiedFileName() {
		return simplifiedFileName;
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
			downloadEntries.add(entry);
		}
		return downloadEntries;
	}
	
	@Override
	public int compareTo(@NonNull IndexItem another) {
//		if(another == null) {
//			return -1;
//		}
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

	@Override
	public String getName() {
		return initializedName + " must be fixed";
	}

	public void setName(String initializedName) {
		this.initializedName = initializedName;
	}

	@Override
	public String toString() {
		return "IndexItem{" +
				"description='" + description + '\'' +
				", fileName='" + fileName + '\'' +
				", simplifiedFileName='" + simplifiedFileName + '\'' +
				", size='" + size + '\'' +
				", timestamp=" + timestamp +
				", contentSize=" + contentSize +
				", containerSize=" + containerSize +
				", type=" + type.getTag() +
				", extra=" + extra +
				'}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.description);
		dest.writeString(this.fileName);
		dest.writeString(this.size);
		dest.writeLong(this.timestamp);
		dest.writeLong(this.contentSize);
		dest.writeLong(this.containerSize);
		dest.writeParcelable(this.type, flags);
		dest.writeByte(extra ? (byte) 1 : (byte) 0);
		dest.writeString(this.initializedName);
		dest.writeString(this.simplifiedFileName);
	}

	protected IndexItem(Parcel in) {
		this.description = in.readString();
		this.fileName = in.readString();
		this.size = in.readString();
		this.timestamp = in.readLong();
		this.contentSize = in.readLong();
		this.containerSize = in.readLong();
		this.type = in.readParcelable(DownloadActivityType.class.getClassLoader());
		this.extra = in.readByte() != 0;
		this.initializedName = in.readString();
		this.simplifiedFileName = in.readString();
	}

	public static final Parcelable.Creator<IndexItem> CREATOR = new Parcelable.Creator<IndexItem>() {
		public IndexItem createFromParcel(Parcel source) {
			return new IndexItem(source);
		}

		public IndexItem[] newArray(int size) {
			return new IndexItem[size];
		}
	};
}