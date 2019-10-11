package net.osmand.aidlapi.tiles;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

public class ASqliteDbFile extends AidlParams {

	private String fileName;
	private long modifiedTime;
	private long fileSize;
	private boolean active;

	public ASqliteDbFile(@NonNull String fileName, long modifiedTime, long fileSize, boolean active) {
		this.fileName = fileName;
		this.modifiedTime = modifiedTime;
		this.fileSize = fileSize;
		this.active = active;
	}

	public ASqliteDbFile(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ASqliteDbFile> CREATOR = new Creator<ASqliteDbFile>() {
		@Override
		public ASqliteDbFile createFromParcel(Parcel in) {
			return new ASqliteDbFile(in);
		}

		@Override
		public ASqliteDbFile[] newArray(int size) {
			return new ASqliteDbFile[size];
		}
	};

	public String getFileName() {
		return fileName;
	}

	public long getModifiedTime() {
		return modifiedTime;
	}

	public long getFileSize() {
		return fileSize;
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putLong("modifiedTime", modifiedTime);
		bundle.putLong("fileSize", fileSize);
		bundle.putBoolean("active", active);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		modifiedTime = bundle.getLong("modifiedTime");
		fileSize = bundle.getLong("fileSize");
		active = bundle.getBoolean("active");
	}
}