package net.osmand.plus.profiles;

import android.os.Parcel;
import android.os.Parcelable;

public class ProfileDataObject implements Parcelable {

	private String name;
	private String description;
	private int iconRes;

	public ProfileDataObject(String name, String description, int iconRes) {
		this.name = name;
		this.iconRes = iconRes;
		this.description = description;
	}

	protected ProfileDataObject(Parcel in) {
		name = in.readString();
		description = in.readString();
		iconRes = in.readInt();
	}

	public static final Creator<ProfileDataObject> CREATOR = new Creator<ProfileDataObject>() {
		@Override
		public ProfileDataObject createFromParcel(Parcel in) {
			return new ProfileDataObject(in);
		}

		@Override
		public ProfileDataObject[] newArray(int size) {
			return new ProfileDataObject[size];
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
