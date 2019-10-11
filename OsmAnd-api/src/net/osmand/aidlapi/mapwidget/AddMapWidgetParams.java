package net.osmand.aidlapi.mapwidget;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddMapWidgetParams extends AidlParams {

	private AMapWidget widget;

	public AddMapWidgetParams(AMapWidget widget) {
		this.widget = widget;
	}

	public AddMapWidgetParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddMapWidgetParams> CREATOR = new Creator<AddMapWidgetParams>() {
		@Override
		public AddMapWidgetParams createFromParcel(Parcel in) {
			return new AddMapWidgetParams(in);
		}

		@Override
		public AddMapWidgetParams[] newArray(int size) {
			return new AddMapWidgetParams[size];
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