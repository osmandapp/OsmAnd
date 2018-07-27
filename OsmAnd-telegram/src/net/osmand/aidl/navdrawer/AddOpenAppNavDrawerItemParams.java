package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;

public class AddOpenAppNavDrawerItemParams implements Parcelable {

	private String itemName;
	private String appPackage;
	private int flags;

	public AddOpenAppNavDrawerItemParams(String itemName, String appPackage) {
		this(itemName, appPackage, -1);
	}

	public AddOpenAppNavDrawerItemParams(String itemName, String appPackage, int flags) {
		this.itemName = itemName;
		this.appPackage = appPackage;
		this.flags = flags;
	}

	protected AddOpenAppNavDrawerItemParams(Parcel in) {
		itemName = in.readString();
		appPackage = in.readString();
		flags = in.readInt();
	}

	public String getItemName() {
		return itemName;
	}

	public String getAppPackage() {
		return appPackage;
	}

	public int getFlags() {
		return flags;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(itemName);
		dest.writeString(appPackage);
		dest.writeInt(flags);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<AddOpenAppNavDrawerItemParams> CREATOR = new Creator<AddOpenAppNavDrawerItemParams>() {
		@Override
		public AddOpenAppNavDrawerItemParams createFromParcel(Parcel in) {
			return new AddOpenAppNavDrawerItemParams(in);
		}

		@Override
		public AddOpenAppNavDrawerItemParams[] newArray(int size) {
			return new AddOpenAppNavDrawerItemParams[size];
		}
	};
}
