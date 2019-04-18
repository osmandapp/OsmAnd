package net.osmand.plus.profiles;

import android.os.Parcel;

public class BaseProfile extends ProfileDataObject {

	private String stringKey;

	public BaseProfile(String stringKey, String name, String description, int iconRes) {
		super(name, description, iconRes);
		this.stringKey = stringKey;
	}

	public String getStringKey() {
		return stringKey;
	}

	protected BaseProfile(Parcel in) {
		super(in);
		stringKey = in.readString();
	}

	public static final Creator<BaseProfile> CREATOR = new Creator<BaseProfile>() {
		@Override
		public BaseProfile createFromParcel(Parcel in) {
			return new BaseProfile(in);
		}

		@Override
		public BaseProfile[] newArray(int size) {
			return new BaseProfile[size];
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
