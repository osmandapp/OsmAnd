package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class UpdateMapLayerParams implements Parcelable {
	private AMapLayer layer;

	public UpdateMapLayerParams(AMapLayer layer) {
		this.layer = layer;
	}

	public UpdateMapLayerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UpdateMapLayerParams> CREATOR = new
			Creator<UpdateMapLayerParams>() {
				public UpdateMapLayerParams createFromParcel(Parcel in) {
					return new UpdateMapLayerParams(in);
				}

				public UpdateMapLayerParams[] newArray(int size) {
					return new UpdateMapLayerParams[size];
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
