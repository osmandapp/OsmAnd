package net.osmand.aidl.mapwidget;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveMapWidgetParams implements Parcelable {
	private String id;

	public RemoveMapWidgetParams(String id) {
		this.id = id;
	}

	public RemoveMapWidgetParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<RemoveMapWidgetParams> CREATOR = new
			Parcelable.Creator<RemoveMapWidgetParams>() {
				public RemoveMapWidgetParams createFromParcel(Parcel in) {
					return new RemoveMapWidgetParams(in);
				}

				public RemoveMapWidgetParams[] newArray(int size) {
					return new RemoveMapWidgetParams[size];
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
