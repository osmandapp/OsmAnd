package net.osmand.aidl.mapwidget;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMapWidgetParams  implements Parcelable {
	private AMapWidget widget;

	public AddMapWidgetParams(AMapWidget widget) {
		this.widget = widget;
	}

	public AddMapWidgetParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<AddMapWidgetParams> CREATOR = new
			Parcelable.Creator<AddMapWidgetParams>() {
				public AddMapWidgetParams createFromParcel(Parcel in) {
					return new AddMapWidgetParams(in);
				}

				public AddMapWidgetParams[] newArray(int size) {
					return new AddMapWidgetParams[size];
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
