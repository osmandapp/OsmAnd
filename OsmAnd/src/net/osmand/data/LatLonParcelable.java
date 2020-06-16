package net.osmand.data;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class LatLonParcelable extends LatLon implements Parcelable {
	public LatLonParcelable(double latitude, double longitude) {
		super(latitude, longitude);
	}

	public LatLonParcelable(LatLon latlon) {
		super(latlon.getLatitude(), latlon.getLongitude());
	}

	public LatLonParcelable(Parcel in) {
		super(in.readDouble(), in.readDouble());
	}

	public static final Creator<LatLonParcelable> CREATOR = new Creator<LatLonParcelable>() {
		@Override
		public LatLonParcelable createFromParcel(Parcel in) {
			return new LatLonParcelable(in);
		}

		@Override
		public LatLonParcelable[] newArray(int size) {
			return new LatLonParcelable[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeDouble(getLatitude());
		parcel.writeDouble(getLongitude());
	}
}