package net.osmand.plus.profiles;

import android.os.Parcel;
import android.os.Parcelable;
import net.osmand.plus.routing.RoutingHelper;

public class RoutingProfile extends BaseProfile {

	private BaseProfile parent;
	private boolean isSelected;

	public RoutingProfile(String name, BaseProfile parent, int iconRes, boolean isSelected) {
		super(name, iconRes);
		this.parent = parent;
		this.isSelected = isSelected;
	}

	public BaseProfile getParent() {
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
		parent = in.readParcelable(BaseProfile.class.getClassLoader());
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
		dest.writeParcelable(parent, flags);
		dest.writeByte((byte) (isSelected ? 1 : 0));
	}
}
