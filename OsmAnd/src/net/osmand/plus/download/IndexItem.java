package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class IndexItem extends DownloadItem implements Comparable<IndexItem> {

	private static final Log log = PlatformUtil.getLog(IndexItem.class);

	protected String description;
	protected String fileName;
	protected String size;
	protected long timestamp;
	protected long contentSize;
	protected long containerSize;
	protected boolean extra;
	protected boolean hidden;

	// Update information
	boolean outdated;
	boolean downloaded;
	long localTimestamp;
	boolean free;
	String freeMessage;

	public IndexItem(String fileName,
	                 String description,
	                 long timestamp,
	                 String size,
	                 long contentSize,
	                 long containerSize,
	                 @NonNull DownloadActivityType type,
	                 boolean free,
	                 String freeMessage) {
		this(fileName,
				description,
				timestamp,
				size,
				contentSize,
				containerSize,
				type,
				free,
				freeMessage, false);
	}

	public IndexItem(String fileName,
	                 String description,
	                 long timestamp,
	                 String size,
	                 long contentSize,
	                 long containerSize,
	                 @NonNull DownloadActivityType type,
	                 boolean free,
	                 String freeMessage,
	                 boolean hidden) {
		super(type);
		this.fileName = fileName;
		this.description = description;
		this.timestamp = timestamp;
		this.size = size;
		this.contentSize = contentSize;
		this.containerSize = containerSize;
		this.free = free;
		this.freeMessage = freeMessage;
		this.hidden = hidden;
	}

	public void updateSize(@NonNull String size, long contentSize, long containerSize) {
		this.size = size;
		this.contentSize = contentSize;
		this.containerSize = containerSize;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@NonNull
	@Override
	public List<File> getDownloadedFiles(@NonNull OsmandApplication app) {
		File targetFile = getTargetFile(app);
		if (targetFile.exists()) {
			return Collections.singletonList(targetFile);
		}
		return Collections.emptyList();
	}

	public String getDescription() {
		return description;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getSize() {
		return containerSize;
	}

	public long getContentSize() {
		return contentSize;
	}

	public boolean isHidden() {
		return hidden;
	}

	public double getContentSizeMB() {
		return ((double) contentSize) / (1 << 20);
	}

	public double getArchiveSizeMB() {
		return ((double) containerSize) / (1 << 20);
	}

	@Override
	public double getSizeToDownloadInMb() {
		return Algorithms.parseDoubleSilently(size, 0.0);
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
			entry.zipStream = type.isZipStream();
			entry.unzipFolder = type.isZipFolder();
			entry.dateModified = timestamp;
			entry.sizeMB = contentSize / (1024f * 1024f);
			entry.targetFile = getTargetFile(ctx);
		}
		return entry;
	}

	@Nullable
	public LocalItem toLocalItem(@NonNull OsmandApplication app) {
		File file = getExistedFile(app);
		if (file != null) {
			LocalItemType type = LocalItemUtils.getItemType(app, file);
			if (type != null) {
				LocalItem localItem = new LocalItem(file, type);
				LocalItemUtils.updateItem(app, localItem);
				return localItem;
			}
		}
		return null;
	}

	@Nullable
	public File getExistedFile(@NonNull OsmandApplication ctx) {
		File file = getTargetFile(ctx);
		if (!file.exists()) {
			file = getBackupFile(ctx);
		}
		return file.exists() ? file : null;
	}

	public String getTargetFileName() {
		return type.getTargetFileName(this);
	}

	@NonNull
	public File getTargetFile(@NonNull OsmandApplication ctx) {
		String basename = getTranslatedBasename();
		return new File(type.getDownloadFolder(ctx, this), basename + type.getUnzipExtension(ctx, this));
	}

	@NonNull
	public File getDefaultTargetFile(@NonNull OsmandApplication ctx) {
		String basename = getTranslatedBasename();
		return new File(type.getDefaultDownloadFolder(ctx, this), basename + type.getUnzipExtension(ctx, this));
	}

	public String getTranslatedBasename() {
		if (type == DownloadActivityType.HILLSHADE_FILE) {
			return (FileNameTranslationHelper.HILL_SHADE + "_" + getBasename()).replace("_", " ");
		} else if (type == DownloadActivityType.SLOPE_FILE) {
			return (FileNameTranslationHelper.SLOPE + "_" + getBasename()).replace('_', ' ');
		} else if (type == DownloadActivityType.GEOTIFF_FILE) {
			return (FileNameTranslationHelper.HEIGHTMAP + "_" + getBasename()).replace('_', ' ');
		} else {
			return getBasename();
		}
	}

	public File getBackupFile(OsmandApplication ctx) {
		return new File(ctx.getAppPath(IndexConstants.BACKUP_INDEX_DIR), getTargetFile(ctx).getName());
	}

	@Override
	public int compareTo(@NonNull IndexItem another) {
		return getFileName().compareTo(another.getFileName());
	}

	public String getDate(@NonNull DateFormat dateFormat, boolean remote) {
		return remote ? getRemoteDate(dateFormat) : getLocalDate(dateFormat);
	}

	public String getRemoteDate(DateFormat dateFormat) {
		if (timestamp <= 0) {
			return "";
		}
		return dateFormat.format(new Date(timestamp));
	}

	private String getLocalDate(@NonNull DateFormat dateFormat) {
		if (localTimestamp <= 0) {
			return "";
		}
		return dateFormat.format(new Date(localTimestamp));
	}

	public boolean isOutdated() {
		return outdated
				&& getType() != DownloadActivityType.HILLSHADE_FILE
				&& getType() != DownloadActivityType.SLOPE_FILE
				&& getType() != DownloadActivityType.GEOTIFF_FILE;
	}

	public void setOutdated(boolean outdated) {
		this.outdated = outdated;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}

	@Override
	public boolean hasActualDataToDownload() {
		return !isDownloaded() || isOutdated();
	}

	public void setLocalTimestamp(long localTimestamp) {
		this.localTimestamp = localTimestamp;
	}

	public long getLocalTimestamp() {
		return localTimestamp;
	}

	public boolean isFree() {
		return free;
	}

	@Nullable
	public String getFreeMessage() {
		return freeMessage;
	}

	public boolean isDownloaded() {
		return downloaded;
	}

	@Override
	public boolean isDownloading(@NonNull DownloadIndexesThread thread) {
		return thread.isDownloading(this);
	}

	public String getDate(java.text.DateFormat format) {
		return format.format(new Date(timestamp));
	}

	@Nullable
	@Override
	public String getAdditionalDescription(Context ctx) {
		if (getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
			return SrtmDownloadItem.getAbbreviationInScopes(ctx, this);
		}
		return null;
	}

	public long getExistingFileSize(@NonNull OsmandApplication ctx) {
		File file = getTargetFile(ctx);
		if (file.canRead()) {
			return file.length();
		}
		return 0;
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