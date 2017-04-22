package net.osmand.aidl.mapwidget;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateMapWidgetParams implements Parcelable {
	private AMapWidget widget;

	public UpdateMapWidgetParams(AMapWidget widget) {
		this.widget = widget;
	}

	public UpdateMapWidgetParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<UpdateMapWidgetParams> CREATOR = new
			Parcelable.Creator<UpdateMapWidgetParams>() {
				public UpdateMapWidgetParams createFromParcel(Parcel in) {
					return new UpdateMapWidgetParams(in);
				}

				public UpdateMapWidgetParams[] newArray(int size) {
					return new UpdateMapWidgetParams[size];
				}
			};

	public AMapWidget getWidget() {
		return widget;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(widget, flags);
	}

	private void readFromParcel(Parcel in) {
		widget = in.readParcelable(AMapWidget.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
