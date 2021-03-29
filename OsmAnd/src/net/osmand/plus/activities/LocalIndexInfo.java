package net.osmand.plus.activities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;

import java.io.File;
import java.io.Serializable;

public class LocalIndexInfo implements Comparable<LocalIndexInfo>, Parcelable {

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
	private Object attachedObject;

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

	protected LocalIndexInfo(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<LocalIndexInfo> CREATOR = new Creator<LocalIndexInfo>() {
		@Override
		public LocalIndexInfo createFromParcel(Parcel in) {
			return new LocalIndexInfo(in);
		}

		@Override
		public LocalIndexInfo[] newArray(int size) {
			return new LocalIndexInfo[size];
		}
	};

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

	public String getBaseName() {
		return type.getBasename(this);
	}

	@Override
	public int compareTo(LocalIndexInfo o) {
		return getFileName().compareTo(o.getFileName());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(type);
		dest.writeString(description);
		dest.writeString(name);
		dest.writeByte((byte) (backupedData ? 1 : 0));
		dest.writeByte((byte) (corrupted ? 1 : 0));
		dest.writeByte((byte) (notSupported ? 1 : 0));
		dest.writeByte((byte) (loaded ? 1 : 0));
		dest.writeString(subfolder);
		dest.writeString(pathToData);
		dest.writeString(fileName);
		dest.writeByte((byte) (singleFile ? 1 : 0));
		dest.writeInt(kbSize);
		dest.writeSerializable((Serializable) attachedObject);
		dest.writeByte((byte) (expanded ? 1 : 0));
		dest.writeValue(gpxFile);
	}

	private void readFromParcel(Parcel in) {
		type = (LocalIndexType) in.readSerializable();
		description = in.readString();
		name = in.readString();
		backupedData = in.readByte() != 0;
		corrupted = in.readByte() != 0;
		notSupported = in.readByte() != 0;
		loaded = in.readByte() != 0;
		subfolder = in.readString();
		pathToData = in.readString();
		fileName = in.readString();
		singleFile = in.readByte() != 0;
		kbSize = in.readInt();
		attachedObject = in.readSerializable();
		expanded = in.readByte() != 0;
		gpxFile = (GPXFile) in.readSerializable();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
