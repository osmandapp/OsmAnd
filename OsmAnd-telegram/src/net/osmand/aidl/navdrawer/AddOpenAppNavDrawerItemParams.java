package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;

public class AddOpenAppNavDrawerItemParams implements Parcelable {

	private String itemName;
	private String appPackage;
	private String uri;
	private int flags;

	public AddOpenAppNavDrawerItemParams(String itemName, String appPackage, String uri) {
		this(itemName, appPackage, uri, -1);
	}

	public AddOpenAppNavDrawerItemParams(String itemName, String appPackage, String uri, int flags) {
		this.itemName = itemName;
		this.appPackage = appPackage;
		this.uri = uri;
		this.flags = flags;
	}

	protected AddOpenAppNavDrawerItemParams(Parcel in) {
		itemName = in.readString();
		appPackage = in.readString();
		uri = in.readString();
		flags = in.readInt();
	}

	public String getItemName() {
		return itemName;
	}

	public String getAppPackage() {
		return appPackage;
	}

	public String getUri() {
		return uri;
	}

	public int getFlags() {
		return flags;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(itemName);
		dest.writeString(appPackage);
		dest.writeString(uri);
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
