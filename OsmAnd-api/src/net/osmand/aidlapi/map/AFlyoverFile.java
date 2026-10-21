package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * A flyover video recorded on the phone.
 */
public class AFlyoverFile extends AidlParams {

	private String fileName;
	private long size;
	private long lastModified;
	private boolean recording;

	public AFlyoverFile(String fileName, long size, long lastModified, boolean recording) {
		this.fileName = fileName;
		this.size = size;
		this.lastModified = lastModified;
		this.recording = recording;
	}

	public AFlyoverFile(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AFlyoverFile> CREATOR = new Creator<AFlyoverFile>() {
		@Override
		public AFlyoverFile createFromParcel(Parcel in) {
			return new AFlyoverFile(in);
		}

		@Override
		public AFlyoverFile[] newArray(int size) {
			return new AFlyoverFile[size];
		}
	};

	/**
	 * @return file name in OsmAnd's flyover folder, e.g. flyover_2026-10-12_13-04-11.mp4
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return size in bytes
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @return time of the last change, ms since the epoch
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * @return true while the video is still being recorded; it can be opened only when it is finished
	 */
	public boolean isRecording() {
		return recording;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putLong("size", size);
		bundle.putLong("lastModified", lastModified);
		bundle.putBoolean("recording", recording);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		size = bundle.getLong("size");
		lastModified = bundle.getLong("lastModified");
		recording = bundle.getBoolean("recording");
	}
}
