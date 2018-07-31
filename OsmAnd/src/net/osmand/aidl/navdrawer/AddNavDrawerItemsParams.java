package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class AddNavDrawerItemsParams implements Parcelable {

	private String appPackage;
	private List<NavDrawerItem> items;

	public AddNavDrawerItemsParams(@NonNull String appPackage, @NonNull List<NavDrawerItem> items) {
		this.appPackage = appPackage;
		this.items = items;
	}

	protected AddNavDrawerItemsParams(Parcel in) {
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

	public static final Creator<AddNavDrawerItemsParams> CREATOR = new Creator<AddNavDrawerItemsParams>() {
		@Override
		public AddNavDrawerItemsParams createFromParcel(Parcel in) {
			return new AddNavDrawerItemsParams(in);
		}

		@Override
		public AddNavDrawerItemsParams[] newArray(int size) {
			return new AddNavDrawerItemsParams[size];
		}
	};
}
