package net.osmand.plus.profiles;

import android.os.Parcel;

import net.osmand.router.GeneralRouter;

public class RoutingProfile extends ProfileDataObject {

	private String parent;
	private boolean isSelected;
	private String routerProfile;

	public RoutingProfile(String name, String parent, int iconRes, boolean isSelected) {
		super(name, parent, iconRes);
		this.parent = parent;
		this.isSelected = isSelected;
	}

	public String getParent() {
		return parent;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean selected) {
		isSelected = selected;
	}

	protected RoutingProfile(Parcel in) {
		super(in);
		parent = in.readString();
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
		super.writeToParcel(dest, flags);
		dest.writeString(parent);
		dest.writeByte((byte) (isSelected ? 1 : 0));
	}
}
