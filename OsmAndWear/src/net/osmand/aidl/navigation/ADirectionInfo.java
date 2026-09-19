package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class ADirectionInfo implements Parcelable {

	private int distanceTo; //distance to next turn
	private int turnType; //turn type
	private boolean isLeftSide; //is movement left-sided

	public ADirectionInfo(int distanceTo, int turnType, boolean isLeftSide) {
		this.distanceTo = distanceTo;
		this.turnType = turnType;
		this.isLeftSide = isLeftSide;
	}

	protected ADirectionInfo(Parcel in) {
		distanceTo = in.readInt();
		turnType = in.readInt();
		isLeftSide = in.readByte() != 0;
	}

	public static final Creator<ADirectionInfo> CREATOR = new Creator<ADirectionInfo>() {
		@Override
		public ADirectionInfo createFromParcel(Parcel in) {
			return new ADirectionInfo(in);
		}

		@Override
		public ADirectionInfo[] newArray(int size) {
			return new ADirectionInfo[size];
		}
	};

	public int getDistanceTo() {
		return distanceTo;
	}

	public int getTurnType() {
		return turnType;
	}

	public boolean isLeftSide() {
		return isLeftSide;
	}

	public void setDistanceTo(int distanceTo) {
		this.distanceTo = distanceTo;
	}

	public void setTurnType(int turnType) {
		this.turnType = turnType;
	}

	public void setLeftSide(boolean leftSide) {
		isLeftSide = leftSide;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(distanceTo);
		dest.writeInt(turnType);
		dest.writeByte((byte) (isLeftSide ? 1 : 0));
	}


}
