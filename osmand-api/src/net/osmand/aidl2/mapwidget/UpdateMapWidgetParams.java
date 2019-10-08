package net.osmand.aidl2.mapwidget;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class UpdateMapWidgetParams extends AidlParams {

	private AMapWidget widget;

	public UpdateMapWidgetParams(AMapWidget widget) {
		this.widget = widget;
	}

	public UpdateMapWidgetParams(Parcel in) {
		super(in);
	}

	public static final Creator<UpdateMapWidgetParams> CREATOR = new Creator<UpdateMapWidgetParams>() {
		@Override
		public UpdateMapWidgetParams createFromParcel(Parcel in) {
			return new UpdateMapWidgetParams(in);
		}

		@Override
		public UpdateMapWidgetParams[] newArray(int size) {
			return new UpdateMapWidgetParams[size];
		}
	};

	public AMapWidget getWidget() {
		return widget;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("widget", widget);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapWidget.class.getClassLoader());
		widget = bundle.getParcelable("widget");
	}
}