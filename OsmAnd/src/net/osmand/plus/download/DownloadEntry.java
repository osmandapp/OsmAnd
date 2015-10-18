package net.osmand.plus.download;

import java.io.File;

public class DownloadEntry {
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