package net.osmand.plus.settings.datastorage.item;

import static net.osmand.plus.settings.datastorage.DataStorageHelper.MANUALLY_SPECIFIED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.SHARED_STORAGE;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IdRes;

public class StorageItem implements Parcelable, Cloneable {

	private String key;
	private int type;
	private String title;
	private String description;
	private String directory;
	@IdRes
	private int notSelectedIconResId;
	@IdRes
	private int selectedIconResId;

	private StorageItem(String key,
	                    int type,
	                    String title,
	                    String description,
	                    String directory,
	                    int notSelectedIconResId,
	                    int selectedIconResId) {
		this.key = key;
		this.type = type;
		this.title = title;
		this.description = description;
		this.directory = directory;
		this.notSelectedIconResId = notSelectedIconResId;
		this.selectedIconResId = selectedIconResId;
	}

	private StorageItem(Parcel in) {
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

	public int getNotSelectedIconResId() {
		return notSelectedIconResId;
	}

	public int getSelectedIconResId() {
		return selectedIconResId;
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

	public boolean isStorageSizeDefinable() {
		return !SHARED_STORAGE.equals(key) && !MANUALLY_SPECIFIED.equals(key);
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

	public static final Parcelable.Creator<StorageItem> CREATOR = new Parcelable.Creator<StorageItem>() {

		@Override
		public StorageItem createFromParcel(Parcel source) {
			return new StorageItem(source);
		}

		@Override
		public StorageItem[] newArray(int size) {
			return new StorageItem[size];
		}
	};

	public static class DataStorageMenuItemBuilder {
		private String key;
		private int type;
		private String title;
		private String description;
		private String directory;
		@IdRes
		private int notSelectedIconResId;
		@IdRes
		private int selectedIconResId;

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
			this.notSelectedIconResId = iconResId;
			this.selectedIconResId = iconResId;
			return this;
		}

		public DataStorageMenuItemBuilder setIconResIds(int notSelectedIconResId, int selectedIconResId) {
			this.notSelectedIconResId = notSelectedIconResId;
			this.selectedIconResId = selectedIconResId;
			return this;
		}

		public StorageItem createItem() {
			return new StorageItem(key, type, title, description, directory, notSelectedIconResId, selectedIconResId);
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return builder()
				.setKey(this.key)
				.setTitle(this.title)
				.setDescription(this.description)
				.setDirectory(this.directory)
				.setType(this.type)
				.setIconResIds(this.notSelectedIconResId, this.selectedIconResId)
				.createItem();
	}
}