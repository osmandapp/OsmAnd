package net.osmand.plus.download;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;

import java.io.File;

public class LocalIndexInfo implements Comparable<LocalIndexInfo> {

	private final LocalIndexType type;
	private String description = "";
	private String name;

	private boolean backupedData;
	private boolean corrupted;
	private boolean notSupported;
	private boolean loaded;
	private String subfolder;
	private String pathToData;
	private String fileName;
	private boolean singleFile;
	private int kbSize = -1;
	private Object attachedObject;

	// UI state expanded
	private boolean expanded;

	private GPXFile gpxFile;

	public LocalIndexInfo(@NonNull LocalIndexType type, @NonNull File f, boolean backuped) {
		this.pathToData = f.getAbsolutePath();
		this.fileName = f.getName();
		this.name = formatName(f.getName());
		this.type = type;
		this.singleFile = !f.isDirectory();
		if (this.singleFile) {
			this.kbSize = (int) ((f.length() + 512) >> 10);
		}
		this.backupedData = backuped;
	}

	public void setAttachedObject(Object attachedObject) {
		this.attachedObject = attachedObject;
	}

	public Object getAttachedObject() {
		return attachedObject;
	}

	private String formatName(String name) {
		int ext = name.indexOf('.');
		if (ext != -1) {
			name = name.substring(0, ext);
		}
		return name.replace('_', ' ');
	}

	// Special domain object represents category
	public LocalIndexInfo(@NonNull LocalIndexType type, boolean backup, @NonNull String subfolder) {
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
		return backupedData ? LocalIndexType.DEACTIVATED : type;
	}

	public LocalIndexType getOriginalType() {
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

	public String getFileNameWithoutRoadSuffix() {
		if (fileName.endsWith(BINARY_ROAD_MAP_INDEX_EXT)) {
			return fileName.substring(0, fileName.lastIndexOf(BINARY_ROAD_MAP_INDEX_EXT)) + BINARY_MAP_INDEX_EXT;
		}
		return fileName;
	}

	public String getBaseName(@NonNull OsmandApplication app) {
		return type.getBasename(app, this);
	}

	@Override
	public int compareTo(LocalIndexInfo o) {
		return getFileName().compareTo(o.getFileName());
	}
}