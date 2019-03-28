package net.osmand.plus.profiles;

import android.os.Parcel;
import android.os.Parcelable;

public class AppProfile implements Parcelable {
	private int iconRes;
	private String title;
	private String navType;
	private boolean isSelected;
	private boolean isAppDefault;

	public AppProfile(int iconRes, String title, String descr, boolean isAppDefault) {
		this.iconRes = iconRes;
		this.title = title;
		this.navType = descr;
		this.isAppDefault = isAppDefault;
	}



	public int getIconRes() {
		return iconRes;
	}

	public void setIconRes(int iconRes) {
		this.iconRes = iconRes;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getNavType() {
		return navType;
	}

	public void setNavType(String descr) {
		this.navType = descr;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public boolean isAppDefault() {
		return isAppDefault;
	}

	public void setAppDefault(boolean appDefault) {
		isAppDefault = appDefault;
	}

	protected AppProfile(Parcel in) {
		iconRes = in.readInt();
		title = in.readString();
		navType = in.readString();
		isSelected = in.readByte() != 0;
		isAppDefault = in.readByte() != 0;
	}

	public static final Creator<AppProfile> CREATOR = new Creator<AppProfile>() {
		@Override
		public AppProfile createFromParcel(Parcel in) {
			return new AppProfile(in);
		}

		@Override
		public AppProfile[] newArray(int size) {
			return new AppProfile[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(iconRes);
		dest.writeString(title);
		dest.writeString(navType);
		dest.writeByte((byte) (isSelected ? 1 : 0));
		dest.writeByte((byte) (isAppDefault ? 1 : 0));
	}


}
