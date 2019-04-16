package net.osmand.plus.profiles;

import android.os.Parcel;
import android.os.Parcelable;

public class BaseProfile implements Parcelable {

	private String name;
	private String description;
	private int iconRes;

	public BaseProfile(String name, int iconRes) {
		this.name = name;
		this.name = description;
		this.iconRes = iconRes;
	}

	public BaseProfile(String name, String description, int iconRes) {
		this.name = name;
		this.name = description;
		this.iconRes = iconRes;
	}

	protected BaseProfile(Parcel in) {
		name = in.readString();
		description = in.readString();
		iconRes = in.readInt();

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

	public String getName() {
		return name;
	}

	public int getIconRes() {
		return iconRes;
	}

	public String getDescription() {
		return description;
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(description);
		dest.writeInt(iconRes);
	}
}
