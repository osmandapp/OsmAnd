package net.osmand.plus.profiles;

import android.os.Parcel;


public class RoutingProfile extends ProfileDataObject {

	private String stringKey;

	public RoutingProfile(String stringKey, String name, String descr,  int iconRes, boolean isSelected) {
		super(name, descr, iconRes, isSelected);
		this.stringKey = stringKey;
	}

	public String getStringKey() {
		return stringKey;
	}

	protected RoutingProfile(Parcel in) {
		super(in);
		stringKey = in.readString();
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
	}
}
