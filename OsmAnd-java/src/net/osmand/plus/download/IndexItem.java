package net.osmand.plus.download;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_ZIP;
import static net.osmand.IndexConstants.VOICE_INDEX_EXT_ZIP;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.ClientContext;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class IndexItem implements Comparable<IndexItem> {
	private static final Log log = PlatformUtil.getLog(IndexItem.class);
	
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

	public String getVisibleDescription(ClientContext ctx) {
		String s = ""; //$NON-NLS-1$
		if (type == DownloadActivityType.SRTM_FILE) {
			return ctx.getString(R.string.download_srtm_maps);
		} else if (type == DownloadActivityType.ROADS_FILE) {
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

	public String getVisibleName(ClientContext ctx) {
		return getBasename().replace('_', ' ');
	}

	public String getBasename() {
		if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length()).replace('_', ' ');
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
				|| fileName.endsWith(IndexConstants.SQLITE_EXT)
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
	
	public String getSizeDescription(ClientContext ctx) {
		return size + " MB";
	}

	public String getSize() {
		return size;
	}

	public List<DownloadEntry> createDownloadEntry(ClientContext ctx, DownloadActivityType type, 
			List<DownloadEntry> downloadEntries) {
		String fileName = this.fileName;
		File parent = null;
		String toSavePostfix = null;
		String toCheckPostfix = null;
		boolean unzipDir = false;
		boolean preventMediaIndexing = false;
		if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			parent = ctx.getAppPath(IndexConstants.MAPS_PATH);
			toSavePostfix = BINARY_MAP_INDEX_EXT;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			parent = ctx.getAppPath(IndexConstants.MAPS_PATH);
			toSavePostfix = BINARY_MAP_INDEX_EXT_ZIP;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			parent = ctx.getAppPath("");
			// unzipDir = true;
			toSavePostfix = IndexConstants.EXTRA_ZIP_EXT;
			toCheckPostfix = IndexConstants.EXTRA_EXT;
		} else if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			parent = ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
			toSavePostfix = IndexConstants.SQLITE_EXT;
			toCheckPostfix = IndexConstants.SQLITE_EXT;
		} else if (fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)) {
			parent = ctx.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			toSavePostfix = VOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
			preventMediaIndexing = true;
		} else if (fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
			parent = ctx.getAppPath(IndexConstants.VOICE_INDEX_DIR);
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
		} else {
			entry = new DownloadEntry();
			entry.type = type;
			entry.baseName = getBasename();
			String url = "http://" + IndexConstants.INDEX_DOWNLOAD_DOMAIN + "/download?event=2&";
			url += Version.getVersionAsURLParam(ctx) + "&";
			if (type == DownloadActivityType.ROADS_FILE) {
				url += "road=yes&";
			}
			if (type == DownloadActivityType.HILLSHADE_FILE) {
				url += "hillshade=yes&";
			}
			entry.urlToDownload = url + "file=" + fileName;
			entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
			entry.unzip = unzipDir;
			try {
				Date d = Algorithms.getDateFormat().parse(date);
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
			File backup = new File(ctx.getAppPath(IndexConstants.BACKUP_INDEX_DIR), entry.fileToUnzip.getName());
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
	
	public String getTargetFileName(){
		String e = getFileName();
		
		if (e.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || e.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			int l = e.lastIndexOf('_');
			if(l == -1) {
				l = e.length();
			}
			String s = e.substring(0, l);
			if (getType() == DownloadActivityType.ROADS_FILE) {
				s += "-roads" ;
			}	
			s += IndexConstants.BINARY_MAP_INDEX_EXT;
			return s;
		} else if(e.endsWith(IndexConstants.SQLITE_EXT)){
			return e.replace('_', ' ');
		} else if(e.endsWith(IndexConstants.EXTRA_ZIP_EXT)){
			return e.substring(0, e.length() - IndexConstants.EXTRA_ZIP_EXT.length()) + IndexConstants.EXTRA_EXT; 
		} else if(e.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP) || e.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
			int l = e.lastIndexOf('_');
			if(l == -1) {
				l = e.length();
			}
			String s = e.substring(0, l);
			return s;
		}
			
		return e;
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

}