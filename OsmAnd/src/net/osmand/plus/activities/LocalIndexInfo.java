package net.osmand.plus.activities;

import java.io.File;

import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;

public class LocalIndexInfo {

	private LocalIndexType type;
	private String description = "";
	private String name;

	private boolean backupedData;
	private boolean corrupted = false;
	private boolean notSupported = false;
	private boolean loaded;
	private String subfolder;
	private String pathToData;
	private String fileName;
	private boolean singleFile;
	private int kbSize = -1;

	// UI state expanded
	private boolean expanded;

	private GPXFile gpxFile;

	public LocalIndexInfo(LocalIndexType type, File f, boolean backuped) {
		pathToData = f.getAbsolutePath();
		fileName = f.getName();
		name = formatName(f.getName());
		this.type = type;
		singleFile = !f.isDirectory();
		if (singleFile) {
			kbSize = (int) ((f.length() + 512) >> 10);
		}
		this.backupedData = backuped;
	}

	private String formatName(String name) {
		int ext = name.indexOf('.');
		if (ext != -1) {
			name = name.substring(0, ext);
		}
		return name.replace('_', ' ');
	}

	// Special domain object represents category
	public LocalIndexInfo(LocalIndexType type, boolean backup, String subfolder) {
		this.type = type;
		backupedData = backup;
		this.subfolder = subfolder;
	}

	public void setCorrupted(boolean corrupted) {
		this.corrupted = corrupted;
		if (corrupted) {
			this.loaded = false;
		}
	}

	public void setBackupedData(boolean backupedData) {
		this.backupedData = backupedData;
	}

	public void setSize(int size) {
		this.kbSize = size;
	}

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public void setNotSupported(boolean notSupported) {
		this.notSupported = notSupported;
		if (notSupported) {
			this.loaded = false;
		}
	}
	
	public void setSubfolder(String subfolder) {
		this.subfolder = subfolder;
	}
	
	public String getSubfolder() {
		return subfolder;
	}

	public int getSize() {
		return kbSize;
	}

	public boolean isNotSupported() {
		return notSupported;
	}

	public String getName() {
		return name;
	}

	public LocalIndexType getType() {
		return type;
	}

	public boolean isSingleFile() {
		return singleFile;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public boolean isCorrupted() {
		return corrupted;
	}

	public boolean isBackupedData() {
		return backupedData;
	}

	public String getPathToData() {
		return pathToData;
	}

	public String getDescription() {
		return description;
	}

	public String getFileName() {
		return fileName;
	}

}