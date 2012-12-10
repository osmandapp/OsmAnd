package net.osmand.plus.download;

import java.io.File;
import java.util.List;

public class DownloadEntry {
	public File fileToSave;
	public File fileToUnzip;
	public boolean unzip;
	public Long dateModified;
	public double sizeMB;
	public String baseName;
	public String urlToDownload;
	public int parts;
	public File existingBackupFile;
	public boolean isAsset;
	public DownloadActivityType type;
	
	public List<String> srtmFilesToDownload;
	public DownloadEntry attachedEntry;

	public DownloadEntry() {
		// default
	}

	public DownloadEntry(String assetName, String fileName, long dateModified) {
		this.dateModified = dateModified;
		fileToUnzip = new File(fileName);
		fileToSave = new File(assetName);
		isAsset = true;
	}

}