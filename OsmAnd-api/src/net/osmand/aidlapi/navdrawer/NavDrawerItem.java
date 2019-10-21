package net.osmand.aidlapi.navdrawer;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class NavDrawerItem extends AidlParams {

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
		readFromParcel(in);
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
	protected void readFromBundle(Bundle bundle) {
		name = bundle.getString("name");
		uri = bundle.getString("uri");
		iconName = bundle.getString("iconName");
		flags = bundle.getInt("flags");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("name", name);
		bundle.putString("uri", uri);
		bundle.putString("iconName", iconName);
		bundle.putInt("flags", flags);
	}
}