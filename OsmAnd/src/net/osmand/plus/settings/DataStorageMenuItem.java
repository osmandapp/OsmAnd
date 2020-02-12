package net.osmand.plus.settings;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IdRes;

public class DataStorageMenuItem implements Parcelable, Cloneable {

	private String key;
	private int type;
	private String title;
	private String description;
	private String directory;
	@IdRes
	private int iconResId;

	private DataStorageMenuItem(String key, int type, String title, String description,
	                            String directory, int iconResId) {
		this.key = key;
		this.type = type;
		this.title = title;
		this.description = description;
		this.directory = directory;
		this.iconResId = iconResId;
	}

	private DataStorageMenuItem(Parcel in) {
		key = in.readString();
		type = in.readInt();
		title = in.readString();
		description = in.readString();
		directory = in.readString();
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getDirectory() {
		return directory;
	}

	public int getIconResId() {
		return iconResId;
	}

	public String getKey() {
		return key;
	}

	public int getType() {
		return type;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public void setIconResId(int iconResId) {
		this.iconResId = iconResId;
	}

	public static DataStorageMenuItemBuilder builder() {
		return new DataStorageMenuItemBuilder();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeInt(type);
		dest.writeString(title);
		dest.writeString(description);
		dest.writeString(directory);
	}

	public static final Parcelable.Creator<DataStorageMenuItem> CREATOR = new Parcelable.Creator<DataStorageMenuItem>() {

		@Override
		public DataStorageMenuItem createFromParcel(Parcel source) {
			return new DataStorageMenuItem(source);
		}

		@Override
		public DataStorageMenuItem[] newArray(int size) {
			return new DataStorageMenuItem[size];
		}
	};

	public static class DataStorageMenuItemBuilder {
		private String key;
		private int type;
		private String title;
		private String description;
		private String directory;
		@IdRes
		private int iconResId;

		public DataStorageMenuItemBuilder setKey(String key) {
			this.key = key;
			return this;
		}

		public DataStorageMenuItemBuilder setType(int type) {
			this.type = type;
			return this;
		}

		public DataStorageMenuItemBuilder setTitle(String title) {
			this.title = title;
			return this;
		}

		public DataStorageMenuItemBuilder setDescription(String description) {
			this.description = description;
			return this;
		}

		public DataStorageMenuItemBuilder setDirectory(String directory) {
			this.directory = directory;
			return this;
		}

		public DataStorageMenuItemBuilder setIconResId(int iconResId) {
			this.iconResId = iconResId;
			return this;
		}

		public DataStorageMenuItem createItem() {
			return new DataStorageMenuItem(key, type, title, description, directory, iconResId);
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return DataStorageMenuItem.builder()
				.setKey(this.key)
				.setTitle(this.title)
				.setDescription(this.description)
				.setDirectory(this.directory)
				.setType(this.type)
				.setIconResId(this.iconResId)
				.createItem();
	}
}