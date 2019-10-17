package net.osmand.plus.settings;

import android.os.Parcel;
import android.os.Parcelable;

public class DataStorageMemoryItem implements Parcelable {
	private String key;
	private String[] extensions;
	private String[] directories;
	private long usedMemoryBytes;

	private DataStorageMemoryItem(String key, String[] extensions, long usedMemoryBytes, String[] directories) {
		this.key = key;
		this.extensions = extensions;
		this.usedMemoryBytes = usedMemoryBytes;
		this.directories = directories;
	}

	private DataStorageMemoryItem(Parcel in) {
		key = in.readString();
		in.readStringArray(extensions);
		in.readStringArray(directories);
		usedMemoryBytes = in.readLong();
	}

	public String getKey() {
		return key;
	}

	public long getUsedMemoryBytes() {
		return usedMemoryBytes;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setUsedMemoryBytes(long usedMemoryBytes) {
		this.usedMemoryBytes = usedMemoryBytes;
	}
	
	public static DataStorageMemoryItemBuilder builder() {
		return new DataStorageMemoryItemBuilder();
	}

	public String[] getExtensions() {
		return extensions;
	}

	public void setExtensions(String[] extensions) {
		this.extensions = extensions;
	}

	public String[] getDirectories() {
		return directories;
	}

	public void setDirectories(String[] directories) {
		this.directories = directories;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeStringArray(extensions);
		dest.writeStringArray(directories);
		dest.writeLong(usedMemoryBytes);
	}

	public static final Parcelable.Creator<DataStorageMemoryItem> CREATOR = new Parcelable.Creator<DataStorageMemoryItem>() {

		@Override
		public DataStorageMemoryItem createFromParcel(Parcel source) {
			return new DataStorageMemoryItem(source);
		}

		@Override
		public DataStorageMemoryItem[] newArray(int size) {
			return new DataStorageMemoryItem[size];
		}
	};

	public static class DataStorageMemoryItemBuilder {
		private String key;
		private String[] extensions;
		private String[] directories;
		private long usedMemoryBytes;

		public DataStorageMemoryItemBuilder setKey(String key) {
			this.key = key;
			return this;
		}

		public DataStorageMemoryItemBuilder setExtensions(String ... extensions) {
			this.extensions = extensions;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setDirectories(String ... directories) {
			this.directories = directories;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setUsedMemoryBytes(long usedMemoryBytes) {
			this.usedMemoryBytes = usedMemoryBytes;
			return this;
		}
		
		public DataStorageMemoryItem createItem() {
			return new DataStorageMemoryItem(key, extensions, usedMemoryBytes, directories);
		}
	}
	
	public void addBytes(long bytes) {
		this.usedMemoryBytes += bytes;
	}
}
