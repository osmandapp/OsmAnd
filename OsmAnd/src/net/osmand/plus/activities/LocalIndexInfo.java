package net.osmand.plus.activities;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;

import java.io.File;

public class LocalIndexInfo implements Parcelable {

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

	private WorldRegion worldRegion;

	// UI state expanded
	private boolean expanded;

	private GPXFile gpxFile;

	public LocalIndexInfo(@NonNull LocalIndexType type, @NonNull File f, boolean backuped,
						  @NonNull OsmandApplication app) {
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
	public LocalIndexInfo(@NonNull LocalIndexType type, boolean backup, @NonNull String subfolder,
						  @NonNull OsmandApplication app) {
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

	public WorldRegion getWorldRegion() {
		return worldRegion;
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

	public String getBaseName() {
		return type.getBasename(this);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.type == null ? -1 : this.type.ordinal());
		dest.writeString(this.description);
		dest.writeString(this.name);
		dest.writeByte(backupedData ? (byte) 1 : (byte) 0);
		dest.writeByte(corrupted ? (byte) 1 : (byte) 0);
		dest.writeByte(notSupported ? (byte) 1 : (byte) 0);
		dest.writeByte(loaded ? (byte) 1 : (byte) 0);
		dest.writeString(this.subfolder);
		dest.writeString(this.pathToData);
		dest.writeString(this.fileName);
		dest.writeByte(singleFile ? (byte) 1 : (byte) 0);
		dest.writeInt(this.kbSize);
		dest.writeSerializable(this.worldRegion);
		dest.writeByte(expanded ? (byte) 1 : (byte) 0);
	}

	protected LocalIndexInfo(Parcel in) {
		int tmpType = in.readInt();
		this.type = tmpType == -1 ? null : LocalIndexType.values()[tmpType];
		this.description = in.readString();
		this.name = in.readString();
		this.backupedData = in.readByte() != 0;
		this.corrupted = in.readByte() != 0;
		this.notSupported = in.readByte() != 0;
		this.loaded = in.readByte() != 0;
		this.subfolder = in.readString();
		this.pathToData = in.readString();
		this.fileName = in.readString();
		this.singleFile = in.readByte() != 0;
		this.kbSize = in.readInt();
		this.worldRegion = (WorldRegion) in.readSerializable();
		this.expanded = in.readByte() != 0;
	}

	public static final Creator<LocalIndexInfo> CREATOR = new Creator<LocalIndexInfo>() {
		public LocalIndexInfo createFromParcel(Parcel source) {
			return new LocalIndexInfo(source);
		}

		public LocalIndexInfo[] newArray(int size) {
			return new LocalIndexInfo[size];
		}
	};
}