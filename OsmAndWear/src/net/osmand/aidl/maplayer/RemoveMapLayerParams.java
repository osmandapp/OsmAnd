package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class RemoveMapLayerParams implements Parcelable {
	private String id;

	public RemoveMapLayerParams(String id) {
		this.id = id;
	}

	public RemoveMapLayerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveMapLayerParams> CREATOR = new
			Creator<RemoveMapLayerParams>() {
				public RemoveMapLayerParams createFromParcel(Parcel in) {
					return new RemoveMapLayerParams(in);
				}

				public RemoveMapLayerParams[] newArray(int size) {
					return new RemoveMapLayerParams[size];
				}
			};

	public String getId() {
		return id;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
	}

	public int describeContents() {
		return 0;
	}
}
