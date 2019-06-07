package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class NavDrawerItem implements Parcelable {

	private String name;
	private String uri;
	private String iconName;
	private int flags;

	public NavDrawerItem(@NonNull String name, @NonNull String uri, @Nullable String iconName) {
		this(name, uri, iconName, -1);
	}

	public NavDrawerItem(@NonNull String name, @NonNull String uri, @Nullable String iconName, int flags) {
		this.name = name;
		this.uri = uri;
		this.iconName = iconName;
		this.flags = flags;
	}

	protected NavDrawerItem(Parcel in) {
		name = in.readString();
		uri = in.readString();
		iconName = in.readString();
		flags = in.readInt();
	}

	public String getName() {
		return name;
	}

	public String getUri() {
		return uri;
	}

	public String getIconName() {
		return iconName;
	}

	public int getFlags() {
		return flags;
	}

	@Override
	public void writeToParcel(Parcel dest, int f) {
		dest.writeString(name);
		dest.writeString(uri);
		dest.writeString(iconName);
		dest.writeInt(flags);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<NavDrawerItem> CREATOR = new Creator<NavDrawerItem>() {
		@Override
		public NavDrawerItem createFromParcel(Parcel in) {
			return new NavDrawerItem(in);
		}

		@Override
		public NavDrawerItem[] newArray(int size) {
			return new NavDrawerItem[size];
		}
	};
}
