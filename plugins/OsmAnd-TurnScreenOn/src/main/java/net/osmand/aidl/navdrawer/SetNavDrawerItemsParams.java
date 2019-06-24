package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SetNavDrawerItemsParams implements Parcelable {

	private String appPackage;
	private List<NavDrawerItem> items;

	public SetNavDrawerItemsParams(@NonNull String appPackage, @NonNull List<NavDrawerItem> items) {
		this.appPackage = appPackage;
		this.items = items;
	}

	protected SetNavDrawerItemsParams(Parcel in) {
		appPackage = in.readString();
		items = new ArrayList<>();
		in.readTypedList(items, NavDrawerItem.CREATOR);
	}

	public String getAppPackage() {
		return appPackage;
	}

	public List<NavDrawerItem> getItems() {
		return items;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(appPackage);
		dest.writeTypedList(new ArrayList<>(items));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<SetNavDrawerItemsParams> CREATOR = new Creator<SetNavDrawerItemsParams>() {
		@Override
		public SetNavDrawerItemsParams createFromParcel(Parcel in) {
			return new SetNavDrawerItemsParams(in);
		}

		@Override
		public SetNavDrawerItemsParams[] newArray(int size) {
			return new SetNavDrawerItemsParams[size];
		}
	};
}
