package net.osmand.download;

import java.io.File;
import java.util.List;

public class DownloadEntry {
	public Long dateModified;
	public double sizeMB;
	
	public File targetFile;
	public boolean zipStream;
	public boolean unzipFolder;
	
	public File fileToDownload;
	
	public String baseName;
	public String urlToDownload;
	public int parts;
	public File existingBackupFile;
	public boolean isAsset;
	public String assetName;
	public DownloadActivityType type;
	
	public List<String> srtmFilesToDownload;
	public DownloadEntry attachedEntry;
	public IndexItem item;

	public DownloadEntry(IndexItem item) {
		this.item = item;
	}

	public DownloadEntry(IndexItem pr, String assetName, String fileName, long dateModified) {
		this.dateModified = dateModified;
		this.item = pr;
		targetFile = new File(fileName);
		this.assetName = assetName;
		isAsset = true;
	}

}