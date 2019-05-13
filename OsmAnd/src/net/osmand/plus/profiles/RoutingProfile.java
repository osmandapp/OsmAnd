package net.osmand.plus.profiles;

import android.os.Parcel;


public class RoutingProfile extends ProfileDataObject {

	private String stringKey;
	private String fileName;

	public RoutingProfile(String stringKey, String name, String descr,  int iconRes, boolean isSelected, String fileName) {
		super(name, descr, iconRes, isSelected);
		this.stringKey = stringKey;
		this.fileName = fileName;
	}

	public String getStringKey() {
		return stringKey;
	}

	protected RoutingProfile(Parcel in) {
		super(in);
		stringKey = in.readString();
		fileName = in.readString();
	}

	public String getFileName() {
		return fileName;
	}

	public static final Creator<RoutingProfile> CREATOR = new Creator<RoutingProfile>() {
		@Override
		public RoutingProfile createFromParcel(Parcel in) {
			return new RoutingProfile(in);
		}

		@Override
		public RoutingProfile[] newArray(int size) {
			return new RoutingProfile[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(stringKey);
		dest.writeString(fileName);
	}
}
