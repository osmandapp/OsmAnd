package net.osmand.aidl.navigation;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class NavigateGpxParams implements Parcelable {

	private String data;
	private Uri uri;
	private boolean force;

	public NavigateGpxParams(String data, boolean force) {
		this.data = data;
		this.force = force;
	}

	public NavigateGpxParams(Uri uri, boolean force) {
		this.uri = uri;
		this.force = force;
	}

	public NavigateGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<NavigateGpxParams> CREATOR = new Creator<NavigateGpxParams>() {
		@Override
		public NavigateGpxParams createFromParcel(Parcel in) {
			return new NavigateGpxParams(in);
		}

		@Override
		public NavigateGpxParams[] newArray(int size) {
			return new NavigateGpxParams[size];
		}
	};

	public String getData() {
		return data;
	}

	public Uri getUri() {
		return uri;
	}

	public boolean isForce() {
		return force;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(data);
		out.writeParcelable(uri, flags);
		out.writeByte((byte) (force ? 1 : 0));
	}

	private void readFromParcel(Parcel in) {
		data = in.readString();
		uri = in.readParcelable(Uri.class.getClassLoader());
		force = in.readByte() != 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

}
