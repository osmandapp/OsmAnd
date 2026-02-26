package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class AddMapLayerParams implements Parcelable {
	private AMapLayer layer;

	public AddMapLayerParams(AMapLayer layer) {
		this.layer = layer;
	}

	public AddMapLayerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddMapLayerParams> CREATOR = new
			Creator<AddMapLayerParams>() {
				public AddMapLayerParams createFromParcel(Parcel in) {
					return new AddMapLayerParams(in);
				}

				public AddMapLayerParams[] newArray(int size) {
					return new AddMapLayerParams[size];
				}
			};

	public AMapLayer getLayer() {
		return layer;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(layer, flags);
	}

	private void readFromParcel(Parcel in) {
		layer = in.readParcelable(AMapLayer.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
