package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigateParams implements Parcelable {

	private String startName;
	private double startLat;
	private double startLon;
	private String destName;
	private double destLat;
	private double destLon;
	private String profile;
	private boolean force;

	public NavigateParams(String startName, double startLat, double startLon, String destName, double destLat, double destLon, String profile, boolean force) {
		this.startName = startName;
		this.startLat = startLat;
		this.startLon = startLon;
		this.destName = destName;
		this.destLat = destLat;
		this.destLon = destLon;
		this.profile = profile;
		this.force = force;
	}

	public NavigateParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<NavigateParams> CREATOR = new Creator<NavigateParams>() {
		@Override
		public NavigateParams createFromParcel(Parcel in) {
			return new NavigateParams(in);
		}

		@Override
		public NavigateParams[] newArray(int size) {
			return new NavigateParams[size];
		}
	};

	public String getStartName() {
		return startName;
	}

	public double getStartLat() {
		return startLat;
	}

	public double getStartLon() {
		return startLon;
	}

	public String getDestName() {
		return destName;
	}

	public double getDestLat() {
		return destLat;
	}

	public double getDestLon() {
		return destLon;
	}

	public String getProfile() {
		return profile;
	}

	public boolean isForce() {
		return force;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(startName);
		out.writeDouble(startLat);
		out.writeDouble(startLon);
		out.writeString(destName);
		out.writeDouble(destLat);
		out.writeDouble(destLon);
		out.writeString(profile);
		out.writeByte((byte) (force ? 1 : 0));
	}

	private void readFromParcel(Parcel in) {
		startName = in.readString();
		startLat = in.readDouble();
		startLon = in.readDouble();
		destName = in.readString();
		destLat = in.readDouble();
		destLon = in.readDouble();
		profile = in.readString();
		force = in.readByte() != 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

}
