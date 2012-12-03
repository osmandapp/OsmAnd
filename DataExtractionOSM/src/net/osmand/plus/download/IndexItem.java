package net.osmand.plus.download;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;

import net.osmand.LogUtil;
import net.osmand.data.IndexConstants;
import net.osmand.plus.ClientContext;
import net.osmand.plus.R;
import static net.osmand.data.IndexConstants.*;

public class IndexItem {
	private static final Log log = LogUtil.getLog(IndexItem.class);
	
	String description;
	String date;
	String parts;
	String fileName;
	String size;
	IndexItem attachedItem;
	DownloadActivityType type;

	public IndexItem(String fileName, String description, String date, String size, String parts) {
		this.fileName = fileName;
		this.description = description;
		this.date = date;
		this.size = size;
		this.parts = parts;
		this.type = DownloadActivityType.NORMAL_FILE;
	}

	public DownloadActivityType getType() {
		return type;
	}

	public void setType(DownloadActivityType type) {
		this.type = type;
	}

	public String getVisibleDescription(ClientContext ctx, DownloadActivityType type) {
		String s = ""; //$NON-NLS-1$
		if (type == DownloadActivityType.ROADS_FILE) {
			return ctx.getString(R.string.download_roads_only_item);
		}
		if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
		} else if (fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)) {
			s = ctx.getString(R.string.voice);
		} else if (fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
			s = ctx.getString(R.string.ttsvoice);
		}
		return s;
	}

	public String getVisibleName() {
		return getBasename().replace('_', ' ');
	}

	public String getBasename() {
		if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
		}
		int ls = fileName.lastIndexOf('_');
		if (ls >= 0) {
			return fileName.substring(0, ls);
		}
		return fileName;
	}

	public boolean isAccepted() {
		// POI index download is not supported any longer
		if (fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT, IndexConstants.BINARY_MAP_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION))
				|| fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)
		// || fileName.endsWith(addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION)) drop support for
		// downloading tts files from inet
		) {
			return true;
		}
		return false;
	}

	protected static String addVersionToExt(String ext, int version) {
		return "_" + version + ext;
	}

	public String getFileName() {
		return fileName;
	}

	public String getDescription() {
		return description;
	}

	public String getDate() {
		return date;
	}

	public String getSize() {
		return size;
	}

	public DownloadEntry createDownloadEntry(ClientContext ctx, DownloadActivityType type) {
		String fileName = this.fileName;
		File parent = null;
		String toSavePostfix = null;
		String toCheckPostfix = null;
		boolean unzipDir = false;
		boolean preventMediaIndexing = false;
		if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			parent = ctx.getAppDir();
			toSavePostfix = BINARY_MAP_INDEX_EXT;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			parent = ctx.getAppDir();
			toSavePostfix = BINARY_MAP_INDEX_EXT_ZIP;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			parent = ctx.getAppDir();
			// unzipDir = true;
			toSavePostfix = IndexConstants.EXTRA_ZIP_EXT;
			toCheckPostfix = IndexConstants.EXTRA_EXT;
		} else if (fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)) {
			parent = ctx.getVoiceDir();
			toSavePostfix = VOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
			preventMediaIndexing = true;
		} else if (fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
			parent = ctx.getVoiceDir();
			toSavePostfix = TTSVOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
		}
		if (type == DownloadActivityType.ROADS_FILE) {
			toSavePostfix = "-roads" + toSavePostfix;
			toCheckPostfix = "-roads" + toCheckPostfix;
		}
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
			entry = null;
		} else {
			entry = new DownloadEntry();
			entry.type = type;
			entry.baseName = getBasename();
			entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
			entry.unzip = unzipDir;
			SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
			try {
				Date d = format.parse(date);
				entry.dateModified = d.getTime();
			} catch (ParseException e1) {
				log.error("ParseException", e1);
			}
			try {
				entry.sizeMB = Double.parseDouble(size);
			} catch (NumberFormatException e1) {
				log.error("ParseException", e1);
			}
			entry.parts = 1;
			if (parts != null) {
				entry.parts = Integer.parseInt(parts);
			}
			entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
			File backup = new File(ctx.getBackupDir(), entry.fileToUnzip.getName());
			if (backup.exists()) {
				entry.existingBackupFile = backup;
			}
		}
		if (attachedItem != null) {
			entry.attachedEntry = attachedItem.createDownloadEntry(ctx, type);
		}
		return entry;
	}

	public String convertServerFileNameToLocal() {
		String e = getFileName();
		int l = e.lastIndexOf('_');
		String s;
		if (e.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || e.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			s = IndexConstants.BINARY_MAP_INDEX_EXT;
		} else {
			s = ""; //$NON-NLS-1$
		}
		if (getType() == DownloadActivityType.ROADS_FILE) {
			s = "-roads" + s;
		}
		return e.substring(0, l) + s;
	}
}