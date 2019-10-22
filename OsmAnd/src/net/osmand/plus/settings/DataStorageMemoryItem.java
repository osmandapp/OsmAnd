package net.osmand.plus.settings;

import android.os.Parcel;
import android.os.Parcelable;

public class DataStorageMemoryItem implements Parcelable {
	public final static int EXTENSIONS = 0;
	public final static int PREFIX = 1;
	
	private String key;
	private String[] extensions;
	private String[] prefixes;
	private Directory[] directories;
	private long usedMemoryBytes;

	private DataStorageMemoryItem(String key, String[] extensions, String[] prefixes, long usedMemoryBytes, Directory[] directories) {
		this.key = key;
		this.extensions = extensions;
		this.prefixes = prefixes;
		this.usedMemoryBytes = usedMemoryBytes;
		this.directories = directories;
	}

	private DataStorageMemoryItem(Parcel in) {
		key = in.readString();
		in.readStringArray(extensions);
		in.writeStringArray(prefixes);
		directories = (Directory[]) in.readArray(Directory.class.getClassLoader());
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

	public static DataStorageMemoryItemBuilder builder() {
		return new DataStorageMemoryItemBuilder();
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String[] getPrefixes() {
		return prefixes;
	}

	public Directory[] getDirectories() {
		return directories;
	}

	public void addBytes(long bytes) {
		this.usedMemoryBytes += bytes;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeStringArray(extensions);
		dest.writeStringArray(prefixes);
		dest.writeArray(directories);
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
		private String[] prefixes;
		private Directory[] directories;
		private long usedMemoryBytes;

		public DataStorageMemoryItemBuilder setKey(String key) {
			this.key = key;
			return this;
		}

		public DataStorageMemoryItemBuilder setExtensions(String ... extensions) {
			this.extensions = extensions;
			return this;
		}

		public DataStorageMemoryItemBuilder setPrefixes(String ... prefixes) {
			this.prefixes = prefixes;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setDirectories(Directory ... directories) {
			this.directories = directories;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setUsedMemoryBytes(long usedMemoryBytes) {
			this.usedMemoryBytes = usedMemoryBytes;
			return this;
		}
		
		public DataStorageMemoryItem createItem() {
			return new DataStorageMemoryItem(key, extensions, prefixes, usedMemoryBytes, directories);
		}
	}
	
	public static class Directory implements Parcelable {
		private String absolutePath;
		private boolean goDeeper;
		private int checkingType;
		private boolean skipOther;

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(absolutePath);
			dest.writeInt(goDeeper ? 1 : 0);
			dest.writeInt(checkingType);
			dest.writeInt(skipOther ? 1 : 0);
		}

		public Directory(String absolutePath, boolean goDeeper, int checkingType, boolean skipOther) {
			this.absolutePath = absolutePath;
			this.goDeeper = goDeeper;
			this.checkingType = checkingType;
			this.skipOther = skipOther;
		}

		public String getAbsolutePath() {
			return absolutePath;
		}

		public boolean isGoDeeper() {
			return goDeeper;
		}

		public int getCheckingType() {
			return checkingType;
		}

		public boolean isSkipOther() {
			return skipOther;
		}

		private Directory(Parcel in) {
			absolutePath = in.readString();
			goDeeper = in.readInt() == 1;
			checkingType = in.readInt();
			skipOther = in.readInt() == 1;
		}

		public static final Parcelable.Creator<Directory> CREATOR = new Parcelable.Creator<Directory>() {

			@Override
			public Directory createFromParcel(Parcel source) {
				return new Directory(source);
			}

			@Override
			public Directory[] newArray(int size) {
				return new Directory[size];
			}
		};
	}
}
