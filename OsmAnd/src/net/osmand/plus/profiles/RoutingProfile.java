package net.osmand.plus.profiles;

import android.os.Parcel;
import android.os.Parcelable;

public class RoutingProfile implements Parcelable {

	private String name;
	private String origin;
	private int iconRes;
	private boolean isSelected;

	public RoutingProfile(String name, String origin, int iconRes, boolean isSelected) {
		this.name = name;
		this.origin = origin;
		this.iconRes = iconRes;
		this.isSelected = isSelected;
	}

	public String getName() {
		return name;
	}

	public String getOrigin() {
		return origin;
	}

	public int getIconRes() {
		return iconRes;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean selected) {
		isSelected = selected;
	}

	protected RoutingProfile(Parcel in) {
		name = in.readString();
		origin = in.readString();
		iconRes = in.readInt();
		isSelected = in.readByte() != 0;
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
		dest.writeString(name);
		dest.writeString(origin);
		dest.writeInt(iconRes);
		dest.writeByte((byte) (isSelected ? 1 : 0));
	}
}
