package net.osmand.plus.download;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.map.RegionCountry;
import net.osmand.plus.ClientContext;
import net.osmand.plus.R;
import net.osmand.plus.Version;

public class SrtmIndexItem extends IndexItem {
	
	private RegionCountry item;
	private List<String> tilesToDownload = new ArrayList<String>();
	public SrtmIndexItem(RegionCountry item, Map<String, String> existingFileNames) {
		super(fileName(item), "Elevation lines", "",  item.getTileSize()+"", null);
		this.item = item;
		type = DownloadActivityType.SRTM_FILE;
		updateExistingTiles(existingFileNames);
	}
	
	public void updateExistingTiles(Map<String, String> existingFileNames) {
		tilesToDownload.clear();
		for (int i = 0; i < item.getTileSize(); i++) {
			int lat = item.getLat(i);
			int lon = item.getLon(i);
			String fname = getFileName(lat, lon);
			if (!existingFileNames.containsKey(fname + IndexConstants.BINARY_MAP_INDEX_EXT)) {
				tilesToDownload.add(fname);
			}
		}
	}

	private String getFileName(int lat, int lon) {
		String fn = lat >= 0 ? "N" : "S";
		if(Math.abs(lat) < 10) {
			fn += "0";
		}
		fn += Math.abs(lat);
		fn += lon >= 0 ? "e" : "w";
		if(Math.abs(lon) < 10) {
			fn += "00";
		} else if(Math.abs(lon) < 100) {
			fn += "0";
		}
		fn += Math.abs(lon);
		return fn;
	}

	private static String fileName(RegionCountry r) {
		if(r.parent == null) {
			return (r.continentName + " " + r.name).trim();
		} else {
			return (r.parent.continentName + " " + r.parent.name + " " + r.name).trim();
		}
	}

	@Override
	public boolean isAccepted() {
		return true;
	}
	
	@Override
	public List<DownloadEntry> createDownloadEntry(ClientContext ctx, DownloadActivityType type, 
			List<DownloadEntry> downloadEntries) {
		File parent = ctx.getAppPath(IndexConstants.SRTM_INDEX_DIR);
		parent.mkdirs();
		List<DownloadEntry> toDownload = new ArrayList<DownloadEntry>();
		if (parent == null || !parent.exists()) {
			ctx.showToastMessage(R.string.sd_dir_not_accessible);
		} else {
			for (String fileToDownload : tilesToDownload) {
				DownloadEntry entry = new DownloadEntry();
				entry.type = type;
				entry.baseName = fileToDownload;
				String url = "http://" + IndexConstants.INDEX_DOWNLOAD_DOMAIN;
				url += "/download?event=2&srtm=yes&";
				url += Version.getVersionAsURLParam(ctx) + "&";
				String fullName = fileToDownload + "_" + IndexConstants.BINARY_MAP_VERSION + IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
				entry.urlToDownload = url +"file=" + fullName;
				// url + "file=" + fileName;
				entry.fileToSave = new File(parent, fullName);
				entry.unzip = false;
				entry.dateModified = System.currentTimeMillis();
				entry.sizeMB = 10;
				entry.parts = 1;
				entry.fileToUnzip = new File(parent, entry.baseName + IndexConstants.BINARY_MAP_INDEX_EXT);
				downloadEntries.add(entry);
				toDownload.size();
			}
		}
		return downloadEntries;
	}
	
	@Override
	public String getTargetFileName() {
		return fileName+".nonexistent";
	}
	
	@Override
	public String getBasename() {
		return fileName;
	}
	
	@Override
	public String getSizeDescription(ClientContext ctx) {
		return (item.getTileSize() - tilesToDownload.size()) + "/" + item.getTileSize() + " " + ctx.getString(R.string.index_srtm_parts);
	}
	
	@Override
	public String getVisibleDescription(ClientContext ctx) {
		return ctx.getString(R.string.index_srtm_ele);
	}
	
	@Override
	public boolean isAlreadyDownloaded(Map<String, String> listAlreadyDownloaded) {
		for (int i = 0; i < item.getTileSize(); i++) {
			int lat = item.getLat(i);
			int lon = item.getLon(i);
			String fname = getFileName(lat, lon);
			if (listAlreadyDownloaded.containsKey(fname + IndexConstants.BINARY_MAP_INDEX_EXT)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String getVisibleName(ClientContext ctx) {
		if(item.parent == null) {
			return item.name;
		} else {
			return item.parent.name +" "+item.name;
		}
	}
}
