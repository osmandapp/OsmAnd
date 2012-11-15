package net.osmand.plus.download;

import java.io.File;

public class DownloadEntry {
	public File fileToSave;
	public File fileToUnzip;
	public boolean unzip;
	public Long dateModified;
	public double sizeMB;
	public String baseName;
	public int parts;
	public File existingBackupFile;
	public DownloadEntry attachedEntry;
	public boolean isAsset;
	public boolean isRoadMap;

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